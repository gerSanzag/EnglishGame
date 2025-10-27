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
    
    // In-memory storage for databases
    private final Map<String, Set<SpanishExpression>> spanishDatabases;
    private final Map<String, Set<EnglishExpression>> englishDatabases;
    
    public DatabaseServiceImpl(GameDataService gameDataService) {
        this.gameDataService = gameDataService;
        this.spanishDatabases = new HashMap<>();
        this.englishDatabases = new HashMap<>();
        initializeDefaultDatabases();
    }
    
    @Override
    public boolean createDatabase(String databaseName) {
        return Optional.ofNullable(databaseName)
                .filter(name -> !name.trim().isEmpty())
                .filter(name -> !databaseExists(name))
                .map(name -> {
                       // Create in-memory databases
                       spanishDatabases.put(name, new HashSet<>());
                       englishDatabases.put(name, new HashSet<>());
                    
                    // Save database metadata to repository for persistence
                    saveDatabaseMetadataToRepository(name);
                    
                    log.info("Database '{}' created successfully", name);
                    return true;
                })
                .orElseGet(() -> {
                    if (databaseName == null || databaseName.trim().isEmpty()) {
                        log.warn("Cannot create database with null or empty name");
                    } else {
                        log.warn("Database '{}' already exists", databaseName);
                    }
                    return false;
                });
    }
    
    @Override
    public boolean deleteDatabase(String databaseName) {
        return Optional.ofNullable(databaseName)
                .filter(name -> !name.trim().isEmpty())
                .filter(name -> databaseExists(name))
                .filter(name -> !LEARNED_WORDS_DATABASE.equals(name))
                .map(name -> {
                    // Remove from in-memory databases
                    spanishDatabases.remove(name);
                    englishDatabases.remove(name);
                    
                    // Remove from repository for persistence
                    removeDatabaseFromRepository(name);
                    
                    log.info("Database '{}' deleted successfully", name);
                    return true;
                })
                .orElseGet(() -> {
                    if (databaseName == null || databaseName.trim().isEmpty()) {
                        log.warn("Cannot delete database with null or empty name");
                    } else if (!databaseExists(databaseName)) {
                        log.warn("Database '{}' does not exist", databaseName);
                    } else if (LEARNED_WORDS_DATABASE.equals(databaseName)) {
                        log.warn("Cannot delete the learned words database");
                    }
                    return false;
                });
    }
    
    @Override
    public List<String> getAvailableDatabases() {
        return new ArrayList<>(spanishDatabases.keySet());
    }
    
    @Override
    public boolean databaseExists(String databaseName) {
        return spanishDatabases.containsKey(databaseName);
    }
    
    @Override
    public List<SpanishExpression> getSpanishExpressions(String databaseName) {
        if (!databaseExists(databaseName)) {
            log.warn("Database '{}' does not exist", databaseName);
            return new ArrayList<>();
        }
        
        return new ArrayList<>(spanishDatabases.get(databaseName));
    }
    
    @Override
    public List<EnglishExpression> getEnglishExpressions(String databaseName) {
        if (!databaseExists(databaseName)) {
            log.warn("Database '{}' does not exist", databaseName);
            return new ArrayList<>();
        }
        
        return new ArrayList<>(englishDatabases.get(databaseName));
    }
    
    @Override
    public boolean addSpanishExpression(String databaseName, SpanishExpression spanishExpression) {
        return Optional.ofNullable(databaseName)
                .filter(this::databaseExists)
                .flatMap(db -> Optional.ofNullable(spanishExpression)
                        .filter(expr -> expr.getExpression() != null && !expr.getExpression().trim().isEmpty())
                        .map(expr -> {
                            // HashSet.add() automatically returns false if duplicate
                            boolean added = spanishDatabases.get(db).add(expr);
                            
                            if (added) {
                                // Save expression to repository for persistence
                                saveExpressionToRepository(db, expr);
                                log.debug("Added Spanish expression '{}' to database '{}'", 
                                        expr.getExpression(), db);
                            } else {
                                log.warn("Spanish expression '{}' already exists in database '{}'", 
                                        expr.getExpression(), db);
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
        if (!databaseExists(databaseName)) {
            log.warn("Database '{}' does not exist", databaseName);
            return false;
        }
        
        if (englishExpression == null) {
            log.warn("Cannot add null English expression");
            return false;
        }
        
        englishDatabases.get(databaseName).add(englishExpression);
        log.debug("Added English expression '{}' to database '{}'", 
                englishExpression.getExpression(), databaseName);
        
        return true;
    }
    
    @Override
    public boolean removeSpanishExpression(String databaseName, String expression) {
        if (!databaseExists(databaseName)) {
            log.warn("Database '{}' does not exist", databaseName);
            return false;
        }
        
        boolean removed = spanishDatabases.get(databaseName)
                .removeIf(spanishExpr -> spanishExpr.getExpression().equals(expression));
        
        if (removed) {
            log.debug("Removed Spanish expression '{}' from database '{}'", expression, databaseName);
        } else {
            log.warn("Spanish expression '{}' not found in database '{}'", expression, databaseName);
        }
        
        return removed;
    }
    
    @Override
    public boolean removeEnglishExpression(String databaseName, String expression) {
        if (!databaseExists(databaseName)) {
            log.warn("Database '{}' does not exist", databaseName);
            return false;
        }
        
        boolean removed = englishDatabases.get(databaseName)
                .removeIf(englishExpr -> englishExpr.getExpression().equals(expression));
        
        if (removed) {
            log.debug("Removed English expression '{}' from database '{}'", expression, databaseName);
        } else {
            log.warn("English expression '{}' not found in database '{}'", expression, databaseName);
        }
        
        return removed;
    }
    
    @Override
    public SpanishExpression getRandomSpanishExpression(String databaseName) {
        List<SpanishExpression> expressions = getSpanishExpressions(databaseName);
        if (expressions.isEmpty()) {
            log.warn("Database '{}' is empty", databaseName);
            return null;
        }
        
        Random random = new Random();
        SpanishExpression selected = expressions.get(random.nextInt(expressions.size()));
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
        if (!databaseExists(databaseName)) {
            return 0;
        }
        return spanishDatabases.get(databaseName).size();
    }
    
    @Override
    public int getEnglishExpressionCount(String databaseName) {
        if (!databaseExists(databaseName)) {
            return 0;
        }
        return englishDatabases.get(databaseName).size();
    }
    
    @Override
    public int getTotalExpressionCount(String databaseName) {
        return getSpanishExpressionCount(databaseName) + getEnglishExpressionCount(databaseName);
    }
    
    @Override
    public List<SpanishExpression> searchSpanishExpressions(String databaseName, String searchText) {
        if (!databaseExists(databaseName)) {
            return new ArrayList<>();
        }
        
        return spanishDatabases.get(databaseName).stream()
                .filter(spanishExpr -> spanishExpr.getExpression().toLowerCase()
                        .contains(searchText.toLowerCase()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<EnglishExpression> searchEnglishExpressions(String databaseName, String searchText) {
        if (!databaseExists(databaseName)) {
            return new ArrayList<>();
        }
        
        return englishDatabases.get(databaseName).stream()
                .filter(englishExpr -> englishExpr.getExpression().toLowerCase()
                        .contains(searchText.toLowerCase()))
                .collect(Collectors.toList());
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
        
        boolean added = addEnglishExpression(LEARNED_WORDS_DATABASE, englishExpression);
        if (added) {
            log.info("Moved English expression '{}' to learned words database", 
                    englishExpression.getExpression());
        }
        
        return added;
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
        // Create learned words database (required for system functionality)
        createDatabase(LEARNED_WORDS_DATABASE);
        
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
                            
                            addSpanishExpression(databaseName, spanishExpr);
                            log.debug("Loaded Spanish expression '{}' into database '{}'", expression, databaseName);
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
    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
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
                    
                    List<Map<String, Object>> exprRecord = Arrays.asList(expressionData);
                    gameDataService.getRepository().save(exprRecord);
                }
            }
            
            log.debug("Database '{}' and all its expressions removed from repository", databaseName);
        } catch (Exception e) {
            log.error("Error removing database from repository: {}", e.getMessage());
        }
    }
    
    @Override
    public boolean moveSpanishExpression(String sourceDatabase, String targetDatabase, String expression) {
        return Optional.ofNullable(sourceDatabase)
                .filter(this::databaseExists)
                .flatMap(sourceDb -> Optional.ofNullable(targetDatabase)
                        .filter(this::databaseExists)
                        .filter(targetDb -> !sourceDb.equals(targetDb))
                        .map(targetDb -> {
                            // Find the Spanish expression in source database
                            Optional<SpanishExpression> spanishExpr = spanishDatabases.get(sourceDb)
                                    .stream()
                                    .filter(expr -> expr.getExpression().equals(expression))
                                    .findFirst();
                            
                            if (spanishExpr.isPresent()) {
                                // Remove from source database
                                spanishDatabases.get(sourceDb).remove(spanishExpr.get());
                                
                                // Add to target database
                                spanishDatabases.get(targetDb).add(spanishExpr.get());
                                
                                // Update repository
                                updateRepositoryAfterMove(sourceDb, targetDb, spanishExpr.get(), "spanish");
                                
                                log.info("Spanish expression '{}' moved from '{}' to '{}'", expression, sourceDb, targetDb);
                                return true;
                            } else {
                                log.warn("Spanish expression '{}' not found in source database '{}'", expression, sourceDb);
                                return false;
                            }
                        }))
                .orElseGet(() -> {
                    log.warn("Cannot move Spanish expression '{}' from '{}' to '{}' - invalid databases", 
                            expression, sourceDatabase, targetDatabase);
                    return false;
                });
    }
    
    @Override
    public boolean moveEnglishExpression(String sourceDatabase, String targetDatabase, String expression) {
        return Optional.ofNullable(sourceDatabase)
                .filter(this::databaseExists)
                .flatMap(sourceDb -> Optional.ofNullable(targetDatabase)
                        .filter(this::databaseExists)
                        .filter(targetDb -> !sourceDb.equals(targetDb))
                        .map(targetDb -> {
                            // Find the English expression in source database
                            Optional<EnglishExpression> englishExpr = englishDatabases.get(sourceDb)
                                    .stream()
                                    .filter(expr -> expr.getExpression().equals(expression))
                                    .findFirst();
                            
                            if (englishExpr.isPresent()) {
                                // Remove from source database
                                englishDatabases.get(sourceDb).remove(englishExpr.get());
                                
                                // Add to target database
                                englishDatabases.get(targetDb).add(englishExpr.get());
                                
                                // Update repository
                                updateRepositoryAfterMove(sourceDb, targetDb, englishExpr.get(), "english");
                                
                                log.info("English expression '{}' moved from '{}' to '{}'", expression, sourceDb, targetDb);
                                return true;
                            } else {
                                log.warn("English expression '{}' not found in source database '{}'", expression, sourceDb);
                                return false;
                            }
                        }))
                .orElseGet(() -> {
                    log.warn("Cannot move English expression '{}' from '{}' to '{}' - invalid databases", 
                            expression, sourceDatabase, targetDatabase);
                    return false;
                });
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
