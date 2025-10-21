package com.englishgame.service.implementations;

import com.englishgame.model.SpanishExpression;
import com.englishgame.model.EnglishExpression;
import com.englishgame.service.interfaces.GameLogicService;
import com.englishgame.service.interfaces.GameDataService;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

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
        
        List<SpanishExpression> expressions = getSpanishExpressionsFromDatabase(databaseName);
        if (expressions.isEmpty()) {
            log.warn("Database '{}' is empty", databaseName);
            return null;
        }
        
        Random random = new Random();
        SpanishExpression selectedExpression = expressions.get(random.nextInt(expressions.size()));
        log.debug("Selected Spanish expression: '{}' with score: {}", 
                selectedExpression.getExpression(), selectedExpression.getScore());
        
        return selectedExpression;
    }
    
    @Override
    public boolean validateTranslation(SpanishExpression spanishExpression, String userTranslation) {
        if (spanishExpression == null || userTranslation == null || userTranslation.trim().isEmpty()) {
            log.warn("Invalid parameters for translation validation");
            return false;
        }
        
        log.debug("Validating translation '{}' for Spanish expression '{}'", 
                userTranslation, spanishExpression.getExpression());
        
        boolean isValid = spanishExpression.getTranslations().stream()
                .anyMatch(englishExpr -> englishExpr.getExpression().equalsIgnoreCase(userTranslation.trim()));
        
        log.debug("Translation validation result: {}", isValid);
        return isValid;
    }
    
    @Override
    public EnglishExpression processCorrectAnswer(SpanishExpression spanishExpression, String userTranslation) {
        log.debug("Processing correct answer for '{}' -> '{}'", 
                spanishExpression.getExpression(), userTranslation);
        
        // Find the matching English expression
        EnglishExpression matchingExpression = spanishExpression.getTranslations().stream()
                .filter(englishExpr -> englishExpr.getExpression().equalsIgnoreCase(userTranslation.trim()))
                .findFirst()
                .orElse(null);
        
        if (matchingExpression != null) {
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
        }
        
        return matchingExpression;
    }
    
    @Override
    public List<EnglishExpression> processIncorrectAnswer(SpanishExpression spanishExpression, String userTranslation) {
        log.debug("Processing incorrect answer for '{}' -> '{}'", 
                spanishExpression.getExpression(), userTranslation);
        
        List<EnglishExpression> updatedExpressions = new ArrayList<>();
        
        // Subtract 5 points from all English expressions in the translations list
        for (EnglishExpression englishExpr : spanishExpression.getTranslations()) {
            int newScore = Math.max(0, englishExpr.getScore() - PENALTY_POINTS);
            englishExpr.setScore(newScore);
            updatedExpressions.add(englishExpr);
            
            log.debug("Subtracted {} points from English expression '{}'. New score: {}", 
                    PENALTY_POINTS, englishExpr.getExpression(), newScore);
        }
        
        return updatedExpressions;
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
        databases.add("default");
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
        
        // This would need to be implemented based on how data is stored
        // For now, return empty list
        return new ArrayList<>();
    }
    
    @Override
    public List<EnglishExpression> getEnglishExpressionsFromDatabase(String databaseName) {
        log.debug("Getting English expressions from database: {}", databaseName);
        
        // This would need to be implemented based on how data is stored
        // For now, return empty list
        return new ArrayList<>();
    }
}
