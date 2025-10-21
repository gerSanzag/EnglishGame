package com.englishgame.repository.implementations;

import com.englishgame.repository.interfaces.DataBase;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of DataBase interface for managing List<Map<String, Object>>
 */
@Slf4j
public class DataBaseImpl implements DataBase {
    
    private final List<Map<String, Object>> dataList;
    
    public DataBaseImpl() {
        this.dataList = new ArrayList<>();
    }
    
    @Override
    public List<Map<String, Object>> findAll() {
        log.debug("Retrieving all maps from data list");
        return new ArrayList<>(dataList);
    }
    
    @Override
    public List<Map<String, Object>> findBy(String key, Object value) {
        log.debug("Searching maps with key '{}' and value '{}'", key, value);
        return dataList.stream()
                .filter(map -> map.containsKey(key) && Objects.equals(map.get(key), value))
                .collect(Collectors.toList());
    }
    
    @Override
    public Map<String, Object> get(int index) {
        log.debug("Getting map at index {}", index);
        if (index < 0 || index >= dataList.size()) {
            log.warn("Index {} out of range. Data list size: {}", index, dataList.size());
            return null;
        }
        return new HashMap<>(dataList.get(index));
    }
    
    @Override
    public boolean add(Map<String, Object> map) {
        if (map == null) {
            log.warn("Attempt to add null map");
            return false;
        }
        
        try {
            // Create a deep copy to avoid external modifications
            Map<String, Object> mapCopy = new HashMap<>(map);
            dataList.add(mapCopy);
            log.debug("Map added successfully. Total maps: {}", dataList.size());
            return true;
        } catch (Exception e) {
            log.error("Error adding map: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean update(int index, Map<String, Object> map) {
        if (map == null) {
            log.warn("Attempt to update with null map");
            return false;
        }
        
        if (index < 0 || index >= dataList.size()) {
            log.warn("Index {} out of range for update. Data list size: {}", index, dataList.size());
            return false;
        }
        
        try {
            // Create a deep copy to avoid external modifications
            Map<String, Object> mapCopy = new HashMap<>(map);
            dataList.set(index, mapCopy);
            log.debug("Map at index {} updated successfully", index);
            return true;
        } catch (Exception e) {
            log.error("Error updating map at index {}: {}", index, e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean remove(int index) {
        if (index < 0 || index >= dataList.size()) {
            log.warn("Index {} out of range for removal. Data list size: {}", index, dataList.size());
            return false;
        }
        
        try {
            dataList.remove(index);
            log.debug("Map at index {} removed successfully. Total maps: {}", index, dataList.size());
            return true;
        } catch (Exception e) {
            log.error("Error removing map at index {}: {}", index, e.getMessage());
            return false;
        }
    }
    
    @Override
    public int removeBy(String key, Object value) {
        log.debug("Removing maps with key '{}' and value '{}'", key, value);
        List<Map<String, Object>> toRemove = findBy(key, value);
        int removedCount = 0;
        
        for (Map<String, Object> map : toRemove) {
            if (dataList.remove(map)) {
                removedCount++;
            }
        }
        
        log.debug("Removed {} maps", removedCount);
        return removedCount;
    }
    
    @Override
    public int size() {
        int size = dataList.size();
        log.debug("Data list size: {}", size);
        return size;
    }
    
    @Override
    public boolean isEmpty() {
        boolean empty = dataList.isEmpty();
        log.debug("Data list empty: {}", empty);
        return empty;
    }
    
    @Override
    public void clear() {
        dataList.clear();
        log.debug("Data list cleared completely");
    }
    
    @Override
    public boolean exists(int index) {
        boolean exists = index >= 0 && index < dataList.size();
        log.debug("Map exists at index {}: {}", index, exists);
        return exists;
    }
    
    @Override
    public int indexOf(String key, Object value) {
        log.debug("Searching index of map with key '{}' and value '{}'", key, value);
        for (int i = 0; i < dataList.size(); i++) {
            Map<String, Object> map = dataList.get(i);
            if (map.containsKey(key) && Objects.equals(map.get(key), value)) {
                log.debug("Found map at index {}", i);
                return i;
            }
        }
        log.debug("Map not found");
        return -1;
    }
}
