package com.englishgame.repository.implementations;

import com.englishgame.repository.interfaces.DBRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for DBRepositoryImpl
 * Tests all CRUD operations for List<List<Map<String, Object>>> structure
 */
@DisplayName("DBRepository Tests")
class DBRepositoryImplTest {
    
    private DBRepository repository;
    
    @BeforeEach
    void setUp() {
        repository = new DBRepositoryImpl();
    }
    
    @Test
    @DisplayName("Should start with empty database")
    void shouldStartWithEmptyDatabase() {
        assertTrue(repository.isEmpty());
        assertEquals(0, repository.size());
    }
    
    @Test
    @DisplayName("Should save a record successfully")
    void shouldSaveRecordSuccessfully() {
        // Given
        List<Map<String, Object>> record = createSpanishExpressionRecord();
        
        // When
        boolean result = repository.save(record);
        
        // Then
        assertTrue(result);
        assertEquals(1, repository.size());
        assertFalse(repository.isEmpty());
    }
    
    @Test
    @DisplayName("Should not save null record")
    void shouldNotSaveNullRecord() {
        // When
        boolean result = repository.save(null);
        
        // Then
        assertFalse(result);
        assertTrue(repository.isEmpty());
    }
    
    @Test
    @DisplayName("Should not save empty record")
    void shouldNotSaveEmptyRecord() {
        // Given
        List<Map<String, Object>> emptyRecord = new ArrayList<>();
        
        // When
        boolean result = repository.save(emptyRecord);
        
        // Then
        assertFalse(result);
        assertTrue(repository.isEmpty());
    }
    
    @Test
    @DisplayName("Should find all records")
    void shouldFindAllRecords() {
        // Given
        List<Map<String, Object>> record1 = createSpanishExpressionRecord();
        List<Map<String, Object>> record2 = createEnglishExpressionRecord();
        repository.save(record1);
        repository.save(record2);
        
        // When
        List<List<Map<String, Object>>> allRecords = repository.findAll();
        
        // Then
        assertEquals(2, allRecords.size());
        assertNotSame(record1, allRecords.get(0)); // Should be deep copy
        assertNotSame(record2, allRecords.get(1)); // Should be deep copy
    }
    
    @Test
    @DisplayName("Should find records by criteria")
    void shouldFindRecordsByCriteria() {
        // Given
        List<Map<String, Object>> spanishRecord = createSpanishExpressionRecord();
        List<Map<String, Object>> englishRecord = createEnglishExpressionRecord();
        repository.save(spanishRecord);
        repository.save(englishRecord);
        
        // When
        List<List<Map<String, Object>>> spanishResults = repository.findBy("language", "spanish");
        List<List<Map<String, Object>>> englishResults = repository.findBy("language", "english");
        
        // Then
        assertEquals(1, spanishResults.size());
        assertEquals(1, englishResults.size());
    }
    
    @Test
    @DisplayName("Should find record by index")
    void shouldFindRecordById() {
        // Given
        List<Map<String, Object>> record1 = createSpanishExpressionRecord();
        List<Map<String, Object>> record2 = createEnglishExpressionRecord();
        repository.save(record1);
        repository.save(record2);
        
        // When
        List<Map<String, Object>> foundRecord = repository.findById(0);
        List<Map<String, Object>> foundRecord2 = repository.findById(1);
        List<Map<String, Object>> notFoundRecord = repository.findById(5);
        
        // Then
        assertNotNull(foundRecord);
        assertNotNull(foundRecord2);
        assertNull(notFoundRecord);
        assertEquals(2, foundRecord.size());
        assertEquals(2, foundRecord2.size());
    }
    
    @Test
    @DisplayName("Should update record successfully")
    void shouldUpdateRecordSuccessfully() {
        // Given
        List<Map<String, Object>> originalRecord = createSpanishExpressionRecord();
        repository.save(originalRecord);
        
        List<Map<String, Object>> updatedRecord = createEnglishExpressionRecord();
        
        // When
        boolean result = repository.update(0, updatedRecord);
        
        // Then
        assertTrue(result);
        List<Map<String, Object>> retrievedRecord = repository.findById(0);
        assertNotNull(retrievedRecord);
        assertEquals("english", retrievedRecord.get(0).get("language"));
    }
    
    @Test
    @DisplayName("Should not update with invalid index")
    void shouldNotUpdateWithInvalidIndex() {
        // Given
        List<Map<String, Object>> record = createSpanishExpressionRecord();
        repository.save(record);
        
        // When
        boolean result = repository.update(5, record);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should delete record successfully")
    void shouldDeleteRecordSuccessfully() {
        // Given
        List<Map<String, Object>> record1 = createSpanishExpressionRecord();
        List<Map<String, Object>> record2 = createEnglishExpressionRecord();
        repository.save(record1);
        repository.save(record2);
        assertEquals(2, repository.size());
        
        // When
        boolean result = repository.delete(0);
        
        // Then
        assertTrue(result);
        assertEquals(1, repository.size());
    }
    
    @Test
    @DisplayName("Should delete records by criteria")
    void shouldDeleteRecordsByCriteria() {
        // Given
        List<Map<String, Object>> spanishRecord1 = createSpanishExpressionRecord();
        List<Map<String, Object>> spanishRecord2 = createSpanishExpressionRecord();
        List<Map<String, Object>> englishRecord = createEnglishExpressionRecord();
        repository.save(spanishRecord1);
        repository.save(spanishRecord2);
        repository.save(englishRecord);
        assertEquals(3, repository.size());
        
        // When
        int deletedCount = repository.deleteBy("language", "spanish");
        
        // Then
        assertEquals(2, deletedCount);
        assertEquals(1, repository.size());
    }
    
    @Test
    @DisplayName("Should clear all records")
    void shouldClearAllRecords() {
        // Given
        List<Map<String, Object>> record1 = createSpanishExpressionRecord();
        List<Map<String, Object>> record2 = createEnglishExpressionRecord();
        repository.save(record1);
        repository.save(record2);
        assertEquals(2, repository.size());
        
        // When
        repository.clear();
        
        // Then
        assertTrue(repository.isEmpty());
        assertEquals(0, repository.size());
    }
    
    // Helper methods to create test data
    private List<Map<String, Object>> createSpanishExpressionRecord() {
        List<Map<String, Object>> record = new ArrayList<>();
        
        Map<String, Object> expression1 = new HashMap<>();
        expression1.put("language", "spanish");
        expression1.put("expression", "Hola");
        expression1.put("score", 10);
        expression1.put("translations", Arrays.asList("Hello", "Hi"));
        
        Map<String, Object> expression2 = new HashMap<>();
        expression2.put("language", "spanish");
        expression2.put("expression", "Adiós");
        expression2.put("score", 15);
        expression2.put("translations", Arrays.asList("Goodbye", "Bye"));
        
        record.add(expression1);
        record.add(expression2);
        
        return record;
    }
    
    private List<Map<String, Object>> createEnglishExpressionRecord() {
        List<Map<String, Object>> record = new ArrayList<>();
        
        Map<String, Object> expression1 = new HashMap<>();
        expression1.put("language", "english");
        expression1.put("expression", "Hello");
        expression1.put("score", 10);
        expression1.put("translations", Arrays.asList("Hola", "Saludos"));
        
        Map<String, Object> expression2 = new HashMap<>();
        expression2.put("language", "english");
        expression2.put("expression", "Goodbye");
        expression2.put("score", 15);
        expression2.put("translations", Arrays.asList("Adiós", "Hasta luego"));
        
        record.add(expression1);
        record.add(expression2);
        
        return record;
    }
}
