package com.englishgame.service.implementations;

import com.englishgame.model.SpanishExpression;
import com.englishgame.model.EnglishExpression;
import com.englishgame.service.interfaces.DatabaseService;
import com.englishgame.service.interfaces.GameDataService;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Optional;

/**
 * Implementation of DatabaseService for managing game databases
 * Handles creation, management, and operations on game databases
 */
@Slf4j
public class DatabaseServiceImpl implements DatabaseService {
    
    private final GameDataService gameDataService;
    private static final String LEARNED_WORDS_DATABASE = "learned_words";
    private static final String PHRASAL_VERBS_DATABASE = "Phrasal verbs";
    
    // In-memory storage for databases
    private final Map<String, Set<SpanishExpression>> spanishDatabases;
    private final Map<String, Set<EnglishExpression>> englishDatabases;
    
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
                        || PHRASAL_VERBS_DATABASE.equalsIgnoreCase(key))
                .orElse(false);
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
                                saveExpressionToRepository(dbKey, expr);
                                gameDataService.saveGameData();
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
    public SpanishExpression getRandomSpanishExpression(String databaseName) {
        return getRandomSpanishExpression(databaseName, null);
    }
    
    @Override
    public SpanishExpression getRandomSpanishExpression(String databaseName, SpanishExpression excludePreviousRound) {
        List<SpanishExpression> expressions = getSpanishExpressions(databaseName);
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
                .map(k -> englishDatabases.get(k).size())
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
        if (LEARNED_WORDS_DATABASE.equalsIgnoreCase(practiceKey.get())) {
            log.warn("Cannot promote from learned_words");
            return false;
        }
        
        String practiceDb = practiceKey.get();
        Set<SpanishExpression> practicePhrases = spanishDatabases.get(practiceDb);
        if (!practicePhrases.contains(hostPhrase)) {
            log.warn("Host phrase '{}' not found in practice database '{}'",
                    hostPhrase.getExpression(), practiceDb);
            return false;
        }
        
        boolean detached = hostPhrase.getTranslations().removeIf(en ->
                en.getExpression().equalsIgnoreCase(englishTranslation.getExpression()));
        if (!detached) {
            log.warn("English '{}' not found on host phrase '{}'",
                    englishTranslation.getExpression(), hostPhrase.getExpression());
            return false;
        }

        // Keep source Spanish phrase for Learned Words UI and JSON round-trip (spanish_sources)
        String hostExpr = Optional.ofNullable(hostPhrase.getExpression()).map(String::trim).orElse("");
        boolean hasSameSource = englishTranslation.getTranslations().stream()
                .anyMatch(sp -> sp != null && expressionsEqualNormalized(sp.getExpression(), hostExpr));
        if (!hostExpr.isEmpty() && !hasSameSource) {
            SpanishExpression link = new SpanishExpression();
            link.setExpression(hostExpr);
            link.setScore(0);
            englishTranslation.getTranslations().add(link);
        }
        
        Set<EnglishExpression> learned = englishDatabases.get(LEARNED_WORDS_DATABASE);
        learned.removeIf(en -> en.getExpression().equalsIgnoreCase(englishTranslation.getExpression()));
        englishTranslation.setIncludedAtEpochMillis(System.currentTimeMillis());
        learned.add(englishTranslation);
        
        if (hostPhrase.getTranslations().isEmpty()) {
            practicePhrases.remove(hostPhrase);
            log.debug("Removed emptied Spanish '{}' from '{}'", hostPhrase.getExpression(), practiceDb);
        }
        
        gameDataService.saveGameData();
        log.info("Learned '{}' added to '{}' and detached from '{}' (phrase '{}')",
                englishTranslation.getExpression(), LEARNED_WORDS_DATABASE,
                practiceDb, hostPhrase.getExpression());
        return true;
    }
    
    @Override
    public List<EnglishExpression> getLearnedExpressions() {
        return getEnglishExpressions(LEARNED_WORDS_DATABASE);
    }
    
    @Override
    public void synchronizeWithRepository() {
        log.info("Synchronizing database service with repository data...");
        loadDataFromRepository();
        log.info("Database synchronization completed. Available databases: {}", getAvailableDatabases());
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
        log.info("Initialized learned words database: {}", LEARNED_WORDS_DATABASE);
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
                        createDatabase(databaseName);
                        log.debug("Created database '{}' from loaded data", databaseName);
                    }
                } else {
                    // This is an expression record, find its database
                    String databaseName = (String) firstMap.get("database");
                    String language = (String) firstMap.get("language");
                    String expression = (String) firstMap.get("expression");
                    
                    if (databaseName != null && language != null && expression != null) {
                        // Ensure database exists
                        if (!databaseExists(databaseName)) {
                            createDatabase(databaseName);
                        }

                        Optional<String> dbKey = resolveCanonicalDatabaseKey(databaseName);
                        if (dbKey.isEmpty()) {
                            log.warn("Could not resolve database key for '{}' while loading JSON", databaseName);
                            continue;
                        }
                        
                        if ("spanish".equals(language)) {
                            // Create Spanish expression
                            SpanishExpression spanishExpr = new SpanishExpression();
                            spanishExpr.setExpression(expression);
                            spanishExpr.setScore(getIntValue(firstMap, "score", 0));
                            
                            // Add translations if they exist
                            Object translationsObj = firstMap.get("translations");
                            if (translationsObj instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<String> translations = (List<String>) translationsObj;
                                for (String translation : translations) {
                                    EnglishExpression englishExpr = new EnglishExpression();
                                    englishExpr.setExpression(translation);
                                    englishExpr.setScore(getIntValue(firstMap, "score", 0));
                                    spanishExpr.getTranslations().add(englishExpr);
                                }
                            }
                            
                            spanishExpr.setIncludedAtEpochMillis(getLongValue(firstMap, "included_at", 0L));
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
        return en;
    }
    
    /**
     * Saves database metadata to the repository for persistence
     */
    private void saveDatabaseMetadataToRepository(String databaseName) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("type", "database_metadata");
            metadata.put("database", databaseName);
            metadata.put("created_at", System.currentTimeMillis());
            
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
            expressionData.put("type", "spanish_expression");
            expressionData.put("database", databaseName);
            expressionData.put("language", "spanish");
            expressionData.put("expression", spanishExpression.getExpression());
            expressionData.put("score", spanishExpression.getScore());
            
            // Add translations
            List<String> translations = new ArrayList<>();
            for (EnglishExpression translation : spanishExpression.getTranslations()) {
                translations.add(translation.getExpression());
            }
            expressionData.put("translations", translations);
            expressionData.put("included_at", spanishExpression.getIncludedAtEpochMillis());

            List<Map<String, Object>> record = Arrays.asList(expressionData);
            gameDataService.getRepository().save(record);

            log.debug("Spanish expression '{}' saved to repository", spanishExpression.getExpression());
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
                
                List<Map<String, Object>> record = Arrays.asList(metadata);
                gameDataService.getRepository().save(record);
                
                // Add all Spanish expressions from this database
                List<SpanishExpression> spanishExpressions = getSpanishExpressions(dbName);
                for (SpanishExpression spanishExpr : spanishExpressions) {
                    Map<String, Object> expressionData = new HashMap<>();
                    expressionData.put("type", "spanish_expression");
                    expressionData.put("database", dbName);
                    expressionData.put("language", "spanish");
                    expressionData.put("expression", spanishExpr.getExpression());
                    expressionData.put("score", spanishExpr.getScore());
                    
                    // Add translations
                    List<String> translations = new ArrayList<>();
                    for (EnglishExpression translation : spanishExpr.getTranslations()) {
                        translations.add(translation.getExpression());
                    }
                    expressionData.put("translations", translations);
                    expressionData.put("included_at", spanishExpr.getIncludedAtEpochMillis());

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

            // Add translations
            List<String> translations = new ArrayList<>();
            for (SpanishExpression translation : englishExpression.getTranslations()) {
                translations.add(translation.getExpression());
            }
            expressionData.put("translations", translations);
            
            List<Map<String, Object>> record = Arrays.asList(expressionData);
            gameDataService.getRepository().save(record);
            
            log.debug("English expression '{}' saved to repository", englishExpression.getExpression());
        } catch (Exception e) {
            log.error("Error saving English expression to repository: {}", e.getMessage());
        }
    }
}
