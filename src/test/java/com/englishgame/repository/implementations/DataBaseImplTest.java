package com.englishgame.repository.implementations;

import com.englishgame.repository.interfaces.DataBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for DataBaseImpl
 * Tests all CRUD operations for List<Map<String, Object>> structure
 */
@DisplayName("DataBase Tests")
class DataBaseImplTest {
    
    private DataBase dataBase;
    
    @BeforeEach
    void setUp() {
        dataBase = new DataBaseImpl();
    }
    
    @Test
    @DisplayName("Should start with empty data list")
    void shouldStartWithEmptyDataList() {
        assertTrue(dataBase.isEmpty());
        assertEquals(0, dataBase.size());
    }
    
    @Test
    @DisplayName("Should add map successfully")
    void shouldAddMapSuccessfully() {
        // Given
        Map<String, Object> map = createSpanishExpressionMap();
        
        // When
        boolean result = dataBase.add(map);
        
        // Then
        assertTrue(result);
        assertEquals(1, dataBase.size());
        assertFalse(dataBase.isEmpty());
    }
    
    @Test
    @DisplayName("Should not add null map")
    void shouldNotAddNullMap() {
        // When
        boolean result = dataBase.add(null);
        
        // Then
        assertFalse(result);
        assertTrue(dataBase.isEmpty());
    }
    
    @Test
    @DisplayName("Should find all maps")
    void shouldFindAllMaps() {
        // Given
        Map<String, Object> map1 = createSpanishExpressionMap();
        Map<String, Object> map2 = createEnglishExpressionMap();
        dataBase.add(map1);
        dataBase.add(map2);
        
        // When
        List<Map<String, Object>> allMaps = dataBase.findAll();
        
        // Then
        assertEquals(2, allMaps.size());
        assertNotSame(map1, allMaps.get(0)); // Should be deep copy
        assertNotSame(map2, allMaps.get(1)); // Should be deep copy
    }
    
    @Test
    @DisplayName("Should find maps by criteria")
    void shouldFindMapsByCriteria() {
        // Given
        Map<String, Object> spanishMap = createSpanishExpressionMap();
        Map<String, Object> englishMap = createEnglishExpressionMap();
        dataBase.add(spanishMap);
        dataBase.add(englishMap);
        
        // When
        List<Map<String, Object>> spanishResults = dataBase.findBy("language", "spanish");
        List<Map<String, Object>> englishResults = dataBase.findBy("language", "english");
        
        // Then
        assertEquals(1, spanishResults.size());
        assertEquals(1, englishResults.size());
    }
    
    @Test
    @DisplayName("Should get map by index")
    void shouldGetMapByIndex() {
        // Given
        Map<String, Object> map1 = createSpanishExpressionMap();
        Map<String, Object> map2 = createEnglishExpressionMap();
        dataBase.add(map1);
        dataBase.add(map2);
        
        // When
        Map<String, Object> foundMap = dataBase.get(0);
        Map<String, Object> foundMap2 = dataBase.get(1);
        Map<String, Object> notFoundMap = dataBase.get(5);
        
        // Then
        assertNotNull(foundMap);
        assertNotNull(foundMap2);
        assertNull(notFoundMap);
        assertEquals("spanish", foundMap.get("language"));
        assertEquals("english", foundMap2.get("language"));
    }
    
    @Test
    @DisplayName("Should update map successfully")
    void shouldUpdateMapSuccessfully() {
        // Given
        Map<String, Object> originalMap = createSpanishExpressionMap();
        dataBase.add(originalMap);
        
        Map<String, Object> updatedMap = createEnglishExpressionMap();
        
        // When
        boolean result = dataBase.update(0, updatedMap);
        
        // Then
        assertTrue(result);
        Map<String, Object> retrievedMap = dataBase.get(0);
        assertNotNull(retrievedMap);
        assertEquals("english", retrievedMap.get("language"));
    }
    
    @Test
    @DisplayName("Should not update with invalid index")
    void shouldNotUpdateWithInvalidIndex() {
        // Given
        Map<String, Object> map = createSpanishExpressionMap();
        dataBase.add(map);
        
        // When
        boolean result = dataBase.update(5, map);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should not update with null map")
    void shouldNotUpdateWithNullMap() {
        // Given
        Map<String, Object> map = createSpanishExpressionMap();
        dataBase.add(map);
        
        // When
        boolean result = dataBase.update(0, null);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should remove map successfully")
    void shouldRemoveMapSuccessfully() {
        // Given
        Map<String, Object> map1 = createSpanishExpressionMap();
        Map<String, Object> map2 = createEnglishExpressionMap();
        dataBase.add(map1);
        dataBase.add(map2);
        assertEquals(2, dataBase.size());
        
        // When
        boolean result = dataBase.remove(0);
        
        // Then
        assertTrue(result);
        assertEquals(1, dataBase.size());
    }
    
    @Test
    @DisplayName("Should not remove with invalid index")
    void shouldNotRemoveWithInvalidIndex() {
        // Given
        Map<String, Object> map = createSpanishExpressionMap();
        dataBase.add(map);
        
        // When
        boolean result = dataBase.remove(5);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should remove maps by criteria")
    void shouldRemoveMapsByCriteria() {
        // Given
        Map<String, Object> spanishMap1 = createSpanishExpressionMap();
        Map<String, Object> spanishMap2 = createSpanishExpressionMap();
        Map<String, Object> englishMap = createEnglishExpressionMap();
        dataBase.add(spanishMap1);
        dataBase.add(spanishMap2);
        dataBase.add(englishMap);
        assertEquals(3, dataBase.size());
        
        // When
        int removedCount = dataBase.removeBy("language", "spanish");
        
        // Then
        assertEquals(2, removedCount);
        assertEquals(1, dataBase.size());
    }
    
    @Test
    @DisplayName("Should check if index exists")
    void shouldCheckIfIndexExists() {
        // Given
        Map<String, Object> map = createSpanishExpressionMap();
        dataBase.add(map);
        
        // When & Then
        assertTrue(dataBase.exists(0));
        assertFalse(dataBase.exists(5));
        assertFalse(dataBase.exists(-1));
    }
    
    @Test
    @DisplayName("Should find index of map")
    void shouldFindIndexOfMap() {
        // Given
        Map<String, Object> spanishMap = createSpanishExpressionMap();
        Map<String, Object> englishMap = createEnglishExpressionMap();
        dataBase.add(spanishMap);
        dataBase.add(englishMap);
        
        // When
        int spanishIndex = dataBase.indexOf("language", "spanish");
        int englishIndex = dataBase.indexOf("language", "english");
        int notFoundIndex = dataBase.indexOf("language", "french");
        
        // Then
        assertEquals(0, spanishIndex);
        assertEquals(1, englishIndex);
        assertEquals(-1, notFoundIndex);
    }
    
    @Test
    @DisplayName("Should clear all maps")
    void shouldClearAllMaps() {
        // Given
        Map<String, Object> map1 = createSpanishExpressionMap();
        Map<String, Object> map2 = createEnglishExpressionMap();
        dataBase.add(map1);
        dataBase.add(map2);
        assertEquals(2, dataBase.size());
        
        // When
        dataBase.clear();
        
        // Then
        assertTrue(dataBase.isEmpty());
        assertEquals(0, dataBase.size());
    }
    
    // Helper methods to create test data
    private Map<String, Object> createSpanishExpressionMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("language", "spanish");
        map.put("expression", "Hola");
        map.put("score", 10);
        map.put("translations", Arrays.asList("Hello", "Hi"));
        return map;
    }
    
    private Map<String, Object> createEnglishExpressionMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("language", "english");
        map.put("expression", "Hello");
        map.put("score", 10);
        map.put("translations", Arrays.asList("Hola", "Saludos"));
        return map;
    }
}
