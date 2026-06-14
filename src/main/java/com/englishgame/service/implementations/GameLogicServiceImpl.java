package com.englishgame.service.implementations;

import com.englishgame.model.SpanishExpression;
import com.englishgame.model.EnglishExpression;
import com.englishgame.model.CorrectAnswerOutcome;
import com.englishgame.service.interfaces.GameLogicService;
import com.englishgame.service.interfaces.GameDataService;
import com.englishgame.service.interfaces.DatabaseService;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.Locale;

/**
 * Implementation of GameLogicService for managing game logic
 * Handles the core game flow, scoring, and learning progression
 */
@Slf4j
public class GameLogicServiceImpl implements GameLogicService {
    
    private final GameDataService gameDataService;
    private final DatabaseService databaseService;
    // Umbral de promoción a learned words.
    private static final int LEARNED_THRESHOLD = 21;
    private static final int PENALTY_POINTS = 5;
    private static final String LEARNED_WORDS_DATABASE = "learned_words";
    
    public GameLogicServiceImpl(GameDataService gameDataService) {
        this.gameDataService = gameDataService;
        this.databaseService = null; // Backward compatibility if constructed without DatabaseService
    }

    public GameLogicServiceImpl(GameDataService gameDataService, DatabaseService databaseService) {
        this.gameDataService = gameDataService;
        this.databaseService = databaseService;
    }

    private static String normalizeSpanishPhrase(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * All {@link SpanishExpression} rows in {@code databaseName} with the same Spanish text as {@code anchor}
     * (trim + case insensitive). Fallback: singleton list with {@code anchor} when DB unavailable.
     */
    private List<SpanishExpression> spanishPhraseCohort(String databaseName, SpanishExpression anchor) {
        if (anchor == null || anchor.getExpression() == null) {
            return List.of();
        }
        if (databaseName == null || databaseName.isBlank() || databaseService == null) {
            return List.of(anchor);
        }
        String needle = normalizeSpanishPhrase(anchor.getExpression());
        List<SpanishExpression> cohort = databaseService.getSpanishExpressions(databaseName).stream()
                .filter(e -> e != null && e.getExpression() != null
                        && normalizeSpanishPhrase(e.getExpression()).equals(needle))
                .collect(Collectors.toList());
        return cohort.isEmpty() ? List.of(anchor) : cohort;
    }

    private static String normalizeEnglishLemmaForCompare(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().toLowerCase(Locale.ROOT).replace('-', ' ').replaceAll("\\s+", " ");
    }

    private static boolean englishMatchesUser(String userTrimmed, EnglishExpression en) {
        return en != null && en.getExpression() != null
                && normalizeEnglishLemmaForCompare(userTrimmed)
                        .equals(normalizeEnglishLemmaForCompare(en.getExpression()));
    }

    @Override
    public SpanishExpression getRandomSpanishExpression(String databaseName) {
        return getRandomSpanishExpression(databaseName, null);
    }
    
    @Override
    public SpanishExpression getRandomSpanishExpression(String databaseName, SpanishExpression excludePreviousRound) {
        log.debug("Getting random Spanish expression from database: {}", databaseName);
        if (databaseService != null) {
            return databaseService.getRandomSpanishExpression(databaseName, excludePreviousRound);
        }
        // Fallback to previous behavior if databaseService is not provided
        return Optional.ofNullable(databaseName)
                .map(this::getSpanishExpressionsFromDatabase)
                .filter(expressions -> !expressions.isEmpty())
                .map(expressions -> {
                    List<SpanishExpression> pool = new ArrayList<>(expressions);
                    if (excludePreviousRound != null && excludePreviousRound.getExpression() != null
                            && pool.size() > 1) {
                        pool.removeIf(e ->
                                Objects.equals(e.getExpression(), excludePreviousRound.getExpression()));
                    }
                    if (pool.isEmpty()) {
                        pool = new ArrayList<>(expressions);
                    }
                    Random random = new Random();
                    SpanishExpression selectedExpression = pool.get(random.nextInt(pool.size()));
                    log.debug("Selected Spanish expression: '{}' with score: {}",
                            selectedExpression.getExpression(), selectedExpression.getScore());
                    return selectedExpression;
                })
                .orElseGet(() -> {
                    log.warn("Database '{}' is empty", databaseName);
                    return null;
                });
    }
    
    @Override
    public boolean validateTranslation(SpanishExpression promptCard, String userTranslation,
            String practiceDatabaseName) {
        if (promptCard == null || userTranslation == null || userTranslation.trim().isEmpty()) {
            log.warn("Invalid parameters for translation validation");
            return false;
        }
        String userTrim = userTranslation.trim();
        List<SpanishExpression> cohort = spanishPhraseCohort(practiceDatabaseName, promptCard);
        boolean isValid = cohort.stream()
                .filter(e -> e.getTranslations() != null)
                .flatMap(e -> e.getTranslations().stream())
                .anyMatch(en -> englishMatchesUser(userTrim, en));
        log.debug("Validating '{}' for '{}' ({} cohort records): {}",
                userTrim, promptCard.getExpression(), cohort.size(), isValid);
        return isValid;
    }

    @Override
    public CorrectAnswerOutcome processCorrectAnswer(SpanishExpression promptCard,
            String userTranslation, String practiceDatabaseName) {
        if (promptCard == null || userTranslation == null || userTranslation.trim().isEmpty()) {
            return null;
        }
        String userTrim = userTranslation.trim();
        List<SpanishExpression> cohort = spanishPhraseCohort(practiceDatabaseName, promptCard);
        for (SpanishExpression expr : cohort) {
            if (expr.getTranslations() == null) {
                continue;
            }
            for (EnglishExpression englishExpr : expr.getTranslations()) {
                if (!englishMatchesUser(userTrim, englishExpr)) {
                    continue;
                }
                log.debug("Correct match on record '{}' -> '{}'",
                        expr.getExpression(), englishExpr.getExpression());
                englishExpr.setScore(englishExpr.getScore() + 1);
                expr.setScore(expr.getScore() + 1);
                log.debug("Added 1 point to English '{}'. New score: {}",
                        englishExpr.getExpression(), englishExpr.getScore());

                boolean promoted = false;
                if (isExpressionLearned(englishExpr)
                        && databaseService != null
                        && practiceDatabaseName != null && !practiceDatabaseName.isBlank()) {
                    log.info("English expression '{}' reached learned threshold. Promoting to learned words.",
                            englishExpr.getExpression());
                    promoted = databaseService.promoteTranslationToLearned(
                            practiceDatabaseName, expr, englishExpr);
                }
                return new CorrectAnswerOutcome(englishExpr, promoted);
            }
        }
        return null;
    }

    @Override
    public List<EnglishExpression> processIncorrectAnswer(SpanishExpression promptCard, String userTranslation,
            String practiceDatabaseName) {
        if (promptCard == null) {
            return Collections.emptyList();
        }
        List<SpanishExpression> cohort = spanishPhraseCohort(practiceDatabaseName, promptCard);
        log.debug("Incorrect answer '{}' for '{}' — penalizing {} cohort record(s)",
                userTranslation == null ? "" : userTranslation, promptCard.getExpression(), cohort.size());
        for (SpanishExpression expr : cohort) {
            if (expr.getTranslations() != null) {
                for (EnglishExpression englishExpr : expr.getTranslations()) {
                    int currentScore = englishExpr.getScore();
                    int penalty = calculateDynamicPenalty(currentScore);
                    int newScore = Math.max(0, currentScore - penalty);
                    englishExpr.setScore(newScore);
                    log.debug("Penalty {} on English '{}' under phrase '{}' (score {} -> {})",
                            penalty, englishExpr.getExpression(), expr.getExpression(), currentScore, newScore);
                }
            }
            int phraseScoreBefore = expr.getScore();
            int phrasePenalty = calculateDynamicPenalty(phraseScoreBefore);
            expr.setScore(Math.max(0, phraseScoreBefore - phrasePenalty));
            log.debug("Phrase score penalty for '{}' (score {} -> {})",
                    expr.getExpression(), phraseScoreBefore, expr.getScore());
        }
        return promptCard.getTranslations() != null ? promptCard.getTranslations() : Collections.emptyList();
    }

    @Override
    public List<SpanishExpression> getSpanishPhraseCohort(String practiceDatabaseName, SpanishExpression anchor) {
        return new ArrayList<>(spanishPhraseCohort(practiceDatabaseName, anchor));
    }
    
    /**
     * Calculates dynamic penalty based on current score
     * @param currentScore the current score of the expression
     * @return penalty points to subtract
     */
    private int calculateDynamicPenalty(int currentScore) {
        if (currentScore < 5) {
            return 2; // Light penalty for low scores
        } else if (currentScore <= 10) {
            return 3; // Medium penalty for medium scores
        } else {
            return 5; // Heavy penalty for high scores
        }
    }
    
    @Override
    public boolean isExpressionLearned(EnglishExpression englishExpression) {
        boolean learned = englishExpression.getScore() >= LEARNED_THRESHOLD;
        log.debug("English expression '{}' learned status: {} (score: {})", 
                englishExpression.getExpression(), learned, englishExpression.getScore());
        return learned;
    }
    
    @Override
    public boolean moveToLearnedWords(EnglishExpression englishExpression) {
        log.debug("Moving English expression '{}' to learned words database", 
                englishExpression.getExpression());
        if (databaseService == null) {
            log.warn("DatabaseService not available to move learned words");
            return false;
        }
        return databaseService.moveToLearnedWords(englishExpression);
    }
    
    @Override
    public int getLearnedScoreThreshold() {
        return LEARNED_THRESHOLD;
    }
    
    @Override
    public int getSpanishExpressionScore(SpanishExpression spanishExpression) {
        return spanishExpression.getScore();
    }
    
    @Override
    public int getEnglishExpressionScore(EnglishExpression englishExpression) {
        return englishExpression.getScore();
    }
    
    @Override
    public List<String> getAvailableDatabases() {
        log.debug("Getting available databases");
        if (databaseService != null) {
            return databaseService.getAvailableDatabases();
        }
        List<String> databases = new ArrayList<>();
        databases.add(LEARNED_WORDS_DATABASE);
        return databases;
    }
    
    @Override
    public boolean createDatabase(String databaseName) {
        log.debug("Creating new database: {}", databaseName);
        if (databaseService == null) {
            log.warn("DatabaseService not available to create database");
            return false;
        }
        return databaseService.createDatabase(databaseName);
    }
    
    @Override
    public List<SpanishExpression> getSpanishExpressionsFromDatabase(String databaseName) {
        log.debug("Getting Spanish expressions from database: {}", databaseName);
        if (databaseService != null) {
            return databaseService.getSpanishExpressions(databaseName);
        }
        return new ArrayList<>();
    }
    
    /**
     * Maps repository data to SpanishExpression object
     */
    private SpanishExpression mapToSpanishExpression(Map<String, Object> data) {
        // Deprecated: repository mapping removed in favor of DatabaseService delegation
        SpanishExpression expression = new SpanishExpression();
        expression.setExpression((String) data.get("expression"));
        expression.setScore(((Number) data.get("score")).intValue());
        return expression;
    }
    
    @Override
    public List<EnglishExpression> getEnglishExpressionsFromDatabase(String databaseName) {
        log.debug("Getting English expressions from database: {}", databaseName);
        if (databaseService != null) {
            return databaseService.getEnglishExpressions(databaseName);
        }
        return new ArrayList<>();
    }
}
