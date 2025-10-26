package com.englishgame.service.interfaces;

import java.util.List;
import java.util.Map;

/**
 * Service interface for managing game data persistence
 * Handles saving and loading game data to/from disk
 */
public interface GameDataService {
    
    /**
     * Saves all game data to disk
     * @return true if saved successfully, false otherwise
     */
    boolean saveGameData();
    
    /**
     * Loads all game data from disk
     * @return true if loaded successfully, false otherwise
     */
    boolean loadGameData();
    
    /**
     * Saves game data to a specific file
     * @param filename name of the file to save to
     * @return true if saved successfully, false otherwise
     */
    boolean saveGameDataToFile(String filename);
    
    /**
     * Loads game data from a specific file
     * @param filename name of the file to load from
     * @return true if loaded successfully, false otherwise
     */
    boolean loadGameDataFromFile(String filename);
    
    /**
     * Creates a backup of current game data
     * @return true if backup created successfully, false otherwise
     */
    boolean createBackup();
    
    /**
     * Restores game data from backup
     * @return true if restored successfully, false otherwise
     */
    boolean restoreFromBackup();
    
    /**
     * Gets the current data directory path
     * @return String path to data directory
     */
    String getDataDirectory();
    
    /**
     * Sets the data directory path
     * @param directory path to data directory
     */
    void setDataDirectory(String directory);
    
    /**
     * Checks if game data exists on disk
     * @return true if data exists, false otherwise
     */
    boolean gameDataExists();
    
    /**
     * Gets the size of saved game data
     * @return long size in bytes, -1 if error
     */
    long getGameDataSize();
    
    /**
     * Exports game data to JSON string
     * @return String JSON representation of game data
     */
    String exportToJSON();
    
    /**
     * Imports game data from JSON string
     * @param jsonData JSON string to import
     * @return true if imported successfully, false otherwise
     */
    boolean importFromJSON(String jsonData);
    
    /**
     * Gets all data from the repository
     * @return List<List<Map<String, Object>>> with all repository data
     */
    List<List<Map<String, Object>>> getAllData();
    
    /**
     * Gets access to the repository for direct operations
     * @return DBRepository instance
     */
    com.englishgame.repository.interfaces.DBRepository getRepository();
}
