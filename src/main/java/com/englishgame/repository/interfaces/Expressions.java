package com.englishgame.repository.interfaces;

import java.util.Map;
import java.util.Set;

/**
 * Repository interface for managing Map<String, Object> data structure
 * This handles individual map operations within the data structure
 */
public interface Expressions {
    
    /**
     * Gets all key-value pairs from the map
     * @return Map<String, Object> with all entries
     */
    Map<String, Object> getAll();
    
    /**
     * Gets a value by key
     * @param key key to search for
     * @return Object value or null if key doesn't exist
     */
    Object get(String key);
    
    /**
     * Sets a key-value pair in the map
     * @param key key to set
     * @param value value to set
     * @return true if set successfully, false otherwise
     */
    boolean set(String key, Object value);
    
    /**
     * Updates a value for an existing key
     * @param key key to update
     * @param value new value
     * @return true if updated successfully, false if key doesn't exist
     */
    boolean update(String key, Object value);
    
    /**
     * Removes a key-value pair from the map
     * @param key key to remove
     * @return true if removed successfully, false if key doesn't exist
     */
    boolean remove(String key);
    
    /**
     * Checks if a key exists in the map
     * @param key key to check
     * @return true if key exists, false otherwise
     */
    boolean containsKey(String key);
    
    /**
     * Checks if a value exists in the map
     * @param value value to check
     * @return true if value exists, false otherwise
     */
    boolean containsValue(Object value);
    
    /**
     * Gets all keys from the map
     * @return Set<String> with all keys
     */
    Set<String> getKeys();
    
    /**
     * Gets the size of the map
     * @return number of key-value pairs
     */
    int size();
    
    /**
     * Checks if the map is empty
     * @return true if empty, false otherwise
     */
    boolean isEmpty();
    
    /**
     * Clears all entries from the map
     */
    void clear();
    
    /**
     * Gets a value by key with a default value if key doesn't exist
     * @param key key to search for
     * @param defaultValue default value to return if key doesn't exist
     * @return Object value or default value
     */
    Object getOrDefault(String key, Object defaultValue);
    
    /**
     * Merges another map into this map
     * @param otherMap map to merge
     * @return true if merged successfully, false otherwise
     */
    boolean merge(Map<String, Object> otherMap);
    
    /**
     * Gets a string representation of the map
     * @return String representation
     */
    String toString();
}
