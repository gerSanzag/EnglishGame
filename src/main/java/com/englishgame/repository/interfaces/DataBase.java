package com.englishgame.repository.interfaces;

import java.util.List;
import java.util.Map;

/**
 * Repository interface for managing List<Map<String, Object>> data structure
 * This handles the inner list operations within the main database
 */
public interface DataBase {
    
    /**
     * Retrieves all maps from the list
     * @return List<Map<String, Object>> with all maps
     */
    List<Map<String, Object>> findAll();
    
    /**
     * Searches maps by specific key-value pair
     * @param key key to search for
     * @param value value to search for
     * @return List<Map<String, Object>> with matching maps
     */
    List<Map<String, Object>> findBy(String key, Object value);
    
    /**
     * Gets a map by index
     * @param index index of the map
     * @return Map<String, Object> or null if index is invalid
     */
    Map<String, Object> get(int index);
    
    /**
     * Adds a new map to the list
     * @param map Map to add
     * @return true if added successfully, false otherwise
     */
    boolean add(Map<String, Object> map);
    
    /**
     * Updates a map at specific index
     * @param index index of the map to update
     * @param map new map data
     * @return true if updated successfully, false otherwise
     */
    boolean update(int index, Map<String, Object> map);
    
    /**
     * Removes a map by index
     * @param index index of the map to remove
     * @return true if removed successfully, false otherwise
     */
    boolean remove(int index);
    
    /**
     * Removes maps that match specific criteria
     * @param key key to search for
     * @param value value to search for
     * @return number of maps removed
     */
    int removeBy(String key, Object value);
    
    /**
     * Gets the size of the list
     * @return number of maps
     */
    int size();
    
    /**
     * Checks if the list is empty
     * @return true if empty, false otherwise
     */
    boolean isEmpty();
    
    /**
     * Clears all maps from the list
     */
    void clear();
    
    /**
     * Checks if a map exists at the given index
     * @param index index to check
     * @return true if map exists, false otherwise
     */
    boolean exists(int index);
    
    /**
     * Gets the index of the first map that matches the criteria
     * @param key key to search for
     * @param value value to search for
     * @return index of the map or -1 if not found
     */
    int indexOf(String key, Object value);
}
