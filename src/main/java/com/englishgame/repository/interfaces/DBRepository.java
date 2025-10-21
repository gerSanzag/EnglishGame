package com.englishgame.repository.interfaces;

import java.util.List;
import java.util.Map;

/**
 * Repository interface for managing data with CRUD operations
 * Uses List<List<Map<String, Object>>> as data structure
 */
public interface DBRepository {
    
    /**
     * Retrieves all data from the database
     * @return List<List<Map>> with all records
     */
    List<List<Map<String, Object>>> findAll();
    
    /**
     * Searches records by specific criteria
     * @param key key to search for
     * @param value value to search for
     * @return List<List<Map>> with found records
     */
    List<List<Map<String, Object>>> findBy(String key, Object value);
    
    /**
     * Searches a specific record by index
     * @param index record index
     * @return List<Map> with the found record or null if not exists
     */
    List<Map<String, Object>> findById(int index);
    
    /**
     * Saves a new record
     * @param record List<Map> with the record to save
     * @return true if saved successfully, false otherwise
     */
    boolean save(List<Map<String, Object>> record);
    
    /**
     * Updates an existing record
     * @param index index of the record to update
     * @param record List<Map> with new data
     * @return true if updated successfully, false otherwise
     */
    boolean update(int index, List<Map<String, Object>> record);
    
    /**
     * Deletes a record by index
     * @param index index of the record to delete
     * @return true if deleted successfully, false otherwise
     */
    boolean delete(int index);
    
    /**
     * Deletes records that match specific criteria
     * @param key key to search for
     * @param value value to search for
     * @return number of deleted records
     */
    int deleteBy(String key, Object value);
    
    /**
     * Gets the database size
     * @return number of records
     */
    int size();
    
    /**
     * Checks if the database is empty
     * @return true if empty, false otherwise
     */
    boolean isEmpty();
    
    /**
     * Clears the entire database
     */
    void clear();
}
