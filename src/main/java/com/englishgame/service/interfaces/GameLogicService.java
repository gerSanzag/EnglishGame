package com.englishgame.service.interfaces;

import com.englishgame.model.SpanishExpression;
import com.englishgame.model.EnglishExpression;
import com.englishgame.model.CorrectAnswerOutcome;

/**
 * Service interface for managing game logic
 * Handles the core game flow and rules
 */
public interface GameLogicService {
    
    /**
     * Gets a random Spanish expression from the specified database
     * @param databaseName name of the database to select from
     * @return SpanishExpression or null if database is empty
     */
    SpanishExpression getRandomSpanishExpression(String databaseName);
    
    /**
     * Same as {@link #getRandomSpanishExpression(String)} but avoids repeating the previous round's phrase when possible.
     */
    SpanishExpression getRandomSpanishExpression(String databaseName, SpanishExpression excludePreviousRound);
    
    /**
     * Validates user's English translation against the Spanish expression
     * @param spanishExpression the Spanish expression being translated
     * @param userTranslation the user's English translation
     * @return true if translation is correct, false otherwise
     */
    boolean validateTranslation(SpanishExpression spanishExpression, String userTranslation);
    
    /**
     * Processes correct answer - adds points to matching English expression
     * @param spanishExpression the Spanish expression
     * @param userTranslation the correct English translation
     * @param practiceDatabase vocabulary where {@code spanishExpression} belongs (needed to retire learned translations)
     * @return outcome with the updated English expression and whether it was promoted to learned words; null if no matching translation
     */
    CorrectAnswerOutcome processCorrectAnswer(SpanishExpression spanishExpression, String userTranslation,
                                              String practiceDatabase);
    
    /**
     * Processes incorrect answer - subtracts points from all English expressions
     * @param spanishExpression the Spanish expression
     * @param userTranslation the incorrect English translation
     * @return list of updated English expressions with new scores
     */
    java.util.List<EnglishExpression> processIncorrectAnswer(SpanishExpression spanishExpression, String userTranslation);
    
    /**
     * Checks if an English expression has reached the learned threshold (15 points)
     * @param englishExpression the English expression to check
     * @return true if learned (score >= 15), false otherwise
     */
    boolean isExpressionLearned(EnglishExpression englishExpression);
    
    /**
     * Moves a learned expression to the learned words database
     * @param englishExpression the learned English expression
     * @return true if moved successfully, false otherwise
     */
    boolean moveToLearnedWords(EnglishExpression englishExpression);
    
    /**
     * Minimum score required on each English translation to count as learned (aligned with persisted rules).
     * Used so the phrase score progress bar matches game logic.
     */
    int getLearnedScoreThreshold();
    
    /**
     * Gets the current score of a Spanish expression
     * @param spanishExpression the Spanish expression
     * @return current score
     */
    int getSpanishExpressionScore(SpanishExpression spanishExpression);
    
    /**
     * Gets the current score of an English expression
     * @param englishExpression the English expression
     * @return current score
     */
    int getEnglishExpressionScore(EnglishExpression englishExpression);
    
    /**
     * Gets all available database names
     * @return list of database names
     */
    java.util.List<String> getAvailableDatabases();
    
    /**
     * Creates a new database with the given name
     * @param databaseName name of the new database
     * @return true if created successfully, false if already exists
     */
    boolean createDatabase(String databaseName);
    
    /**
     * Gets all Spanish expressions from a specific database
     * @param databaseName name of the database
     * @return list of Spanish expressions
     */
    java.util.List<SpanishExpression> getSpanishExpressionsFromDatabase(String databaseName);
    
    /**
     * Gets all English expressions from a specific database
     * @param databaseName name of the database
     * @return list of English expressions
     */
    java.util.List<EnglishExpression> getEnglishExpressionsFromDatabase(String databaseName);
}
