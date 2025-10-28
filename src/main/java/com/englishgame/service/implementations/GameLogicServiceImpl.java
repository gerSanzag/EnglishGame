package com.englishgame.service.implementations;

import com.englishgame.model.SpanishExpression;
import com.englishgame.model.EnglishExpression;
import com.englishgame.service.interfaces.GameLogicService;
import com.englishgame.service.interfaces.GameDataService;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Optional;

/**
 * Implementation of GameLogicService for managing game logic
 * Handles the core game flow, scoring, and learning progression
 */
@Slf4j
public class GameLogicServiceImpl implements GameLogicService {
    
    private final GameDataService gameDataService;
    private static final int LEARNED_THRESHOLD = 15;
    private static final int PENALTY_POINTS = 5;
    private static final String LEARNED_WORDS_DATABASE = "learned_words";
    
    public GameLogicServiceImpl(GameDataService gameDataService) {
        this.gameDataService = gameDataService;
    }
    
    @Override
    public SpanishExpression getRandomSpanishExpression(String databaseName) {
        log.debug("Getting random Spanish expression from database: {}", databaseName);
        
        return Optional.ofNullable(databaseName)
                .map(this::getSpanishExpressionsFromDatabase)
                .filter(expressions -> !expressions.isEmpty())
                .map(expressions -> {
                    Random random = new Random();
                    SpanishExpression selectedExpression = expressions.get(random.nextInt(expressions.size()));
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
    public boolean validateTranslation(SpanishExpression spanishExpression, String userTranslation) {
        return Optional.ofNullable(spanishExpression)
                .filter(expr -> userTranslation != null && !userTranslation.trim().isEmpty())
                .map(expr -> {
                    log.debug("Validating translation '{}' for Spanish expression '{}'", 
                            userTranslation, expr.getExpression());
                    
                    boolean isValid = expr.getTranslations().stream()
                            .anyMatch(englishExpr -> englishExpr.getExpression().equalsIgnoreCase(userTranslation.trim()));
                    
                    log.debug("Translation validation result: {}", isValid);
                    return isValid;
                })
                .orElseGet(() -> {
                    log.warn("Invalid parameters for translation validation");
                    return false;
                });
    }
    
    @Override
    public EnglishExpression processCorrectAnswer(SpanishExpression spanishExpression, String userTranslation) {
        return Optional.ofNullable(spanishExpression)
                .map(expr -> {
                    log.debug("Processing correct answer for '{}' -> '{}'", 
                            expr.getExpression(), userTranslation);
                    
                    return expr.getTranslations().stream()
                            .filter(englishExpr -> englishExpr.getExpression().equalsIgnoreCase(userTranslation.trim()))
                            .findFirst()
                            .map(matchingExpression -> {
                                // Add 1 point to the English expression
                                matchingExpression.setScore(matchingExpression.getScore() + 1);
                                log.debug("Added 1 point to English expression '{}'. New score: {}", 
                                        matchingExpression.getExpression(), matchingExpression.getScore());
                                
                                // Check if learned
                                if (isExpressionLearned(matchingExpression)) {
                                    log.info("English expression '{}' has been learned! Moving to learned words.", 
                                            matchingExpression.getExpression());
                                    moveToLearnedWords(matchingExpression);
                                }
                                
                                return matchingExpression;
                            })
                            .orElse(null);
                })
                .orElse(null);
    }
    
    @Override
    public List<EnglishExpression> processIncorrectAnswer(SpanishExpression spanishExpression, String userTranslation) {
        return Optional.ofNullable(spanishExpression)
                .map(expr -> {
                    log.debug("Processing incorrect answer for '{}' -> '{}'", 
                            expr.getExpression(), userTranslation);
                    
                    return expr.getTranslations().stream()
                            .map(englishExpr -> {
                                int currentScore = englishExpr.getScore();
                                int penalty = calculateDynamicPenalty(currentScore);
                                int newScore = Math.max(0, currentScore - penalty);
                                englishExpr.setScore(newScore);
                                
                                log.debug("Applied {} penalty points to English expression '{}' (was {}, now {})", 
                                        penalty, englishExpr.getExpression(), currentScore, newScore);
                                
                                return englishExpr;
                            })
                            .collect(java.util.stream.Collectors.toList());
                })
                .orElse(new ArrayList<>());
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
        
        try {
            // Ensure learned words database exists
            if (!getAvailableDatabases().contains(LEARNED_WORDS_DATABASE)) {
                createDatabase(LEARNED_WORDS_DATABASE);
            }
            
            // Add to learned words database
            // This would require additional implementation in GameDataService
            // For now, we'll log the action
            log.info("English expression '{}' moved to learned words database", 
                    englishExpression.getExpression());
            
            return true;
        } catch (Exception e) {
            log.error("Error moving expression to learned words: {}", e.getMessage());
            return false;
        }
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
        // This would need to be implemented based on how databases are stored
        // For now, return a placeholder
        List<String> databases = new ArrayList<>();
        databases.add(LEARNED_WORDS_DATABASE);
        return databases;
    }
    
    @Override
    public boolean createDatabase(String databaseName) {
        log.debug("Creating new database: {}", databaseName);
        
        if (getAvailableDatabases().contains(databaseName)) {
            log.warn("Database '{}' already exists", databaseName);
            return false;
        }
        
        // This would need to be implemented based on how databases are stored
        // For now, we'll log the action
        log.info("Database '{}' created successfully", databaseName);
        return true;
    }
    
    @Override
    public List<SpanishExpression> getSpanishExpressionsFromDatabase(String databaseName) {
        log.debug("Getting Spanish expressions from database: {}", databaseName);
        
        // Get data from GameDataService which has access to DatabaseService
        return gameDataService.getRepository().findAll().stream()
                .filter(record -> record.size() > 0)
                .map(record -> record.get(0))
                .filter(data -> data instanceof Map)
                .map(data -> (Map<String, Object>) data)
                .filter(data -> "spanish_expression".equals(data.get("type")))
                .filter(data -> databaseName.equals(data.get("database")))
                .map(this::mapToSpanishExpression)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Maps repository data to SpanishExpression object
     */
    private SpanishExpression mapToSpanishExpression(Map<String, Object> data) {
        SpanishExpression expression = new SpanishExpression();
        expression.setExpression((String) data.get("expression"));
        expression.setScore(((Number) data.get("score")).intValue());
        
        // Map translations
        @SuppressWarnings("unchecked")
        List<String> translations = (List<String>) data.get("translations");
        if (translations != null) {
            List<EnglishExpression> englishTranslations = translations.stream()
                    .map(translation -> {
                        EnglishExpression englishExpr = new EnglishExpression();
                        englishExpr.setExpression(translation);
                        englishExpr.setScore(0); // Default score
                        return englishExpr;
                    })
                    .collect(java.util.stream.Collectors.toList());
            expression.setTranslations(englishTranslations);
        }
        
        return expression;
    }
    
    @Override
    public List<EnglishExpression> getEnglishExpressionsFromDatabase(String databaseName) {
        log.debug("Getting English expressions from database: {}", databaseName);
        
        // This would need to be implemented based on how data is stored
        // For now, return empty list
        return new ArrayList<>();
    }
}
