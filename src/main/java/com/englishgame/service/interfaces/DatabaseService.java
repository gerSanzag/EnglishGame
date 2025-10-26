package com.englishgame.service.interfaces;

import com.englishgame.model.SpanishExpression;
import com.englishgame.model.EnglishExpression;

/**
 * Service interface for managing game databases
 * Handles creation, management, and operations on game databases
 */
public interface DatabaseService {
    
    /**
     * Creates a new database with the given name
     * @param databaseName name of the new database
     * @return true if created successfully, false if already exists
     */
    boolean createDatabase(String databaseName);
    
    /**
     * Deletes a database by name
     * @param databaseName name of the database to delete
     * @return true if deleted successfully, false if not found
     */
    boolean deleteDatabase(String databaseName);
    
    /**
     * Gets all available database names
     * @return list of database names
     */
    java.util.List<String> getAvailableDatabases();
    
    /**
     * Checks if a database exists
     * @param databaseName name of the database to check
     * @return true if exists, false otherwise
     */
    boolean databaseExists(String databaseName);
    
    /**
     * Gets all Spanish expressions from a specific database
     * @param databaseName name of the database
     * @return list of Spanish expressions
     */
    java.util.List<SpanishExpression> getSpanishExpressions(String databaseName);
    
    /**
     * Gets all English expressions from a specific database
     * @param databaseName name of the database
     * @return list of English expressions
     */
    java.util.List<EnglishExpression> getEnglishExpressions(String databaseName);
    
    /**
     * Adds a Spanish expression to a database
     * @param databaseName name of the database
     * @param spanishExpression the Spanish expression to add
     * @return true if added successfully, false otherwise
     */
    boolean addSpanishExpression(String databaseName, SpanishExpression spanishExpression);
    
    /**
     * Adds an English expression to a database
     * @param databaseName name of the database
     * @param englishExpression the English expression to add
     * @return true if added successfully, false otherwise
     */
    boolean addEnglishExpression(String databaseName, EnglishExpression englishExpression);
    
    /**
     * Removes a Spanish expression from a database
     * @param databaseName name of the database
     * @param expression the Spanish expression to remove
     * @return true if removed successfully, false otherwise
     */
    boolean removeSpanishExpression(String databaseName, String expression);
    
    /**
     * Removes an English expression from a database
     * @param databaseName name of the database
     * @param expression the English expression to remove
     * @return true if removed successfully, false otherwise
     */
    boolean removeEnglishExpression(String databaseName, String expression);
    
    /**
     * Gets a random Spanish expression from a database
     * @param databaseName name of the database
     * @return random Spanish expression or null if database is empty
     */
    SpanishExpression getRandomSpanishExpression(String databaseName);
    
    /**
     * Gets a random English expression from a database
     * @param databaseName name of the database
     * @return random English expression or null if database is empty
     */
    EnglishExpression getRandomEnglishExpression(String databaseName);
    
    /**
     * Gets the count of Spanish expressions in a database
     * @param databaseName name of the database
     * @return number of Spanish expressions
     */
    int getSpanishExpressionCount(String databaseName);
    
    /**
     * Gets the count of English expressions in a database
     * @param databaseName name of the database
     * @return number of English expressions
     */
    int getEnglishExpressionCount(String databaseName);
    
    /**
     * Gets the total count of expressions in a database
     * @param databaseName name of the database
     * @return total number of expressions
     */
    int getTotalExpressionCount(String databaseName);
    
    /**
     * Searches for Spanish expressions containing the given text
     * @param databaseName name of the database
     * @param searchText text to search for
     * @return list of matching Spanish expressions
     */
    java.util.List<SpanishExpression> searchSpanishExpressions(String databaseName, String searchText);
    
    /**
     * Searches for English expressions containing the given text
     * @param databaseName name of the database
     * @param searchText text to search for
     * @return list of matching English expressions
     */
    java.util.List<EnglishExpression> searchEnglishExpressions(String databaseName, String searchText);
    
    /**
     * Gets the learned words database name
     * @return name of the learned words database
     */
    String getLearnedWordsDatabaseName();
    
    /**
     * Moves a learned expression to the learned words database
     * @param englishExpression the learned English expression
     * @return true if moved successfully, false otherwise
     */
    boolean moveToLearnedWords(EnglishExpression englishExpression);
    
    /**
     * Gets all learned expressions
     * @return list of learned English expressions
     */
    java.util.List<EnglishExpression> getLearnedExpressions();
    
    /**
     * Synchronizes database service with repository data
     * Should be called after loading data from JSON
     */
    void synchronizeWithRepository();
}
