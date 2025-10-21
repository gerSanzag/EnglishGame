package com.englishgame.service.implementations;

import com.englishgame.model.SpanishExpression;
import com.englishgame.model.EnglishExpression;
import com.englishgame.service.interfaces.DatabaseService;
import com.englishgame.service.interfaces.GameDataService;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of DatabaseService for managing game databases
 * Handles creation, management, and operations on game databases
 */
@Slf4j
public class DatabaseServiceImpl implements DatabaseService {
    
    private final GameDataService gameDataService;
    private static final String LEARNED_WORDS_DATABASE = "learned_words";
    private static final String DEFAULT_DATABASE = "default";
    
    // In-memory storage for databases
    private final Map<String, List<SpanishExpression>> spanishDatabases;
    private final Map<String, List<EnglishExpression>> englishDatabases;
    
    public DatabaseServiceImpl(GameDataService gameDataService) {
        this.gameDataService = gameDataService;
        this.spanishDatabases = new HashMap<>();
        this.englishDatabases = new HashMap<>();
        initializeDefaultDatabases();
    }
    
    @Override
    public boolean createDatabase(String databaseName) {
        if (databaseName == null || databaseName.trim().isEmpty()) {
            log.warn("Cannot create database with null or empty name");
            return false;
        }
        
        if (databaseExists(databaseName)) {
            log.warn("Database '{}' already exists", databaseName);
            return false;
        }
        
        spanishDatabases.put(databaseName, new ArrayList<>());
        englishDatabases.put(databaseName, new ArrayList<>());
        
        log.info("Database '{}' created successfully", databaseName);
        return true;
    }
    
    @Override
    public boolean deleteDatabase(String databaseName) {
        if (databaseName == null || databaseName.trim().isEmpty()) {
            log.warn("Cannot delete database with null or empty name");
            return false;
        }
        
        if (!databaseExists(databaseName)) {
            log.warn("Database '{}' does not exist", databaseName);
            return false;
        }
        
        if (LEARNED_WORDS_DATABASE.equals(databaseName)) {
            log.warn("Cannot delete the learned words database");
            return false;
        }
        
        spanishDatabases.remove(databaseName);
        englishDatabases.remove(databaseName);
        
        log.info("Database '{}' deleted successfully", databaseName);
        return true;
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
        if (!databaseExists(databaseName)) {
            log.warn("Database '{}' does not exist", databaseName);
            return false;
        }
        
        if (spanishExpression == null) {
            log.warn("Cannot add null Spanish expression");
            return false;
        }
        
        spanishDatabases.get(databaseName).add(spanishExpression);
        log.debug("Added Spanish expression '{}' to database '{}'", 
                spanishExpression.getExpression(), databaseName);
        
        return true;
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
    
    private void initializeDefaultDatabases() {
        // Create default database
        createDatabase(DEFAULT_DATABASE);
        
        // Create learned words database
        createDatabase(LEARNED_WORDS_DATABASE);
        
        log.info("Initialized default databases: {} and {}", DEFAULT_DATABASE, LEARNED_WORDS_DATABASE);
    }
}
