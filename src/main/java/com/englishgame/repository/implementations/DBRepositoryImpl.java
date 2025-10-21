package com.englishgame.repository.implementations;

import com.englishgame.repository.interfaces.DBRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class DBRepositoryImpl implements DBRepository {
    
    private final List<List<Map<String, Object>>> database;
    
    public DBRepositoryImpl() {
        this.database = new ArrayList<>();
    }
    
    @Override
    public List<List<Map<String, Object>>> findAll() {
        log.debug("Retrieving all records from database");
        return new ArrayList<>(database);
    }
    
    @Override
    public List<List<Map<String, Object>>> findBy(String key, Object value) {
        log.debug("Searching records with key '{}' and value '{}'", key, value);
        
        return database.stream()
                .filter(record -> record.stream()
                        .anyMatch(map -> map.containsKey(key) && 
                                Objects.equals(map.get(key), value)))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Map<String, Object>> findById(int index) {
        log.debug("Searching record at index {}", index);
        
        if (index < 0 || index >= database.size()) {
            log.warn("Index {} out of range. Database size: {}", index, database.size());
            return null;
        }
        
        return new ArrayList<>(database.get(index));
    }
    
    @Override
    public boolean save(List<Map<String, Object>> record) {
        if (record == null || record.isEmpty()) {
            log.warn("Attempt to save null or empty record");
            return false;
        }
        
        try {
            // Create a deep copy of the record
            List<Map<String, Object>> recordCopy = record.stream()
                    .map(HashMap::new)
                    .collect(Collectors.toList());
            
            database.add(recordCopy);
            log.debug("Record saved successfully. Total records: {}", database.size());
            return true;
        } catch (Exception e) {
            log.error("Error saving record: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean update(int index, List<Map<String, Object>> record) {
        if (record == null || record.isEmpty()) {
            log.warn("Attempt to update with null or empty record");
            return false;
        }
        
        if (index < 0 || index >= database.size()) {
            log.warn("Index {} out of range for update. Database size: {}", 
                    index, database.size());
            return false;
        }
        
        try {
            // Create a deep copy of the record
            List<Map<String, Object>> recordCopy = record.stream()
                    .map(HashMap::new)
                    .collect(Collectors.toList());
            
            database.set(index, recordCopy);
            log.debug("Record at index {} updated successfully", index);
            return true;
        } catch (Exception e) {
            log.error("Error updating record at index {}: {}", index, e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean delete(int index) {
        if (index < 0 || index >= database.size()) {
            log.warn("Index {} out of range for deletion. Database size: {}", 
                    index, database.size());
            return false;
        }
        
        try {
            database.remove(index);
            log.debug("Record at index {} deleted successfully. Total records: {}", 
                    index, database.size());
            return true;
        } catch (Exception e) {
            log.error("Error deleting record at index {}: {}", index, e.getMessage());
            return false;
        }
    }
    
    @Override
    public int deleteBy(String key, Object value) {
        log.debug("Deleting records with key '{}' and value '{}'", key, value);
        
        List<List<Map<String, Object>>> toDelete = findBy(key, value);
        int deletedCount = 0;
        
        for (List<Map<String, Object>> record : toDelete) {
            if (database.remove(record)) {
                deletedCount++;
            }
        }
        
        log.debug("Deleted {} records", deletedCount);
        return deletedCount;
    }
    
    @Override
    public int size() {
        int size = database.size();
        log.debug("Database size: {}", size);
        return size;
    }
    
    @Override
    public boolean isEmpty() {
        boolean empty = database.isEmpty();
        log.debug("Database empty: {}", empty);
        return empty;
    }
    
    @Override
    public void clear() {
        database.clear();
        log.debug("Database cleared completely");
    }
}
