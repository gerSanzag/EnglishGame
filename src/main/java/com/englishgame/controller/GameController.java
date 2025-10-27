package com.englishgame.controller;

import com.englishgame.model.EnglishExpression;
import com.englishgame.model.SpanishExpression;
import com.englishgame.service.interfaces.DatabaseService;
import com.englishgame.service.interfaces.GameDataService;
import com.englishgame.service.interfaces.GameLogicService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for orchestrating the game flow
 * Handles user interactions and coordinates between services
 */
@Slf4j
public class GameController {

    private final GameLogicService gameLogicService;
    private final DatabaseService databaseService;
    private final GameDataService gameDataService;

    private String currentDatabase;
    private SpanishExpression currentSpanishExpression;

    public GameController(GameLogicService gameLogicService, DatabaseService databaseService, GameDataService gameDataService) {
        this.gameLogicService = gameLogicService;
        this.databaseService = databaseService;
        this.gameDataService = gameDataService;
        
        // Set database service reference in game data service
        if (gameDataService instanceof com.englishgame.service.implementations.GameDataServiceImpl) {
            ((com.englishgame.service.implementations.GameDataServiceImpl) gameDataService).setDatabaseService(databaseService);
        }
        
        initializeGame();
    }

    private void initializeGame() {
        log.info("Initializing game controller...");
        gameDataService.loadGameData(); // Load previous game state
        
        // Synchronize loaded data with database service
        databaseService.synchronizeWithRepository();
        
        log.info("Game initialized. Loaded {} databases.", databaseService.getAvailableDatabases().size());
    }

    public List<String> getAvailableDatabases() {
        return databaseService.getAvailableDatabases().stream()
                .filter(dbName -> !"learned_words".equals(dbName))
                .collect(Collectors.toList());
    }

    public boolean selectDatabase(String databaseName) {
        return Optional.ofNullable(databaseName)
                .filter(databaseService::databaseExists)
                .map(name -> {
                    this.currentDatabase = name;
                    log.info("Database selected: {}", name);
                    return true;
                })
                .orElseGet(() -> {
                    log.warn("Attempted to select non-existent database: {}", databaseName);
                    return false;
                });
    }

    public SpanishExpression startNewRound() {
        return Optional.ofNullable(currentDatabase)
                .map(databaseName -> {
                    log.debug("Starting new round with database: {}", databaseName);
                    return gameLogicService.getRandomSpanishExpression(databaseName);
                })
                .map(expression -> {
                    this.currentSpanishExpression = expression;
                    if (expression != null) {
                        log.info("New round started. Spanish expression: '{}'", expression.getExpression());
                    } else {
                        log.warn("Could not select a Spanish expression from database: {}", currentDatabase);
                    }
                    return expression;
                })
                .orElseGet(() -> {
                    log.error("No database selected. Cannot start a new round.");
                    return null;
                });
    }

    public boolean processAnswer(String userTranslation) {
        return Optional.ofNullable(currentSpanishExpression)
                .map(expr -> {
                    log.debug("Processing answer '{}' for Spanish expression '{}'", userTranslation, expr.getExpression());
                    
                    return gameLogicService.validateTranslation(expr, userTranslation)
                            ? processCorrectAnswer(expr, userTranslation)
                            : processIncorrectAnswer(expr, userTranslation);
                })
                .orElseGet(() -> {
                    log.error("No current Spanish expression to process answer for.");
                    return false;
                });
    }

    private boolean processCorrectAnswer(SpanishExpression spanishExpression, String userTranslation) {
        return Optional.ofNullable(gameLogicService.processCorrectAnswer(spanishExpression, userTranslation))
                .map(updatedEnglishExpression -> {
                    // Update the Spanish expression's translations list with the updated English expression
                    spanishExpression.getTranslations().replaceAll(e -> 
                        e.getExpression().equals(updatedEnglishExpression.getExpression()) ? updatedEnglishExpression : e);
                    
                    log.info("Correct answer! English expression '{}' score updated to {}.", 
                            updatedEnglishExpression.getExpression(), updatedEnglishExpression.getScore());

                    if (gameLogicService.isExpressionLearned(updatedEnglishExpression)) {
                        gameLogicService.moveToLearnedWords(updatedEnglishExpression);
                        log.info("English expression '{}' moved to learned words!", updatedEnglishExpression.getExpression());
                    }
                    
                    return true;
                })
                .orElse(false);
    }

    private boolean processIncorrectAnswer(SpanishExpression spanishExpression, String userTranslation) {
        List<EnglishExpression> updatedTranslations = gameLogicService.processIncorrectAnswer(spanishExpression, userTranslation);
        spanishExpression.setTranslations(updatedTranslations); // Update the Spanish expression with penalized translations
        log.info("Incorrect answer. All English translations for '{}' penalized.", spanishExpression.getExpression());
        return false;
    }

    public SpanishExpression getCurrentSpanishExpression() {
        return currentSpanishExpression;
    }

    public String getCurrentDatabase() {
        return currentDatabase;
    }

    public boolean createNewDatabase(String databaseName) {
        return Optional.ofNullable(databaseName)
                .filter(name -> !name.trim().isEmpty())
                .map(name -> {
                    boolean created = databaseService.createDatabase(name);
                    if (created) {
                        gameDataService.saveGameData(); // Save changes after creating a new database
                        log.info("New database '{}' created successfully", name);
                    }
                    return created;
                })
                .orElseGet(() -> {
                    log.warn("Cannot create database with null or empty name");
                    return false;
                });
    }

    public boolean addExpressionToDatabase(String databaseName, SpanishExpression spanishExpression) {
        return Optional.ofNullable(databaseName)
                .filter(databaseService::databaseExists)
                .map(name -> {
                    boolean added = databaseService.addSpanishExpression(name, spanishExpression);
                    if (added) {
                        gameDataService.saveGameData(); // Save changes after adding expression
                        log.info("Spanish expression '{}' added to database '{}'", 
                                spanishExpression.getExpression(), name);
                    }
                    return added;
                })
                .orElseGet(() -> {
                    log.warn("Database '{}' does not exist", databaseName);
                    return false;
                });
    }

    public boolean deleteDatabase(String databaseName) {
        return Optional.ofNullable(databaseName)
                .filter(name -> !name.trim().isEmpty())
                .filter(databaseService::databaseExists)
                .map(name -> {
                    boolean deleted = databaseService.deleteDatabase(name);
                    if (deleted) {
                        gameDataService.saveGameData(); // Save changes after deleting database
                        
                        // If the deleted database was the current one, clear current database
                        if (name.equals(currentDatabase)) {
                            currentDatabase = null;
                            currentSpanishExpression = null;
                            log.info("Current database cleared after deletion of '{}'", name);
                        }
                        
                        log.info("Database '{}' deleted successfully", name);
                    }
                    return deleted;
                })
                .orElseGet(() -> {
                    log.warn("Cannot delete database '{}' - it may not exist or be protected", databaseName);
                    return false;
                });
    }

    public void saveGameState() {
        gameDataService.saveGameData();
        log.debug("Game state saved successfully");
    }

    public void loadGameState() {
        gameDataService.loadGameData();
        log.debug("Game state loaded successfully");
    }

    /**
     * Gets all Spanish expressions from a specific database
     * @param databaseName name of the database
     * @return list of Spanish expressions
     */
    public List<SpanishExpression> getSpanishExpressionsFromDatabase(String databaseName) {
        return databaseService.getSpanishExpressions(databaseName);
    }

    /**
     * Gets all English expressions from a specific database
     * @param databaseName name of the database
     * @return list of English expressions
     */
    public List<EnglishExpression> getEnglishExpressionsFromDatabase(String databaseName) {
        return databaseService.getEnglishExpressions(databaseName);
    }
    
    /**
     * Moves an expression from one database to another
     * @param sourceDatabase source database name
     * @param targetDatabase target database name
     * @param expression the expression to move
     * @return true if moved successfully, false otherwise
     */
    public boolean moveExpression(String sourceDatabase, String targetDatabase, String expression) {
        return Optional.ofNullable(sourceDatabase)
                .filter(databaseService::databaseExists)
                .flatMap(sourceDb -> Optional.ofNullable(targetDatabase)
                        .filter(databaseService::databaseExists)
                        .filter(targetDb -> !sourceDb.equals(targetDb))
                        .map(targetDb -> {
                            // Try to move as Spanish expression first
                            boolean moved = databaseService.moveSpanishExpression(sourceDb, targetDb, expression);
                            
                            if (!moved) {
                                // If not found as Spanish, try as English expression
                                moved = databaseService.moveEnglishExpression(sourceDb, targetDb, expression);
                            }
                            
                            if (moved) {
                                // Save changes after successful move
                                gameDataService.saveGameData();
                                log.info("Expression '{}' moved from '{}' to '{}' successfully", expression, sourceDb, targetDb);
                            } else {
                                log.warn("Expression '{}' not found in source database '{}'", expression, sourceDb);
                            }
                            
                            return moved;
                        }))
                .orElseGet(() -> {
                    log.warn("Cannot move expression '{}' from '{}' to '{}' - invalid databases", 
                            expression, sourceDatabase, targetDatabase);
                    return false;
                });
    }
}