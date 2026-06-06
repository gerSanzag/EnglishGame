package com.englishgame.service.implementations;

import com.englishgame.AppGameMode;
import com.englishgame.model.LearnedWordsReviewResult;
import com.englishgame.model.ReviewDatabases;
import com.englishgame.model.SpanishExpression;
import com.englishgame.model.EnglishExpression;
import com.englishgame.service.interfaces.DatabaseService;
import com.englishgame.service.interfaces.GameDataService;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Optional;
import javax.swing.JOptionPane;

/**
 * Implementation of DatabaseService for managing game databases
 * Handles creation, management, and operations on game databases
 */
@Slf4j
public class DatabaseServiceImpl implements DatabaseService {
    
    private final GameDataService gameDataService;
    private static final String LEARNED_WORDS_DATABASE = ReviewDatabases.LEARNED_WORDS_KEY;
    private static final String WORDS_DEFINITELY_LEARNED_DATABASE = ReviewDatabases.WORDS_DEFINITELY_LEARNED_KEY;
    private static final String PHRASAL_VERBS_DATABASE = "Phrasal verbs";

    /** Review learned_words: +1 / −5; reapertura a práctica bajo este umbral. */
    private static final int LEARNED_REVIEW_DEMOTION_UNDER = ReviewDatabases.REVIEW_DEMOTION_UNDER_SCORE;
    /** learned_words: al alcanzar esta puntuación pasa a words_definitely_learned (no se purga). */
    private static final int LEARNED_REVIEW_GRADUATE_TO_DEFINITELY_AT = 28;
    /** words_definitely_learned: dominio final y purga en todas las BBDD. */
    private static final int DEFINITELY_REVIEW_MASTER_AT = ReviewDatabases.DEFINITELY_REVIEW_MASTER_SCORE;

    
    // In-memory storage for databases
    private final Map<String, Set<SpanishExpression>> spanishDatabases;
    private final Map<String, Set<EnglishExpression>> englishDatabases;

    /** Dominadas en words_definitely_learned (35) y purgadas; persiste en metadata de la BBDD. */
    private int definitelyMasteredTotal;

    /** Evita escrituras parciales a JSON mientras se rehidrata desde el repositorio. */
    private boolean loadingFromRepository;
    
    public DatabaseServiceImpl(GameDataService gameDataService) {
        this.gameDataService = gameDataService;
        this.spanishDatabases = new HashMap<>();
        this.englishDatabases = new HashMap<>();
        initializeDefaultDatabases();
    }

    /**
     * Resolves the canonical map key for a database (case- and outer-space insensitive).
     */
    private Optional<String> resolveCanonicalDatabaseKey(String databaseName) {
        if (databaseName == null) {
            return Optional.empty();
        }
        String trimmed = databaseName.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        return spanishDatabases.keySet().stream()
                .filter(k -> k.equalsIgnoreCase(trimmed))
                .findFirst();
    }

    private static boolean expressionsEqualNormalized(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    /** Sin traducciones o solo cadenas vacías / en blanco (no deben persistirse en práctica). */
    private static boolean translationsEffectivelyEmpty(SpanishExpression expr) {
        if (expr == null || expr.getTranslations() == null || expr.getTranslations().isEmpty()) {
            return true;
        }
        return expr.getTranslations().stream().allMatch(DatabaseServiceImpl::englishLineBlank);
    }

    private static boolean englishLineBlank(EnglishExpression en) {
        return en == null || en.getExpression() == null || en.getExpression().trim().isEmpty();
    }

    /** Duplicate exacto de registro ES-EN (mismo español y misma traducción inglesa). */
    private boolean hasExactSpanishEnglishDuplicate(String canonicalDbKey, SpanishExpression candidate) {
        if (canonicalDbKey == null || candidate == null || candidate.getExpression() == null) {
            return false;
        }
        Set<SpanishExpression> bucket = spanishDatabases.get(canonicalDbKey);
        if (bucket == null || bucket.isEmpty()) {
            return false;
        }
        String candEs = normalize(candidate.getExpression());
        Set<String> candEn = candidate.getTranslations() == null
                ? Set.of()
                : candidate.getTranslations().stream()
                        .map(EnglishExpression::getExpression)
                        .filter(Objects::nonNull)
                        .map(DatabaseServiceImpl::normalize)
                        .collect(Collectors.toSet());
        if (candEn.isEmpty()) {
            return false;
        }
        for (SpanishExpression existing : bucket) {
            if (existing == null || existing.getExpression() == null) {
                continue;
            }
            if (!candEs.equals(normalize(existing.getExpression()))) {
                continue;
            }
            Set<String> existingEn = existing.getTranslations() == null
                    ? Set.of()
                    : existing.getTranslations().stream()
                            .map(EnglishExpression::getExpression)
                            .filter(Objects::nonNull)
                            .map(DatabaseServiceImpl::normalize)
                            .collect(Collectors.toSet());
            for (String en : candEn) {
                if (existingEn.contains(en)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    @Override
    public boolean createDatabase(String databaseName) {
        return Optional.ofNullable(databaseName)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .filter(name -> resolveCanonicalDatabaseKey(name).isEmpty())
                .map(name -> {
                       // Create in-memory databases
                       spanishDatabases.put(name, new HashSet<>());
                       englishDatabases.put(name, new HashSet<>());
                    
                    // Save database metadata to repository for persistence
                    saveDatabaseMetadataToRepository(name);
                    // Persist to JSON
                    gameDataService.saveGameData();
                    
                    log.info("Database '{}' created successfully", name);
                    return true;
                })
                .orElseGet(() -> {
                    if (databaseName == null || databaseName.trim().isEmpty()) {
                        log.warn("Cannot create database with null or empty name");
                    } else {
                        log.warn("Database '{}' already exists (comparison ignores case/spaces)", databaseName.trim());
                    }
                    return false;
                });
    }
    
    @Override
    public Optional<String> getCanonicalDatabaseName(String databaseName) {
        return resolveCanonicalDatabaseKey(databaseName);
    }

    @Override
    public boolean isSystemDatabase(String databaseName) {
        return resolveCanonicalDatabaseKey(databaseName)
                .map(key -> LEARNED_WORDS_DATABASE.equalsIgnoreCase(key)
                        || WORDS_DEFINITELY_LEARNED_DATABASE.equalsIgnoreCase(key)
                        || PHRASAL_VERBS_DATABASE.equalsIgnoreCase(key))
                .orElse(false);
    }

    @Override
    public boolean isReviewOnlyDatabase(String databaseName) {
        return resolveCanonicalDatabaseKey(databaseName)
                .map(ReviewDatabases::isReviewDatabaseKey)
                .orElse(false);
    }

    @Override
    public int getWordsDefinitelyMasteredTotal() {
        return definitelyMasteredTotal;
    }

    @Override
    public boolean deleteDatabase(String databaseName) {
        return resolveCanonicalDatabaseKey(databaseName)
                .filter(canonical -> !isSystemDatabase(canonical))
                .map(canonical -> {
                    // Remove from in-memory databases
                    spanishDatabases.remove(canonical);
                    englishDatabases.remove(canonical);
                    
                    // Remove from repository for persistence
                    removeDatabaseFromRepository(canonical);
                    // Persist to JSON
                    gameDataService.saveGameData();
                    
                    log.info("Database '{}' deleted successfully", canonical);
                    return true;
                })
                .orElseGet(() -> {
                    if (databaseName == null || databaseName.trim().isEmpty()) {
                        log.warn("Cannot delete database with null or empty name");
                    } else if (!databaseExists(databaseName)) {
                        log.warn("Database '{}' does not exist", databaseName);
                    } else if (isSystemDatabase(databaseName)) {
                        log.warn("Cannot delete system database '{}'", databaseName);
                    }
                    return false;
                });
    }

    @Override
    public Optional<String> renameDatabase(String oldDatabaseName, String newDatabaseName) {
        Optional<String> oldKeyOpt = resolveCanonicalDatabaseKey(oldDatabaseName);
        Optional<String> newTrimmed = Optional.ofNullable(newDatabaseName)
                .map(String::trim)
                .filter(s -> !s.isEmpty());

        if (oldKeyOpt.isEmpty()) {
            log.warn("renameDatabase: source '{}' not found", oldDatabaseName);
            return Optional.empty();
        }
        if (newTrimmed.isEmpty()) {
            log.warn("renameDatabase: empty new name");
            return Optional.empty();
        }

        String oldKey = oldKeyOpt.get();
        String newKey = newTrimmed.get();

        if (isSystemDatabase(oldKey)) {
            log.warn("Cannot rename system database '{}'", oldKey);
            return Optional.empty();
        }

        Optional<String> existingTarget = resolveCanonicalDatabaseKey(newKey);
        if (existingTarget.isPresent() && !existingTarget.get().equals(oldKey)) {
            log.warn("renameDatabase: name '{}' clashes with '{}'", newKey, existingTarget.get());
            return Optional.empty();
        }

        if (oldKey.equals(newKey)) {
            log.debug("renameDatabase: '{}' unchanged", oldKey);
            return Optional.of(oldKey);
        }

        Set<SpanishExpression> spanishBucket = spanishDatabases.remove(oldKey);
        Set<EnglishExpression> englishBucket = englishDatabases.remove(oldKey);
        if (spanishBucket == null || englishBucket == null) {
            log.error("renameDatabase: internal error, missing buckets for '{}'", oldKey);
            return Optional.empty();
        }

        spanishDatabases.put(newKey, spanishBucket);
        englishDatabases.put(newKey, englishBucket);

        gameDataService.saveGameData();
        log.info("Renamed database '{}' -> '{}'", oldKey, newKey);
        return Optional.of(newKey);
    }
    
    @Override
    public List<String> getAvailableDatabases() {
        return spanishDatabases.keySet().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toCollection(ArrayList::new));
    }
    
    @Override
    public boolean databaseExists(String databaseName) {
        return resolveCanonicalDatabaseKey(databaseName).isPresent();
    }
    
    @Override
    public List<SpanishExpression> getSpanishExpressions(String databaseName) {
        return resolveCanonicalDatabaseKey(databaseName)
                .map(key -> new ArrayList<>(spanishDatabases.get(key)))
                .orElseGet(() -> {
                    log.warn("Database '{}' does not exist", databaseName);
                    return new ArrayList<>();
                });
    }
    
    @Override
    public List<EnglishExpression> getEnglishExpressions(String databaseName) {
        return resolveCanonicalDatabaseKey(databaseName)
                .map(key -> new ArrayList<>(englishDatabases.get(key)))
                .orElseGet(() -> {
                    log.warn("Database '{}' does not exist", databaseName);
                    return new ArrayList<>();
                });
    }
    
    @Override
    public boolean addSpanishExpression(String databaseName, SpanishExpression spanishExpression) {
        return resolveCanonicalDatabaseKey(databaseName)
                .flatMap(dbKey -> Optional.ofNullable(spanishExpression)
                        .filter(expr -> expr.getExpression() != null && !expr.getExpression().trim().isEmpty())
                        .map(expr -> {
                            String trimmedPhrase = expr.getExpression().trim();
                            expr.setExpression(trimmedPhrase);
                            if (hasExactSpanishEnglishDuplicate(dbKey, expr)) {
                                log.warn("Exact duplicate pair rejected in '{}': '{}' + '{}'",
                                        dbKey,
                                        trimmedPhrase,
                                        expr.getTranslations().stream()
                                                .map(EnglishExpression::getExpression)
                                                .findFirst().orElse("-"));
                                return false;
                            }
                            boolean added = spanishDatabases.get(dbKey).add(expr);
                            
                            if (added) {
                                if (!loadingFromRepository) {
                                    saveExpressionToRepository(dbKey, expr);
                                    gameDataService.saveGameData();
                                }
                                log.debug("Added Spanish expression '{}' to database '{}'",
                                        trimmedPhrase, dbKey);
                            } else {
                                log.warn("Spanish expression '{}' could not be added to database '{}'",
                                        trimmedPhrase, dbKey);
                            }
                            
                            return added;
                        }))
                .orElseGet(() -> {
                    log.warn("Cannot add Spanish expression to database '{}'", databaseName);
                    return false;
                });
    }
    
    @Override
    public boolean addEnglishExpression(String databaseName, EnglishExpression englishExpression) {
        Optional<String> dbKeyOpt = resolveCanonicalDatabaseKey(databaseName);
        if (dbKeyOpt.isEmpty()) {
            log.warn("Database '{}' does not exist", databaseName);
            return false;
        }
        
        if (englishExpression == null) {
            log.warn("Cannot add null English expression");
            return false;
        }
        
        String dbKey = dbKeyOpt.get();
        String trimmed = englishExpression.getExpression().trim();
        if (trimmed.isEmpty()) {
            log.warn("Cannot add English expression with empty text");
            return false;
        }
        englishExpression.setExpression(trimmed);

        boolean added = englishDatabases.get(dbKey).add(englishExpression);
        if (added) {
            log.debug("Added English expression '{}' to database '{}'", trimmed, dbKey);
            gameDataService.saveGameData();
        } else {
            log.warn("English expression '{}' already present in '{}' (set duplicate)", trimmed, dbKey);
        }
        
        return added;
    }
    
    @Override
    public boolean removeSpanishExpression(String databaseName, String expression) {
        return resolveCanonicalDatabaseKey(databaseName)
                .map(canonical -> {
                    Set<SpanishExpression> expressions = spanishDatabases.get(canonical);
                    log.debug("Before removal: {} expressions in database '{}'", expressions.size(), canonical);

                    boolean removed = expressions.removeIf(spanishExpr ->
                            expressionsEqualNormalized(spanishExpr.getExpression(), expression));

                    log.debug("After removal: {} expressions in database '{}'", expressions.size(), canonical);

                    if (removed) {
                        log.info("Successfully removed Spanish expression '{}' from database '{}'",
                                expression, canonical);
                        gameDataService.saveGameData();
                    } else {
                        log.warn("Spanish expression '{}' not found in database '{}'", expression, canonical);
                    }
                    return removed;
                })
                .orElseGet(() -> {
                    log.warn("Database '{}' does not exist", databaseName);
                    return false;
                });
    }
    
    @Override
    public boolean removeEnglishExpression(String databaseName, String expression) {
        return resolveCanonicalDatabaseKey(databaseName)
                .map(canonical -> {
                    Set<EnglishExpression> expressions = englishDatabases.get(canonical);
                    log.debug("Before removal: {} English expressions in database '{}'", expressions.size(),
                            canonical);

                    boolean removed = expressions.removeIf(englishExpr ->
                            expressionsEqualNormalized(englishExpr.getExpression(), expression));

                    log.debug("After removal: {} English expressions in database '{}'", expressions.size(),
                            canonical);

                    if (removed) {
                        log.info("Successfully removed English expression '{}' from database '{}'",
                                expression, canonical);
                        gameDataService.saveGameData();
                    } else {
                        log.warn("English expression '{}' not found in database '{}'", expression, canonical);
                    }
                    return removed;
                })
                .orElseGet(() -> {
                    log.warn("Database '{}' does not exist", databaseName);
                    return false;
                });
    }
    
    @Override
    public boolean deleteAllSpanishExpressions(String databaseName) {
        Optional<String> key = resolveCanonicalDatabaseKey(databaseName);
        if (key.isEmpty()) {
            log.warn("Database '{}' does not exist", databaseName);
            return false;
        }

        Set<SpanishExpression> expressions = spanishDatabases.get(key.get());
        int countBefore = expressions.size();
        
        expressions.clear();
        
        log.info("Deleted all {} Spanish expressions from database '{}'", countBefore, key.get());
        // Persist to JSON
        gameDataService.saveGameData();
        return countBefore > 0;
    }
    
    @Override
    public boolean deleteAllEnglishExpressions(String databaseName) {
        Optional<String> key = resolveCanonicalDatabaseKey(databaseName);
        if (key.isEmpty()) {
            log.warn("Database '{}' does not exist", databaseName);
            return false;
        }

        Set<EnglishExpression> expressions = englishDatabases.get(key.get());
        int countBefore = expressions.size();
        
        expressions.clear();
        
        log.info("Deleted all {} English expressions from database '{}'", countBefore, key.get());
        // Persist to JSON
        gameDataService.saveGameData();
        return countBefore > 0;
    }

    @Override
    public void pruneSpanishRowsWithoutTranslations() {
        for (Map.Entry<String, Set<SpanishExpression>> e : spanishDatabases.entrySet()) {
            Set<SpanishExpression> bucket = e.getValue();
            if (bucket == null || bucket.isEmpty()) {
                continue;
            }
            int before = bucket.size();
            bucket.removeIf(DatabaseServiceImpl::translationsEffectivelyEmpty);
            int removed = before - bucket.size();
            if (removed > 0) {
                log.debug("Pruned {} invalid Spanish row(s) (no translations) from '{}'", removed, e.getKey());
            }
        }
    }
    
    @Override
    public SpanishExpression getRandomSpanishExpression(String databaseName) {
        return getRandomSpanishExpression(databaseName, null);
    }
    
    @Override
    public SpanishExpression getRandomSpanishExpression(String databaseName, SpanishExpression excludePreviousRound) {
        List<SpanishExpression> expressions = getSpanishExpressions(databaseName);
        expressions.removeIf(DatabaseServiceImpl::translationsEffectivelyEmpty);
        if (expressions.isEmpty()) {
            log.warn("Database '{}' is empty", databaseName);
            return null;
        }
        
        List<SpanishExpression> pool = new ArrayList<>(expressions);
        if (excludePreviousRound != null && excludePreviousRound.getExpression() != null && pool.size() > 1) {
            pool.removeIf(e ->
                    expressionsEqualNormalized(e.getExpression(), excludePreviousRound.getExpression()));
        }
        if (pool.isEmpty()) {
            pool = new ArrayList<>(expressions);
        }
        
        Random random = new Random();
        SpanishExpression selected = pool.get(random.nextInt(pool.size()));
        log.debug("Selected random Spanish expression '{}' from database '{}'",
                selected.getExpression(), databaseName);
        
        return selected;
    }
    
    @Override
    public EnglishExpression getRandomEnglishExpression(String databaseName) {
        List<EnglishExpression> expressions = getEnglishExpressions(databaseName);
        if (expressions.isEmpty()) {
            log.warn("Database '{}' is empty", databaseName);
            return null;
        }
        
        Random random = new Random();
        EnglishExpression selected = expressions.get(random.nextInt(expressions.size()));
        log.debug("Selected random English expression '{}' from database '{}'", 
                selected.getExpression(), databaseName);
        
        return selected;
    }
    
    @Override
    public int getSpanishExpressionCount(String databaseName) {
        return resolveCanonicalDatabaseKey(databaseName)
                .map(k -> spanishDatabases.get(k).size())
                .orElse(0);
    }
    
    @Override
    public int getEnglishExpressionCount(String databaseName) {
        return resolveCanonicalDatabaseKey(databaseName)
                .map(k -> {
                    Set<EnglishExpression> bucket = englishDatabases.get(k);
                    return bucket != null ? bucket.size() : 0;
                })
                .orElse(0);
    }
    
    @Override
    public int getTotalExpressionCount(String databaseName) {
        return getSpanishExpressionCount(databaseName) + getEnglishExpressionCount(databaseName);
    }
    
    @Override
    public List<SpanishExpression> searchSpanishExpressions(String databaseName, String searchText) {
        return resolveCanonicalDatabaseKey(databaseName)
                .map(canonical -> spanishDatabases.get(canonical).stream()
                        .filter(spanishExpr -> spanishExpr.getExpression().toLowerCase()
                                .contains(searchText.toLowerCase()))
                        .collect(Collectors.toList()))
                .orElseGet(ArrayList::new);
    }
    
    @Override
    public List<EnglishExpression> searchEnglishExpressions(String databaseName, String searchText) {
        return resolveCanonicalDatabaseKey(databaseName)
                .map(canonical -> englishDatabases.get(canonical).stream()
                        .filter(englishExpr -> englishExpr.getExpression().toLowerCase()
                                .contains(searchText.toLowerCase()))
                        .collect(Collectors.toList()))
                .orElseGet(ArrayList::new);
    }
    
    @Override
    public String getLearnedWordsDatabaseName() {
        return LEARNED_WORDS_DATABASE;
    }
    
    @Override
    public boolean moveToLearnedWords(EnglishExpression englishExpression) {
        if (englishExpression == null) {
            log.warn("Cannot move null English expression to learned words");
            return false;
        }

        englishExpression.setIncludedAtEpochMillis(System.currentTimeMillis());

        boolean added = addEnglishExpression(LEARNED_WORDS_DATABASE, englishExpression);
        if (added) {
            log.info("Moved English expression '{}' to learned words database", 
                    englishExpression.getExpression());
            // Persist to JSON
            gameDataService.saveGameData();
        }
        
        return added;
    }
    
    @Override
    public boolean promoteTranslationToLearned(String practiceDatabaseName, SpanishExpression hostPhrase,
                                              EnglishExpression englishTranslation) {
        if (englishTranslation == null || hostPhrase == null || practiceDatabaseName == null
                || practiceDatabaseName.isBlank()) {
            log.warn("promoteTranslationToLearned: invalid arguments");
            return false;
        }
        Optional<String> practiceKey = resolveCanonicalDatabaseKey(practiceDatabaseName);
        if (practiceKey.isEmpty()) {
            log.warn("Practice database '{}' does not exist", practiceDatabaseName);
            return false;
        }
        if (ReviewDatabases.isReviewDatabaseKey(practiceKey.get())) {
            log.warn("Cannot promote from a review-only database");
            return false;
        }
        
        String practiceDb = practiceKey.get();
        Set<SpanishExpression> practicePhrases = spanishDatabases.get(practiceDb);
        if (!practicePhrases.contains(hostPhrase)) {
            log.warn("Host phrase '{}' not found in practice database '{}'",
                    hostPhrase.getExpression(), practiceDb);
            return false;
        }

        String promotedEnTrimmed = englishTranslation.getExpression().trim();
        if (promotedEnTrimmed.isEmpty()) {
            log.warn("promoteTranslationToLearned: empty English expression");
            return false;
        }

        String normalizedHostSpanish = normalize(hostPhrase.getExpression());
        List<SpanishExpression> cohortBySpanishPhrase = practicePhrases.stream()
                .filter(expr -> normalize(expr.getExpression()).equals(normalizedHostSpanish))
                .collect(Collectors.toList());

        boolean translationExistsSomewhere = cohortBySpanishPhrase.stream()
                .map(SpanishExpression::getTranslations)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .anyMatch(en -> en.getExpression() != null && en.getExpression().trim().equalsIgnoreCase(promotedEnTrimmed));
        if (!translationExistsSomewhere) {
            log.warn("English '{}' not found under any '{}' row(s) in '{}'",
                    englishTranslation.getExpression(),
                    Optional.ofNullable(hostPhrase.getExpression()).orElse("(null)"), practiceDb);
            return false;
        }

        // Keep source Spanish phrase for Learned Words UI and JSON round-trip (spanish_sources)
        String hostExpr = Optional.ofNullable(hostPhrase.getExpression()).map(String::trim).orElse("");
        boolean hasSameSource = englishTranslation.getTranslations() != null && englishTranslation.getTranslations()
                .stream()
                .anyMatch(sp -> sp != null && expressionsEqualNormalized(sp.getExpression(), hostExpr));
        if (!hostExpr.isEmpty() && !hasSameSource) {
            SpanishExpression link = new SpanishExpression();
            link.setExpression(hostExpr);
            link.setScore(0);
            englishTranslation.getTranslations().add(link);
        }
        englishTranslation.setPracticeSourceDatabase(practiceDb);

        int phrasesTouched = 0;
        for (SpanishExpression expr : cohortBySpanishPhrase) {
            if (expr.getTranslations() != null && expr.getTranslations().removeIf(en -> en != null
                    && en.getExpression() != null
                    && en.getExpression().trim().equalsIgnoreCase(promotedEnTrimmed))) {
                phrasesTouched++;
            }
        }

        Set<EnglishExpression> learned = englishDatabases.get(LEARNED_WORDS_DATABASE);
        learned.removeIf(en -> en.getExpression().equalsIgnoreCase(englishTranslation.getExpression()));
        englishTranslation.setIncludedAtEpochMillis(System.currentTimeMillis());
        learned.add(englishTranslation);

        // Quitar también filas inglés sueltas duplicadas (misma gráfía) que queden en esta BBDD de práctica.
        Set<EnglishExpression> practiceEnglish = englishDatabases.get(practiceDb);
        if (practiceEnglish != null && !practiceEnglish.isEmpty()) {
            practiceEnglish.removeIf(en -> en != null && en.getExpression() != null
                    && en.getExpression().trim().equalsIgnoreCase(promotedEnTrimmed));
        }

        for (SpanishExpression expr : cohortBySpanishPhrase) {
            List<EnglishExpression> tr = expr.getTranslations();
            if (tr == null || tr.isEmpty()) {
                practicePhrases.remove(expr);
                log.debug("Removed emptied Spanish '{}' from '{}'", expr.getExpression(), practiceDb);
            }
        }

        // Cualquier otra fila ES con ese texto y sin traducciones válidas no debe quedar vestigio.
        practicePhrases.removeIf(expr -> normalize(expr.getExpression()).equals(normalizedHostSpanish)
                && translationsEffectivelyEmpty(expr));

        gameDataService.saveGameData();
        log.info(
                "Learned '{}' moved to '{}' and removed from '{}' ({} Spanish row(s); {} row(s) had that translation). No duplicate EN left under same phrase.",
                englishTranslation.getExpression(), LEARNED_WORDS_DATABASE, practiceDb,
                cohortBySpanishPhrase.size(), phrasesTouched);
        return true;
    }

    @Override
    public Optional<LearnedWordsReviewResult> submitLearnedWordsReviewAttempt(EnglishExpression learnedCard,
            String userAnswer, String reviewDatabaseName, boolean requirePracticeSourceMatch,
            String userSelectedPracticeDatabase) {
        if (learnedCard == null || userAnswer == null) {
            return Optional.empty();
        }
        Optional<String> reviewDbKey = resolveReviewDatabaseKey(reviewDatabaseName);
        if (reviewDbKey.isEmpty()) {
            log.warn("Review: base de datos no válida: {}", reviewDatabaseName);
            return Optional.empty();
        }
        String reviewDb = reviewDbKey.get();
        Set<EnglishExpression> learnedBucket = englishDatabases.get(reviewDb);
        if (learnedBucket == null || !learnedBucket.contains(learnedCard)) {
            log.warn("Review: la tarjeta no está en '{}' (referencia inválida o ya movida).", reviewDb);
            return Optional.empty();
        }
        String expectedRaw = Optional.ofNullable(learnedCard.getExpression()).map(String::trim).orElse("");
        String typed = squashWhitespace(userAnswer);
        boolean expressionOk = !expectedRaw.isEmpty()
                && squashWhitespace(expectedRaw).equalsIgnoreCase(typed);

        String expectedSourceLabel = null;
        String userSourceLabel = null;
        boolean sourceOk = true;
        if (requirePracticeSourceMatch) {
            expectedSourceLabel = formatPracticeSourceForReview(learnedCard.getPracticeSourceDatabase());
            userSourceLabel = formatPracticeSourceForReview(userSelectedPracticeDatabase);
            sourceOk = practiceSourceMatches(learnedCard, userSelectedPracticeDatabase);
        }

        boolean ok = expressionOk && sourceOk;
        int prior = learnedCard.getScore();
        boolean definitelyReview = WORDS_DEFINITELY_LEARNED_DATABASE.equals(reviewDb);

        if (ok) {
            int s = prior + 1;
            learnedCard.setScore(s);
            if (definitelyReview) {
                if (s >= DEFINITELY_REVIEW_MASTER_AT) {
                    learnedBucket.remove(learnedCard);
                    definitelyMasteredTotal++;
                    purgeEnglishLemmaEverywhere(expectedRaw);
                    pruneSpanishRowsWithoutTranslations();
                    gameDataService.saveGameData();
                    return Optional.of(reviewResult(
                            LearnedWordsReviewResult.Outcome.MASTERED_REMOVED_EVERYWHERE,
                            true, DEFINITELY_REVIEW_MASTER_AT, expectedRaw, typed, null,
                            true, expectedSourceLabel, userSourceLabel));
                }
                gameDataService.saveGameData();
                return Optional.of(reviewResult(
                        LearnedWordsReviewResult.Outcome.STILL_IN_LEARNED, true, s, expectedRaw, typed, null,
                        true, expectedSourceLabel, userSourceLabel));
            }
            if (s >= LEARNED_REVIEW_GRADUATE_TO_DEFINITELY_AT) {
                learnedCard.setScore(LEARNED_REVIEW_GRADUATE_TO_DEFINITELY_AT);
                if (!promoteLearnedCardToDefinitelyLearned(learnedCard, learnedBucket)) {
                    learnedCard.setScore(prior);
                    gameDataService.saveGameData();
                    JOptionPane.showMessageDialog(null,
                            "No se pudo mover la expresión a Words definitely learned.",
                            "Review Learned Words", JOptionPane.WARNING_MESSAGE);
                    return Optional.of(reviewResult(
                            LearnedWordsReviewResult.Outcome.STILL_IN_LEARNED, true, prior, expectedRaw, typed, null,
                            true, expectedSourceLabel, userSourceLabel));
                }
                gameDataService.saveGameData();
                return Optional.of(reviewResult(
                        LearnedWordsReviewResult.Outcome.PROMOTED_TO_DEFINITELY_LEARNED,
                        true,
                        LEARNED_REVIEW_GRADUATE_TO_DEFINITELY_AT,
                        expectedRaw, typed, null,
                        true, expectedSourceLabel, userSourceLabel));
            }
            gameDataService.saveGameData();
            return Optional.of(reviewResult(
                    LearnedWordsReviewResult.Outcome.STILL_IN_LEARNED, true, s, expectedRaw, typed, null,
                    true, expectedSourceLabel, userSourceLabel));
        }

        int penalized = Math.max(0, prior - 5);
        learnedCard.setScore(penalized);
        if (penalized < LEARNED_REVIEW_DEMOTION_UNDER) {
            if (demoteLearnedCardToPractice(learnedCard, penalized, learnedBucket)) {
                pruneSpanishRowsWithoutTranslations();
                gameDataService.saveGameData();
                String restoredDb = Optional.ofNullable(learnedCard.getPracticeSourceDatabase())
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .orElse(null);
                return Optional.of(reviewResult(
                        LearnedWordsReviewResult.Outcome.DEMOTED_TO_PRACTICE,
                        false,
                        penalized,
                        expectedRaw, typed, restoredDb,
                        expressionOk, expectedSourceLabel, userSourceLabel));
            }
            learnedCard.setScore(prior);
            gameDataService.saveGameData();
            JOptionPane.showMessageDialog(null,
                    "No se pudo devolver la expresión a la base de práctica (sin frase español enlazada o sin BBDD de destino). El score anterior se mantuvo.",
                    "Review Learned Words", JOptionPane.WARNING_MESSAGE);
            return Optional.of(reviewResult(
                    LearnedWordsReviewResult.Outcome.STILL_IN_LEARNED, false, prior, expectedRaw, typed, null,
                    expressionOk, expectedSourceLabel, userSourceLabel));
        }

        gameDataService.saveGameData();
        return Optional.of(reviewResult(
                LearnedWordsReviewResult.Outcome.STILL_IN_LEARNED, false, penalized, expectedRaw, typed, null,
                expressionOk, expectedSourceLabel, userSourceLabel));
    }

    private static LearnedWordsReviewResult reviewResult(
            LearnedWordsReviewResult.Outcome outcome,
            boolean answeredCorrectly,
            int scoreAfter,
            String expectedEnglish,
            String userEntered,
            String restoredToPracticeDatabase,
            boolean expressionMatched,
            String expectedPracticeSourceDatabase,
            String userPracticeSourceDatabase) {
        return new LearnedWordsReviewResult(
                outcome,
                answeredCorrectly,
                scoreAfter,
                expectedEnglish,
                userEntered,
                restoredToPracticeDatabase,
                expressionMatched,
                expectedPracticeSourceDatabase,
                userPracticeSourceDatabase);
    }

    private boolean practiceSourceMatches(EnglishExpression card, String userSelectedPracticeDatabase) {
        if (userSelectedPracticeDatabase == null || userSelectedPracticeDatabase.isBlank()) {
            return false;
        }
        String expected = Optional.ofNullable(card.getPracticeSourceDatabase()).map(String::trim).orElse("");
        if (expected.isEmpty()) {
            return false;
        }
        Optional<String> expectedCanon = resolveCanonicalDatabaseKey(expected);
        Optional<String> selectedCanon = resolveCanonicalDatabaseKey(userSelectedPracticeDatabase.trim());
        if (expectedCanon.isPresent() && selectedCanon.isPresent()) {
            return expectedCanon.get().equalsIgnoreCase(selectedCanon.get());
        }
        return expected.equalsIgnoreCase(userSelectedPracticeDatabase.trim());
    }

    private String formatPracticeSourceForReview(String databaseName) {
        if (databaseName == null || databaseName.isBlank()) {
            return "";
        }
        return resolveCanonicalDatabaseKey(databaseName.trim())
                .flatMap(key -> getAvailableDatabases().stream()
                        .filter(db -> resolveCanonicalDatabaseKey(db)
                                .map(k -> k.equalsIgnoreCase(key))
                                .orElse(false))
                        .findFirst())
                .orElse(databaseName.trim());
    }

    private boolean promoteLearnedCardToDefinitelyLearned(EnglishExpression card,
            Set<EnglishExpression> learnedBucket) {
        Set<EnglishExpression> definitelyBucket = englishDatabases.get(WORDS_DEFINITELY_LEARNED_DATABASE);
        if (definitelyBucket == null) {
            return false;
        }
        String phrase = Optional.ofNullable(card.getExpression()).map(String::trim).orElse("");
        if (!phrase.isEmpty() && definitelyBucket.stream().anyMatch(e -> e != null && e.getExpression() != null
                && e.getExpression().trim().equalsIgnoreCase(phrase))) {
            log.warn("Review promotion: '{}' already in words_definitely_learned", phrase);
            return false;
        }
        if (!learnedBucket.remove(card)) {
            return false;
        }
        definitelyBucket.add(card);
        return true;
    }

    /** Normaliza espacios internos y recorta; comparación típica de respuesta libre. */
    private static String squashWhitespace(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return "";
        }
        return String.join(" ", t.split("\\s+"));
    }

    private boolean demoteLearnedCardToPractice(EnglishExpression card, int penalizedLearnedScore,
            Set<EnglishExpression> learnedBucket) {
        String canonicalDb = resolveCanonicalDatabaseKey(
                Optional.ofNullable(card.getPracticeSourceDatabase()).orElse("")).orElse("");
        if (canonicalDb.isEmpty() || ReviewDatabases.isReviewDatabaseKey(canonicalDb)) {
            Optional<String> auto = resolveAutomaticPracticeDatabaseForDemotion(card);
            if (auto.isEmpty()) {
                JOptionPane.showMessageDialog(null,
                        "No hay ninguna base de vocabulario donde reincorporar la expresión.",
                        "Review Learned Words", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            canonicalDb = auto.get();
            card.setPracticeSourceDatabase(canonicalDb);
            log.info("Review demotion: reincorporación automática en BBDD de práctica '{}'", canonicalDb);
        }
        Set<SpanishExpression> practicePhrases = spanishDatabases.get(canonicalDb);
        if (practicePhrases == null) {
            return false;
        }
        String spanishPhrase = firstSpanishPhraseFor(card);
        if (spanishPhrase == null || spanishPhrase.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Esta entrada en learned_words no lleva texto español enlazado; no se puede reubicar.",
                    "Review Learned Words", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        int practiceScore = penalizedLearnedScore;
        int rollbackLearnedScore = penalizedLearnedScore;
        card.setScore(practiceScore);
        if (!learnedBucket.remove(card)) {
            return false;
        }
        boolean attached = attachEnglishUnderSpanishPhrase(canonicalDb, spanishPhrase.trim(), card);
        if (!attached) {
            learnedBucket.add(card);
            card.setScore(rollbackLearnedScore);
            return false;
        }
        Set<EnglishExpression> practiceStandalone = englishDatabases.get(canonicalDb);
        if (practiceStandalone != null) {
            practiceStandalone.removeIf(e -> e != null && e.getExpression() != null && e.getExpression()
                    .trim()
                    .equalsIgnoreCase(Optional.ofNullable(card.getExpression()).orElse("").trim()));
        }
        return true;
    }

    private static String firstSpanishPhraseFor(EnglishExpression card) {
        if (card.getTranslations() == null) {
            return null;
        }
        return card.getTranslations().stream()
                .map(SpanishExpression::getExpression)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .findFirst()
                .orElse(null);
    }

    /**
     * Cuando {@code practice_source_database} falta o es inválido, elige una BBDD de práctica sin preguntar al usuario:
     * si la frase español enlazada existe en una sola BBDD candidata, esa; si no, la primera disponible (orden estable).
     */
    private Optional<String> resolveAutomaticPracticeDatabaseForDemotion(EnglishExpression card) {
        List<String> displayNames = getAvailableDatabases().stream()
                .filter(Objects::nonNull)
                .filter(name -> !ReviewDatabases.isReviewDatabaseKey(name))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
        if (displayNames.isEmpty()) {
            return Optional.empty();
        }
        List<String> canonicalOrdered = displayNames.stream()
                .map(d -> resolveCanonicalDatabaseKey(d).orElse(d.trim()))
                .filter(d -> !d.isEmpty())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
        if (canonicalOrdered.isEmpty()) {
            return Optional.empty();
        }
        if (canonicalOrdered.size() == 1) {
            return Optional.of(canonicalOrdered.get(0));
        }
        String phrase = firstSpanishPhraseFor(card);
        if (phrase != null && !phrase.trim().isEmpty()) {
            String norm = normalize(phrase.trim());
            List<String> hits = new ArrayList<>();
            for (String canon : canonicalOrdered) {
                Set<SpanishExpression> phrases = spanishDatabases.get(canon);
                if (phrases == null) {
                    continue;
                }
                boolean found = phrases.stream()
                        .filter(Objects::nonNull)
                        .anyMatch(p -> p.getExpression() != null && normalize(p.getExpression()).equals(norm));
                if (found) {
                    hits.add(canon);
                }
            }
            if (hits.size() == 1) {
                return Optional.of(hits.get(0));
            }
            if (hits.size() > 1) {
                log.debug("Demotion resolve: '{}' aparece en varias bases; usando la primera estable.", norm);
            }
        }
        return Optional.of(canonicalOrdered.get(0));
    }

    /**
     * Inserta o fusiona un inglés bajo una frase español en la BD de práctica indicada por clave canónica.
     */
    private boolean attachEnglishUnderSpanishPhrase(String canonicalDb, String trimmedSpanish,
            EnglishExpression english) {
        Set<SpanishExpression> phrases = spanishDatabases.get(canonicalDb);
        if (phrases == null || english == null || english.getExpression() == null) {
            return false;
        }
        String es = trimmedSpanish.trim();
        if (es.isEmpty()) {
            return false;
        }
        String norm = normalize(es);
        SpanishExpression host = phrases.stream()
                .filter(p -> p != null && p.getExpression() != null && normalize(p.getExpression()).equals(norm))
                .findFirst()
                .orElse(null);
        if (host == null) {
            host = new SpanishExpression();
            host.setExpression(es);
            host.setScore(english.getScore());
            List<EnglishExpression> list = new ArrayList<>();
            list.add(english);
            host.setTranslations(list);
            phrases.add(host);
            return true;
        }
        if (host.getTranslations() == null) {
            host.setTranslations(new ArrayList<>());
        }
        for (EnglishExpression ex : host.getTranslations()) {
            if (ex != null && ex.getExpression() != null && ex.getExpression().trim()
                    .equalsIgnoreCase(english.getExpression().trim())) {
                ex.setScore(english.getScore());
                host.setScore(Math.max(host.getScore(), english.getScore()));
                return true;
            }
        }
        host.getTranslations().add(english);
        host.setScore(Math.max(host.getScore(), english.getScore()));
        return true;
    }

    /**
     * Elimina el lema inglés como fila standalone y como traducción anidada en todas las BD (learned incluido).
     */
    private void purgeEnglishLemmaEverywhere(String englishLemmaRaw) {
        if (englishLemmaRaw == null) {
            return;
        }
        String canon = squashWhitespace(englishLemmaRaw).trim().toLowerCase(Locale.ROOT);
        if (canon.isEmpty()) {
            return;
        }
        for (String db : new ArrayList<>(englishDatabases.keySet())) {
            Set<EnglishExpression> bucket = englishDatabases.get(db);
            if (bucket == null) {
                continue;
            }
            bucket.removeIf(e -> e != null && e.getExpression() != null
                    && squashWhitespace(e.getExpression()).trim().toLowerCase(Locale.ROOT).equals(canon));
        }
        for (String db : new ArrayList<>(spanishDatabases.keySet())) {
            Set<SpanishExpression> spans = spanishDatabases.get(db);
            if (spans == null) {
                continue;
            }
            List<SpanishExpression> snapshot = new ArrayList<>(spans);
            for (SpanishExpression sp : snapshot) {
                if (sp.getTranslations() != null) {
                    sp.getTranslations().removeIf(en -> en != null && en.getExpression() != null
                            && squashWhitespace(en.getExpression()).trim().toLowerCase(Locale.ROOT).equals(canon));
                }
                if (translationsEffectivelyEmpty(sp)) {
                    spans.remove(sp);
                }
            }
        }
    }
    
    @Override
    public List<EnglishExpression> getLearnedExpressions() {
        return getEnglishExpressions(LEARNED_WORDS_DATABASE);
    }
    
    @Override
    public void synchronizeWithRepository() {
        log.info("Synchronizing database service with repository data...");
        loadingFromRepository = true;
        try {
            spanishDatabases.clear();
            englishDatabases.clear();
            definitelyMasteredTotal = 0;
            initializeDefaultDatabases();
            loadDataFromRepository();
            log.info("Database synchronization completed. Available databases: {}", getAvailableDatabases());
        } finally {
            loadingFromRepository = false;
        }
    }

    /** Solo memoria: usado al cargar JSON; no persiste (evita sobrescribir game_data.json a medias). */
    private void ensureDatabaseBucketsInMemory(String databaseName) {
        if (databaseName == null || databaseName.trim().isEmpty()) {
            return;
        }
        String name = databaseName.trim();
        if (resolveCanonicalDatabaseKey(name).isPresent()) {
            return;
        }
        spanishDatabases.put(name, new HashSet<>());
        englishDatabases.put(name, new HashSet<>());
        log.debug("Ensured in-memory buckets for database '{}'", name);
    }
    
    private void initializeDefaultDatabases() {
        /*
         * In-memory only: do not call createDatabase() here. That method writes JSON, but Main
         * constructs DatabaseServiceImpl before GameController runs loadGameData(), and until then
         * GameDataServiceImpl has no databaseService, so saveGameData falls back to a nearly
         * empty repository and overwrites game_data.json.
         */
        spanishDatabases.put(LEARNED_WORDS_DATABASE, new HashSet<>());
        englishDatabases.put(LEARNED_WORDS_DATABASE, new HashSet<>());
        spanishDatabases.put(WORDS_DEFINITELY_LEARNED_DATABASE, new HashSet<>());
        englishDatabases.put(WORDS_DEFINITELY_LEARNED_DATABASE, new HashSet<>());
        log.info("Initialized review databases: {}, {}", LEARNED_WORDS_DATABASE, WORDS_DEFINITELY_LEARNED_DATABASE);
    }

    private Optional<String> resolveReviewDatabaseKey(String reviewDatabaseName) {
        if (reviewDatabaseName == null || reviewDatabaseName.isBlank()) {
            return Optional.empty();
        }
        String trimmed = reviewDatabaseName.trim();
        if (ReviewDatabases.isReviewDatabaseKey(trimmed)) {
            return resolveCanonicalDatabaseKey(trimmed);
        }
        String fromDisplay = ReviewDatabases.keyForDisplayName(trimmed);
        return resolveCanonicalDatabaseKey(fromDisplay).filter(ReviewDatabases::isReviewDatabaseKey);
    }
    
    /**
     * Loads data from the GameDataService repository and creates corresponding databases
     */
    private void loadDataFromRepository() {
        try {
            // Get all data from repository
            List<List<Map<String, Object>>> allData = gameDataService.getAllData();
            
            if (allData == null || allData.isEmpty()) {
                log.debug("No data found in repository to load");
                return;
            }
            
            // Process each record
            for (List<Map<String, Object>> record : allData) {
                if (record == null || record.isEmpty()) {
                    continue;
                }
                
                // Check if this is a database metadata record
                Map<String, Object> firstMap = record.get(0);
                if ("database_metadata".equals(firstMap.get("type"))) {
                    String databaseName = (String) firstMap.get("database");
                    if (databaseName != null && !databaseName.trim().isEmpty()) {
                        ensureDatabaseBucketsInMemory(databaseName);
                        if (WORDS_DEFINITELY_LEARNED_DATABASE.equalsIgnoreCase(databaseName.trim())) {
                            definitelyMasteredTotal = getIntValue(firstMap,
                                    ReviewDatabases.METADATA_DEFINITELY_MASTERED_TOTAL, definitelyMasteredTotal);
                        }
                        log.debug("Ensured database '{}' from loaded metadata", databaseName);
                    }
                } else {
                    // This is an expression record, find its database
                    String databaseName = (String) firstMap.get("database");
                    String language = (String) firstMap.get("language");
                    String expression = (String) firstMap.get("expression");
                    
                    if (databaseName != null && language != null && expression != null) {
                        ensureDatabaseBucketsInMemory(databaseName);

                        Optional<String> dbKey = resolveCanonicalDatabaseKey(databaseName);
                        if (dbKey.isEmpty()) {
                            log.warn("Could not resolve database key for '{}' while loading JSON", databaseName);
                            continue;
                        }
                        
                        if (resolveAppGameMode().matchesPromptLanguage(language)) {
                            // Prompt card (Spanish in classic mode, English definition in definition mode)
                            SpanishExpression spanishExpr = new SpanishExpression();
                            spanishExpr.setExpression(expression);
                            spanishExpr.setScore(getIntValue(firstMap, "score", 0));
                            
                            // Add translations if they exist
                            Object translationsObj = firstMap.get("translations");
                            if (translationsObj instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<String> translations = (List<String>) translationsObj;
                                for (String translation : translations) {
                                    if (translation == null || translation.trim().isEmpty()) {
                                        continue;
                                    }
                                    EnglishExpression englishExpr = new EnglishExpression();
                                    englishExpr.setExpression(translation);
                                    englishExpr.setScore(getIntValue(firstMap, "score", 0));
                                    spanishExpr.getTranslations().add(englishExpr);
                                }
                            }
                            
                            spanishExpr.setIncludedAtEpochMillis(getLongValue(firstMap, "included_at", 0L));
                            if (translationsEffectivelyEmpty(spanishExpr)) {
                                log.warn("Skipping load of Spanish '{}' in '{}': no non-blank translations",
                                        expression, databaseName);
                                continue;
                            }
                            addSpanishExpression(databaseName, spanishExpr);
                            log.debug("Loaded Spanish expression '{}' into database '{}'", expression, databaseName);
                        } else if ("english".equals(language)) {
                            EnglishExpression en = englishExpressionFromLoadedMap(expression, firstMap);
                            englishDatabases.get(dbKey.get()).add(en);
                            log.debug("Loaded standalone English '{}' into database '{}'", expression, dbKey.get());
                        }
                    }
                }
            }
            
            log.info("Successfully loaded {} records from repository", allData.size());
            
        } catch (Exception e) {
            log.error("Error loading data from repository: {}", e.getMessage());
        }
    }
    
    /**
     * Helper method to safely get integer values from map
     */
    private long getLongValue(Map<String, Object> map, String key, long defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }

    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /** Rehydrates a lone {@code english_expression} row (e.g. learned words) from persisted JSON. */
    private EnglishExpression englishExpressionFromLoadedMap(String expressionText, Map<String, Object> row) {
        EnglishExpression en = new EnglishExpression();
        en.setExpression(expressionText.trim());
        en.setScore(getIntValue(row, "score", 0));
        Object ss = row.get("spanish_sources");
        if (ss instanceof List<?>) {
            for (Object o : (List<?>) ss) {
                if (o instanceof String str) {
                    String trimmed = str.trim();
                    if (!trimmed.isEmpty()) {
                        SpanishExpression sp = new SpanishExpression();
                        sp.setExpression(trimmed);
                        sp.setScore(0);
                        en.getTranslations().add(sp);
                    }
                }
            }
        }
        en.setIncludedAtEpochMillis(getLongValue(row, "included_at", 0L));
        Object psDb = row.get("practice_source_database");
        if (psDb instanceof String s && !s.trim().isEmpty()) {
            en.setPracticeSourceDatabase(resolveCanonicalDatabaseKey(s.trim()).orElse(s.trim()));
        }
        return en;
    }
    
    /**
     * Saves database metadata to the repository for persistence
     */
    private void enrichReviewDatabaseMetadata(String databaseName, Map<String, Object> metadata) {
        if (WORDS_DEFINITELY_LEARNED_DATABASE.equalsIgnoreCase(databaseName)) {
            metadata.put(ReviewDatabases.METADATA_DEFINITELY_MASTERED_TOTAL, definitelyMasteredTotal);
        }
    }

    private void saveDatabaseMetadataToRepository(String databaseName) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("type", "database_metadata");
            metadata.put("database", databaseName);
            metadata.put("created_at", System.currentTimeMillis());
            enrichReviewDatabaseMetadata(databaseName, metadata);

            List<Map<String, Object>> record = Arrays.asList(metadata);
            gameDataService.getRepository().save(record);
            
            log.debug("Database metadata saved to repository for '{}'", databaseName);
        } catch (Exception e) {
            log.error("Error saving database metadata to repository: {}", e.getMessage());
        }
    }
    
    /**
     * Saves Spanish expression to the repository for persistence
     */
    private void saveExpressionToRepository(String databaseName, SpanishExpression spanishExpression) {
        try {
            Map<String, Object> expressionData = new HashMap<>();
            populatePromptExpressionMap(expressionData, databaseName, spanishExpression);

            List<Map<String, Object>> record = Arrays.asList(expressionData);
            gameDataService.getRepository().save(record);

            log.debug("Prompt expression '{}' saved to repository", spanishExpression.getExpression());
        } catch (Exception e) {
            log.error("Error saving Spanish expression to repository: {}", e.getMessage());
        }
    }
    
    /**
     * Removes database and all its expressions from the repository
     * Uses the same strategy as saveGameData() to ensure consistency
     */
    private void removeDatabaseFromRepository(String databaseName) {
        try {
            // Instead of manipulating repository directly, we let GameDataService
            // rebuild the repository from current database state
            // This ensures consistency and avoids data corruption
            
            // Clear the repository completely
            gameDataService.getRepository().clear();
            
            // Rebuild repository from current database state (excluding deleted database)
            List<String> databases = getAvailableDatabases();
            
            for (String dbName : databases) {
                // Skip the database being deleted (it's already removed from memory)
                if (databaseName.equals(dbName)) {
                    continue;
                }
                
                // Add database metadata
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("type", "database_metadata");
                metadata.put("database", dbName);
                metadata.put("created_at", System.currentTimeMillis());
                enrichReviewDatabaseMetadata(dbName, metadata);

                List<Map<String, Object>> record = Arrays.asList(metadata);
                gameDataService.getRepository().save(record);

                // Add all Spanish expressions from this database
                List<SpanishExpression> spanishExpressions = getSpanishExpressions(dbName);
                for (SpanishExpression spanishExpr : spanishExpressions) {
                    Map<String, Object> expressionData = new HashMap<>();
                    populatePromptExpressionMap(expressionData, dbName, spanishExpr);

                    List<Map<String, Object>> exprRecord = Arrays.asList(expressionData);
                    gameDataService.getRepository().save(exprRecord);
                }

                List<EnglishExpression> loneEnglish = getEnglishExpressions(dbName);
                for (EnglishExpression en : loneEnglish) {
                    Map<String, Object> englishRow = new HashMap<>();
                    englishRow.put("type", "english_expression");
                    englishRow.put("database", dbName);
                    englishRow.put("language", "english");
                    englishRow.put("expression", en.getExpression());
                    englishRow.put("score", en.getScore());
                    List<String> spanishSources = new ArrayList<>();
                    if (en.getTranslations() != null) {
                        for (SpanishExpression sp : en.getTranslations()) {
                            if (sp != null && sp.getExpression() != null && !sp.getExpression().trim().isEmpty()) {
                                spanishSources.add(sp.getExpression().trim());
                            }
                        }
                    }
                    englishRow.put("spanish_sources", spanishSources);
                    englishRow.put("included_at", en.getIncludedAtEpochMillis());
                    if (en.getPracticeSourceDatabase() != null && !en.getPracticeSourceDatabase().trim().isEmpty()) {
                        englishRow.put("practice_source_database", en.getPracticeSourceDatabase().trim());
                    }
                    gameDataService.getRepository().save(Arrays.asList(englishRow));
                }
            }

            log.debug("Database '{}' and all its expressions removed from repository", databaseName);
        } catch (Exception e) {
            log.error("Error removing database from repository: {}", e.getMessage());
        }
    }
    
    @Override
    public boolean moveSpanishExpression(String sourceDatabase, String targetDatabase, String expression) {
        Optional<String> sourceOpt = resolveCanonicalDatabaseKey(sourceDatabase);
        Optional<String> targetOpt = resolveCanonicalDatabaseKey(targetDatabase);
        if (sourceOpt.isEmpty() || targetOpt.isEmpty() || sourceOpt.get().equals(targetOpt.get())) {
            log.warn("Cannot move Spanish expression '{}' from '{}' to '{}' - invalid databases",
                    expression, sourceDatabase, targetDatabase);
            return false;
        }
        String sourceDb = sourceOpt.get();
        String targetDb = targetOpt.get();

        Optional<SpanishExpression> spanishExpr = spanishDatabases.get(sourceDb).stream()
                .filter(expr -> expressionsEqualNormalized(expr.getExpression(), expression))
                .findFirst();

        if (spanishExpr.isEmpty()) {
            log.warn("Spanish expression '{}' not found in source database '{}'", expression, sourceDb);
            return false;
        }

        SpanishExpression moved = spanishExpr.get();
        String phrase = moved.getExpression() != null ? moved.getExpression().trim() : "";
        if (spanishDatabases.get(targetDb).contains(moved)) {
            log.warn("Target database '{}' already has exact duplicate for Spanish '{}'; move cancelled",
                    targetDb, phrase);
            return false;
        }

        spanishDatabases.get(sourceDb).remove(moved);
        spanishDatabases.get(targetDb).add(moved);

        updateRepositoryAfterMove(sourceDb, targetDb, moved, "spanish");
        gameDataService.saveGameData();

        log.info("Spanish expression '{}' moved from '{}' to '{}'", phrase, sourceDb, targetDb);
        return true;
    }
    
    @Override
    public boolean moveEnglishExpression(String sourceDatabase, String targetDatabase, String expression) {
        Optional<String> sourceOpt = resolveCanonicalDatabaseKey(sourceDatabase);
        Optional<String> targetOpt = resolveCanonicalDatabaseKey(targetDatabase);
        if (sourceOpt.isEmpty() || targetOpt.isEmpty() || sourceOpt.get().equals(targetOpt.get())) {
            log.warn("Cannot move English expression '{}' from '{}' to '{}' - invalid databases",
                    expression, sourceDatabase, targetDatabase);
            return false;
        }
        String sourceDb = sourceOpt.get();
        String targetDb = targetOpt.get();

        Optional<EnglishExpression> englishExpr = englishDatabases.get(sourceDb).stream()
                .filter(expr -> expressionsEqualNormalized(expr.getExpression(), expression))
                .findFirst();

        if (englishExpr.isEmpty()) {
            log.warn("English expression '{}' not found in source database '{}'", expression, sourceDb);
            return false;
        }

        EnglishExpression moved = englishExpr.get();
        String phrase = moved.getExpression() != null ? moved.getExpression().trim() : "";
        if (ReviewDatabases.isReviewDatabaseKey(targetDb) && !targetDb.equalsIgnoreCase(sourceDb)) {
            moved.setPracticeSourceDatabase(sourceDb);
        }
        if (englishDatabases.get(targetDb).contains(moved)) {
            log.warn("Target database '{}' already has exact duplicate for English '{}'; move cancelled",
                    targetDb, phrase);
            return false;
        }

        englishDatabases.get(sourceDb).remove(moved);
        englishDatabases.get(targetDb).add(moved);

        updateRepositoryAfterMove(sourceDb, targetDb, moved, "english");
        gameDataService.saveGameData();

        log.info("English expression '{}' moved from '{}' to '{}'", phrase, sourceDb, targetDb);
        return true;
    }
    
    /**
     * Updates repository after moving an expression between databases
     */
    private void updateRepositoryAfterMove(String sourceDb, String targetDb, Object expression, String type) {
        try {
            // Clear repository and rebuild from current state
            gameDataService.getRepository().clear();
            
            // Rebuild repository from current database state
            List<String> databases = getAvailableDatabases();
            
            for (String dbName : databases) {
                // Add database metadata
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("type", "database_metadata");
                metadata.put("database", dbName);
                metadata.put("created_at", System.currentTimeMillis());
                enrichReviewDatabaseMetadata(dbName, metadata);

                List<Map<String, Object>> record = Arrays.asList(metadata);
                gameDataService.getRepository().save(record);

                // Add all Spanish expressions from this database
                List<SpanishExpression> spanishExpressions = getSpanishExpressions(dbName);
                for (SpanishExpression spanishExpr : spanishExpressions) {
                    saveExpressionToRepository(dbName, spanishExpr);
                }
                
                // Add all English expressions from this database
                List<EnglishExpression> englishExpressions = getEnglishExpressions(dbName);
                for (EnglishExpression englishExpr : englishExpressions) {
                    saveEnglishExpressionToRepository(dbName, englishExpr);
                }
            }
            
            log.debug("Repository updated after moving {} expression from '{}' to '{}'", type, sourceDb, targetDb);
        } catch (Exception e) {
            log.error("Error updating repository after move: {}", e.getMessage());
        }
    }
    
    /**
     * Saves English expression to the repository for persistence
     */
    private void saveEnglishExpressionToRepository(String databaseName, EnglishExpression englishExpression) {
        try {
            Map<String, Object> expressionData = new HashMap<>();
            expressionData.put("type", "english_expression");
            expressionData.put("database", databaseName);
            expressionData.put("language", "english");
            expressionData.put("expression", englishExpression.getExpression());
            expressionData.put("score", englishExpression.getScore());
            expressionData.put("included_at", englishExpression.getIncludedAtEpochMillis());
            List<String> spanishSources = new ArrayList<>();
            if (englishExpression.getTranslations() != null) {
                for (SpanishExpression sp : englishExpression.getTranslations()) {
                    if (sp != null && sp.getExpression() != null && !sp.getExpression().trim().isEmpty()) {
                        spanishSources.add(sp.getExpression().trim());
                    }
                }
            }
            expressionData.put("spanish_sources", spanishSources);
            if (englishExpression.getPracticeSourceDatabase() != null
                    && !englishExpression.getPracticeSourceDatabase().trim().isEmpty()) {
                expressionData.put("practice_source_database",
                        englishExpression.getPracticeSourceDatabase().trim());
            }

            List<Map<String, Object>> record = Arrays.asList(expressionData);
            gameDataService.getRepository().save(record);

            log.debug("English expression '{}' saved to repository", englishExpression.getExpression());
        } catch (Exception e) {
            log.error("Error saving English expression to repository: {}", e.getMessage());
        }
    }

    private AppGameMode resolveAppGameMode() {
        if (gameDataService instanceof GameDataServiceImpl impl) {
            return impl.getAppGameMode();
        }
        return AppGameMode.CLASSIC;
    }

    private void populatePromptExpressionMap(Map<String, Object> expressionData, String databaseName,
                                             SpanishExpression spanishExpression) {
        AppGameMode mode = resolveAppGameMode();
        expressionData.put("type", mode.getPromptExpressionType());
        expressionData.put("database", databaseName);
        expressionData.put("language", mode.getPromptLanguage());
        expressionData.put("expression", spanishExpression.getExpression());
        expressionData.put("score", spanishExpression.getScore());

        List<String> translations = new ArrayList<>();
        if (spanishExpression.getTranslations() != null) {
            for (EnglishExpression translation : spanishExpression.getTranslations()) {
                if (translation != null && translation.getExpression() != null) {
                    translations.add(translation.getExpression());
                }
            }
        }
        expressionData.put("translations", translations);
        expressionData.put("included_at", spanishExpression.getIncludedAtEpochMillis());
    }
}
