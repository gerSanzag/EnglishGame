package com.englishgame.controller;

import com.englishgame.model.SpanishExpression;
import com.englishgame.model.EnglishExpression;
import com.englishgame.service.interfaces.GameLogicService;
import com.englishgame.service.interfaces.ScoreService;
import com.englishgame.service.interfaces.DatabaseService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Controller for managing the English learning game
 * Orchestrates the game flow and coordinates between services
 */
@Slf4j
public class GameController {
    
    private final GameLogicService gameLogicService;
    private final ScoreService scoreService;
    private final DatabaseService databaseService;
    
    private String currentDatabase;
    private SpanishExpression currentSpanishExpression;
    private boolean gameInProgress;
    
    public GameController(GameLogicService gameLogicService, 
                         ScoreService scoreService, 
                         DatabaseService databaseService) {
        this.gameLogicService = gameLogicService;
        this.scoreService = scoreService;
        this.databaseService = databaseService;
        this.gameInProgress = false;
    }
    
    /**
     * Starts a new game session with the specified database
     * @param databaseName name of the database to use
     * @return true if game started successfully, false otherwise
     */
    public boolean startGame(String databaseName) {
        log.info("Starting new game with database: {}", databaseName);
        
        if (!databaseService.databaseExists(databaseName)) {
            log.error("Database '{}' does not exist", databaseName);
            return false;
        }
        
        if (databaseService.getSpanishExpressionCount(databaseName) == 0) {
            log.error("Database '{}' is empty", databaseName);
            return false;
        }
        
        this.currentDatabase = databaseName;
        this.gameInProgress = true;
        
        log.info("Game started successfully with database: {}", databaseName);
        return true;
    }
    
    /**
     * Gets a random Spanish expression for the current game
     * @return Spanish expression or null if no game in progress
     */
    public SpanishExpression getCurrentQuestion() {
        if (!gameInProgress) {
            log.warn("No game in progress");
            return null;
        }
        
        currentSpanishExpression = gameLogicService.getRandomSpanishExpression(currentDatabase);
        if (currentSpanishExpression == null) {
            log.error("Failed to get Spanish expression from database: {}", currentDatabase);
            return null;
        }
        
        log.debug("Current question: '{}'", currentSpanishExpression.getExpression());
        return currentSpanishExpression;
    }
    
    /**
     * Submits an answer and processes the result
     * @param userAnswer the user's English translation
     * @return GameResult containing the result of the answer
     */
    public GameResult submitAnswer(String userAnswer) {
        if (!gameInProgress || currentSpanishExpression == null) {
            log.warn("No game in progress or no current question");
            return new GameResult(false, "No game in progress", null, null);
        }
        
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            log.warn("Empty answer submitted");
            return new GameResult(false, "Answer cannot be empty", null, null);
        }
        
        log.debug("Processing answer: '{}' for question: '{}'", 
                userAnswer, currentSpanishExpression.getExpression());
        
        boolean isCorrect = gameLogicService.validateTranslation(currentSpanishExpression, userAnswer);
        
        if (isCorrect) {
            return processCorrectAnswer(userAnswer);
        } else {
            return processIncorrectAnswer(userAnswer);
        }
    }
    
    /**
     * Processes a correct answer
     * @param userAnswer the correct answer
     * @return GameResult with success information
     */
    private GameResult processCorrectAnswer(String userAnswer) {
        log.info("Correct answer: '{}' for '{}'", 
                userAnswer, currentSpanishExpression.getExpression());
        
        EnglishExpression updatedExpression = gameLogicService.processCorrectAnswer(
                currentSpanishExpression, userAnswer);
        
        boolean isLearned = scoreService.isLearned(updatedExpression);
        String message = isLearned ? 
                String.format("Correct! '%s' has been learned!", updatedExpression.getExpression()) :
                String.format("Correct! Score: %d/%d", 
                        updatedExpression.getScore(), scoreService.getLearnedThreshold());
        
        return new GameResult(true, message, updatedExpression, null);
    }
    
    /**
     * Processes an incorrect answer
     * @param userAnswer the incorrect answer
     * @return GameResult with penalty information
     */
    private GameResult processIncorrectAnswer(String userAnswer) {
        log.info("Incorrect answer: '{}' for '{}'", 
                userAnswer, currentSpanishExpression.getExpression());
        
        List<EnglishExpression> penalizedExpressions = gameLogicService.processIncorrectAnswer(
                currentSpanishExpression, userAnswer);
        
        String message = String.format("Incorrect! Penalty applied to all translations. Score: %d", 
                penalizedExpressions.get(0).getScore());
        
        return new GameResult(false, message, null, penalizedExpressions);
    }
    
    /**
     * Ends the current game
     */
    public void endGame() {
        log.info("Ending current game");
        this.gameInProgress = false;
        this.currentSpanishExpression = null;
        this.currentDatabase = null;
    }
    
    /**
     * Gets the current game status
     * @return GameStatus with current game information
     */
    public GameStatus getGameStatus() {
        return new GameStatus(
                gameInProgress,
                currentDatabase,
                currentSpanishExpression,
                databaseService.getAvailableDatabases()
        );
    }
    
    /**
     * Creates a new database
     * @param databaseName name of the new database
     * @return true if created successfully, false otherwise
     */
    public boolean createDatabase(String databaseName) {
        log.info("Creating new database: {}", databaseName);
        return databaseService.createDatabase(databaseName);
    }
    
    /**
     * Gets all available databases
     * @return list of database names
     */
    public List<String> getAvailableDatabases() {
        return databaseService.getAvailableDatabases();
    }
    
    /**
     * Gets the learned expressions
     * @return list of learned English expressions
     */
    public List<EnglishExpression> getLearnedExpressions() {
        return databaseService.getLearnedExpressions();
    }
    
    /**
     * Gets the current database name
     * @return current database name or null if no game in progress
     */
    public String getCurrentDatabase() {
        return currentDatabase;
    }
    
    /**
     * Checks if a game is currently in progress
     * @return true if game in progress, false otherwise
     */
    public boolean isGameInProgress() {
        return gameInProgress;
    }
    
    /**
     * Result of a game answer submission
     */
    public static class GameResult {
        private final boolean correct;
        private final String message;
        private final EnglishExpression updatedExpression;
        private final List<EnglishExpression> penalizedExpressions;
        
        public GameResult(boolean correct, String message, 
                         EnglishExpression updatedExpression, 
                         List<EnglishExpression> penalizedExpressions) {
            this.correct = correct;
            this.message = message;
            this.updatedExpression = updatedExpression;
            this.penalizedExpressions = penalizedExpressions;
        }
        
        public boolean isCorrect() { return correct; }
        public String getMessage() { return message; }
        public EnglishExpression getUpdatedExpression() { return updatedExpression; }
        public List<EnglishExpression> getPenalizedExpressions() { return penalizedExpressions; }
    }
    
    /**
     * Current game status information
     */
    public static class GameStatus {
        private final boolean gameInProgress;
        private final String currentDatabase;
        private final SpanishExpression currentQuestion;
        private final List<String> availableDatabases;
        
        public GameStatus(boolean gameInProgress, String currentDatabase, 
                         SpanishExpression currentQuestion, 
                         List<String> availableDatabases) {
            this.gameInProgress = gameInProgress;
            this.currentDatabase = currentDatabase;
            this.currentQuestion = currentQuestion;
            this.availableDatabases = availableDatabases;
        }
        
        public boolean isGameInProgress() { return gameInProgress; }
        public String getCurrentDatabase() { return currentDatabase; }
        public SpanishExpression getCurrentQuestion() { return currentQuestion; }
        public List<String> getAvailableDatabases() { return availableDatabases; }
    }
}
