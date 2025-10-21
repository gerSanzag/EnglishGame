package com.englishgame.repository.implementations;

import com.englishgame.repository.interfaces.Expressions;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of Expressions interface for managing Map<String, Object>
 */
@Slf4j
public class ExpressionsImpl implements Expressions {
    
    private final Map<String, Object> expressionMap;
    
    public ExpressionsImpl() {
        this.expressionMap = new HashMap<>();
    }
    
    public ExpressionsImpl(Map<String, Object> initialMap) {
        this.expressionMap = new HashMap<>(initialMap);
    }
    
    @Override
    public Map<String, Object> getAll() {
        log.debug("Retrieving all key-value pairs from expression map");
        return new HashMap<>(expressionMap);
    }
    
    @Override
    public Object get(String key) {
        log.debug("Getting value for key '{}'", key);
        if (key == null) {
            log.warn("Attempt to get value with null key");
            return null;
        }
        return expressionMap.get(key);
    }
    
    @Override
    public boolean set(String key, Object value) {
        if (key == null) {
            log.warn("Attempt to set value with null key");
            return false;
        }
        
        try {
            expressionMap.put(key, value);
            log.debug("Set key '{}' with value '{}' successfully", key, value);
            return true;
        } catch (Exception e) {
            log.error("Error setting key '{}' with value '{}': {}", key, value, e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean update(String key, Object value) {
        if (key == null) {
            log.warn("Attempt to update value with null key");
            return false;
        }
        
        if (!expressionMap.containsKey(key)) {
            log.warn("Attempt to update non-existent key '{}'", key);
            return false;
        }
        
        try {
            expressionMap.put(key, value);
            log.debug("Updated key '{}' with value '{}' successfully", key, value);
            return true;
        } catch (Exception e) {
            log.error("Error updating key '{}' with value '{}': {}", key, value, e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean remove(String key) {
        if (key == null) {
            log.warn("Attempt to remove value with null key");
            return false;
        }
        
        if (!expressionMap.containsKey(key)) {
            log.warn("Attempt to remove non-existent key '{}'", key);
            return false;
        }
        
        try {
            Object removedValue = expressionMap.remove(key);
            log.debug("Removed key '{}' with value '{}' successfully", key, removedValue);
            return true;
        } catch (Exception e) {
            log.error("Error removing key '{}': {}", key, e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean containsKey(String key) {
        if (key == null) {
            log.debug("Checking for null key - always false");
            return false;
        }
        boolean contains = expressionMap.containsKey(key);
        log.debug("Map contains key '{}': {}", key, contains);
        return contains;
    }
    
    @Override
    public boolean containsValue(Object value) {
        boolean contains = expressionMap.containsValue(value);
        log.debug("Map contains value '{}': {}", value, contains);
        return contains;
    }
    
    @Override
    public Set<String> getKeys() {
        log.debug("Retrieving all keys from expression map");
        return expressionMap.keySet();
    }
    
    @Override
    public int size() {
        int size = expressionMap.size();
        log.debug("Expression map size: {}", size);
        return size;
    }
    
    @Override
    public boolean isEmpty() {
        boolean empty = expressionMap.isEmpty();
        log.debug("Expression map empty: {}", empty);
        return empty;
    }
    
    @Override
    public void clear() {
        expressionMap.clear();
        log.debug("Expression map cleared completely");
    }
    
    @Override
    public Object getOrDefault(String key, Object defaultValue) {
        log.debug("Getting value for key '{}' with default '{}'", key, defaultValue);
        if (key == null) {
            log.warn("Attempt to get value with null key, returning default");
            return defaultValue;
        }
        return expressionMap.getOrDefault(key, defaultValue);
    }
    
    @Override
    public boolean merge(Map<String, Object> otherMap) {
        if (otherMap == null) {
            log.warn("Attempt to merge null map");
            return false;
        }
        
        try {
            expressionMap.putAll(otherMap);
            log.debug("Merged {} entries from other map", otherMap.size());
            return true;
        } catch (Exception e) {
            log.error("Error merging map: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public String toString() {
        String result = expressionMap.toString();
        log.debug("Expression map toString: {}", result);
        return result;
    }
}
