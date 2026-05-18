package com.englishgame.controller;

import com.englishgame.model.AnswerResult;
import com.englishgame.model.CorrectAnswerOutcome;
import com.englishgame.model.EnglishExpression;
import com.englishgame.model.LearnedWordsReviewResult;
import com.englishgame.model.ReviewDatabases;
import com.englishgame.model.SpanishExpression;
import com.englishgame.service.interfaces.DatabaseService;
import com.englishgame.service.interfaces.GameDataService;
import com.englishgame.service.interfaces.GameLogicService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
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
        registerShutdownSaveHook();
    }

    private void registerShutdownSaveHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (gameDataService.saveGameData()) {
                    log.info("Game data saved on application shutdown");
                }
            } catch (Exception e) {
                log.error("Failed to save game data on shutdown: {}", e.getMessage());
            }
        }, "english-game-shutdown-save"));
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
                .filter(dbName -> !databaseService.isReviewOnlyDatabase(dbName))
                .collect(Collectors.toList());
    }

    /** Claves canónicas de las bases solo usadas en Review. */
    public List<String> getReviewDatabaseKeys() {
        return ReviewDatabases.allKeys();
    }

    public List<String> getReviewDatabaseDisplayNames() {
        return ReviewDatabases.allDisplayNames();
    }

    public boolean isReviewOnlyDatabase(String databaseName) {
        return databaseService.isReviewOnlyDatabase(databaseName);
    }

    public boolean hasAnyReviewContent() {
        return getReviewDatabaseKeys().stream()
                .anyMatch(key -> !getEnglishExpressionsFromDatabase(key).isEmpty());
    }

    public int getWordsDefinitelyMasteredTotal() {
        return databaseService.getWordsDefinitelyMasteredTotal();
    }

    public int getWordsDefinitelyCurrentCount() {
        return databaseService.getEnglishExpressionCount(ReviewDatabases.WORDS_DEFINITELY_LEARNED_KEY);
    }

    public int getLearnedWordsCurrentCount() {
        return databaseService.getEnglishExpressionCount(ReviewDatabases.LEARNED_WORDS_KEY);
    }

    /**
     * Destinos al mover desde Learned words: vocabulario de práctica + Words definitely learned (solo review).
     */
    public List<String> getMoveTargetsFromLearnedWordsDatabase() {
        List<String> targets = new ArrayList<>(getAvailableDatabases());
        String definitely = ReviewDatabases.WORDS_DEFINITELY_LEARNED_KEY;
        if (targets.stream().noneMatch(d -> definitely.equalsIgnoreCase(d))) {
            targets.add(definitely);
        }
        targets.sort(String.CASE_INSENSITIVE_ORDER);
        return targets;
    }

    public boolean selectDatabase(String databaseName) {
        return Optional.ofNullable(databaseName)
                .filter(databaseService::databaseExists)
                .map(name -> {
                    this.currentDatabase = databaseService.getCanonicalDatabaseName(name).orElse(name);
                    log.info("Database selected: {}", this.currentDatabase);
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
                    return gameLogicService.getRandomSpanishExpression(databaseName, currentSpanishExpression);
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

    public AnswerResult processAnswer(String userTranslation) {
        if (currentSpanishExpression == null) {
            log.error("No current Spanish expression to process answer for.");
            return AnswerResult.incorrect();
        }

        SpanishExpression expr = currentSpanishExpression;
        log.debug("Processing answer '{}' for Spanish expression '{}'", userTranslation, expr.getExpression());

        AnswerResult result;
        if (gameLogicService.validateTranslation(expr, userTranslation, currentDatabase)) {
            result = processCorrectAnswerOutcome(expr, userTranslation);
        } else {
            processIncorrectAnswer(expr, userTranslation);
            result = AnswerResult.incorrect();
        }

        saveGameState();
        return result;
    }

    private AnswerResult processCorrectAnswerOutcome(SpanishExpression spanishExpression, String userTranslation) {
        return Optional.ofNullable(gameLogicService.processCorrectAnswer(spanishExpression, userTranslation,
                        currentDatabase))
                .map((CorrectAnswerOutcome outcome) -> {
                    EnglishExpression updatedEnglishExpression = outcome.englishExpression();
                    /*
                     * Sin promoción: sincronizar referencias sobre la tarjeta actual (penalizaciones/coherencia UI).
                     * Si acaba de promocionarse a learned words, DatabaseService ya desasoció el inglés de la BD;
                     * hacer replaceAll aquí podría volver a colgar el mismo objeto en la lista española (= huérfano con "None" al guardar).
                     */
                    if (!outcome.promotedToLearnedWords()) {
                        spanishExpression.getTranslations().replaceAll(e ->
                                e.getExpression().equals(updatedEnglishExpression.getExpression())
                                        ? updatedEnglishExpression : e);
                    }

                    log.info("Correct answer! English expression '{}' score updated to {}.",
                            updatedEnglishExpression.getExpression(), updatedEnglishExpression.getScore());

                    String learnedWord = outcome.promotedToLearnedWords()
                            ? updatedEnglishExpression.getExpression()
                            : null;
                    return new AnswerResult(true, learnedWord);
                })
                .orElse(AnswerResult.incorrect());
    }

    private void processIncorrectAnswer(SpanishExpression spanishExpression, String userTranslation) {
        List<EnglishExpression> updatedTranslations = gameLogicService.processIncorrectAnswer(
                spanishExpression, userTranslation, currentDatabase);
        spanishExpression.setTranslations(updatedTranslations); // Update the Spanish expression with penalized translations
        log.info("Incorrect answer. All English translations for '{}' penalized.", spanishExpression.getExpression());
    }

    public SpanishExpression getCurrentSpanishExpression() {
        return currentSpanishExpression;
    }

    public String getCurrentDatabase() {
        return currentDatabase;
    }

    public int getLearnedScoreThreshold() {
        return gameLogicService.getLearnedScoreThreshold();
    }

    /**
     * English answer(s) to display in practice mode reveal (translations joined with {@code  | }).
     * Incluye todas las traducciones válidas cuando hay varios registros con el mismo español.
     */
    public Optional<String> getRevealAnswersLine() {
        List<EnglishExpression> translations = getCurrentPhraseCohortEnglishTranslations();
        if (translations.isEmpty() && currentSpanishExpression != null
                && currentSpanishExpression.getTranslations() != null
                && !currentSpanishExpression.getTranslations().isEmpty()) {
            translations = currentSpanishExpression.getTranslations();
        }
        if (translations == null || translations.isEmpty()) {
            return Optional.empty();
        }
        String line = translations.stream()
                .map(EnglishExpression::getExpression)
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(" | "));
        return line.isEmpty() ? Optional.empty() : Optional.of(line);
    }

    /**
     * Todas las filas de vocabulario con el mismo texto en español que la tarjeta actual.
     */
    public List<SpanishExpression> getCurrentPhraseCohort() {
        if (currentSpanishExpression == null) {
            return List.of();
        }
        List<SpanishExpression> c =
                gameLogicService.getSpanishPhraseCohort(currentDatabase, currentSpanishExpression);
        if (c == null || c.isEmpty()) {
            return List.of(currentSpanishExpression);
        }
        return c;
    }

    /**
     * Concatenación de las traducciones inglesas de toda la cohorte (varios registros, mismo español).
     */
    public List<EnglishExpression> getCurrentPhraseCohortEnglishTranslations() {
        List<EnglishExpression> out = new ArrayList<>();
        for (SpanishExpression row : getCurrentPhraseCohort()) {
            if (row.getTranslations() != null) {
                out.addAll(row.getTranslations());
            }
        }
        return out;
    }

    /**
     * Validates the translation without modifying scores or persisting.
     */
    public Optional<Boolean> checkAnswerWithoutScoring(String userTranslation) {
        if (currentSpanishExpression == null) {
            log.debug("Practice check skipped: no current expression");
            return Optional.empty();
        }
        if (userTranslation == null || userTranslation.trim().isEmpty()) {
            return Optional.of(false);
        }
        boolean ok = gameLogicService.validateTranslation(currentSpanishExpression, userTranslation,
                currentDatabase);
        log.debug("Practice-only check for '{}': {}", userTranslation, ok);
        return Optional.of(ok);
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

    /**
     * Renames a user vocabulary database (not {@code learned_words}). Updates {@link #currentDatabase} if it was the one renamed.
     *
     * @return true if renamed and persisted
     */
    public boolean renameDatabase(String oldDatabaseName, String newDatabaseName) {
        Optional<String> oldCanonOpt = Optional.ofNullable(oldDatabaseName)
                .flatMap(databaseService::getCanonicalDatabaseName);
        Optional<String> newKeyOpt = databaseService.renameDatabase(oldDatabaseName, newDatabaseName);
        if (newKeyOpt.isEmpty()) {
            log.warn("Rename failed from '{}' to '{}'", oldDatabaseName, newDatabaseName);
            return false;
        }
        String newKey = newKeyOpt.get();
        Optional<String> currCanonOpt = Optional.ofNullable(currentDatabase)
                .flatMap(databaseService::getCanonicalDatabaseName);
        if (oldCanonOpt.isPresent() && currCanonOpt.isPresent()
                && oldCanonOpt.get().equals(currCanonOpt.get())) {
            currentDatabase = newKey;
            log.info("Updated currentDatabase after rename -> '{}'", newKey);
        }
        return true;
    }

    public boolean isSystemDatabase(String databaseName) {
        return databaseService.isSystemDatabase(databaseName);
    }

    public void saveGameState() {
        gameDataService.saveGameData();
        log.debug("Game state saved successfully");
    }

    public void loadGameState() {
        gameDataService.loadGameData();
        databaseService.synchronizeWithRepository();
        log.debug("Game state loaded and synchronized successfully");
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
     * Review de learned_words (+1 / −5, reingreso bajo 21, dominio en 28).
     */
    public Optional<LearnedWordsReviewResult> submitLearnedWordsReviewAnswer(EnglishExpression learnedCard,
            String userAnswer, String reviewDatabaseName) {
        return databaseService.submitLearnedWordsReviewAttempt(learnedCard, userAnswer, reviewDatabaseName);
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
    
    /**
     * Deletes an expression from a database
     * @param databaseName name of the database
     * @param expression the expression to delete
     * @return true if deleted successfully, false otherwise
     */
    public boolean deleteExpression(String databaseName, String expression) {
        return Optional.ofNullable(databaseName)
                .filter(databaseService::databaseExists)
                .map(dbName -> {
                    // Try to delete as Spanish expression first
                    boolean deleted = databaseService.removeSpanishExpression(dbName, expression);
                    
                    if (!deleted) {
                        // If not found as Spanish, try as English expression
                        deleted = databaseService.removeEnglishExpression(dbName, expression);
                    }
                    
                    if (deleted) {
                        // Save changes after successful deletion
                        gameDataService.saveGameData();
                        log.info("Expression '{}' deleted from database '{}' successfully", expression, dbName);
                    } else {
                        log.warn("Expression '{}' not found in database '{}'", expression, dbName);
                    }
                    
                    return deleted;
                })
                .orElseGet(() -> {
                    log.warn("Cannot delete expression '{}' from '{}' - database does not exist", 
                            expression, databaseName);
                    return false;
                });
    }
    
    /**
     * Deletes all expressions from a database
     * @param databaseName name of the database
     * @return true if deleted successfully, false otherwise
     */
    public boolean deleteAllExpressions(String databaseName) {
        log.info("DeleteAllExpressions called for database: {}", databaseName);
        
        return Optional.ofNullable(databaseName)
                .filter(databaseService::databaseExists)
                .map(dbName -> {
                    log.info("Database '{}' exists, proceeding with deletion", dbName);
                    
                    // Delete all Spanish expressions
                    boolean spanishDeleted = databaseService.deleteAllSpanishExpressions(dbName);
                    log.info("Spanish expressions deletion result: {}", spanishDeleted);
                    
                    // Delete all English expressions
                    boolean englishDeleted = databaseService.deleteAllEnglishExpressions(dbName);
                    log.info("English expressions deletion result: {}", englishDeleted);
                    
                    boolean anyDeleted = spanishDeleted || englishDeleted;
                    
                    if (anyDeleted) {
                        // Save changes after successful deletion
                        gameDataService.saveGameData();
                        log.info("All expressions deleted from database '{}' successfully", dbName);
                    } else {
                        log.warn("No expressions found in database '{}' to delete", dbName);
                    }
                    
                    return anyDeleted;
                })
                .orElseGet(() -> {
                    log.warn("Cannot delete all expressions from '{}' - database does not exist", databaseName);
                    return false;
                });
    }
}