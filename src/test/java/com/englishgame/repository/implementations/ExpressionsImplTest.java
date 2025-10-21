package com.englishgame.repository.implementations;

import com.englishgame.repository.interfaces.Expressions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ExpressionsImpl
 * Tests all CRUD operations for Map<String, Object> structure
 */
@DisplayName("Expressions Tests")
class ExpressionsImplTest {
    
    private Expressions expressions;
    
    @BeforeEach
    void setUp() {
        expressions = new ExpressionsImpl();
    }
    
    @Test
    @DisplayName("Should start with empty map")
    void shouldStartWithEmptyMap() {
        assertTrue(expressions.isEmpty());
        assertEquals(0, expressions.size());
    }
    
    @Test
    @DisplayName("Should set key-value pair successfully")
    void shouldSetKeyValuePairSuccessfully() {
        // When
        boolean result = expressions.set("language", "spanish");
        
        // Then
        assertTrue(result);
        assertEquals(1, expressions.size());
        assertFalse(expressions.isEmpty());
    }
    
    @Test
    @DisplayName("Should not set with null key")
    void shouldNotSetWithNullKey() {
        // When
        boolean result = expressions.set(null, "spanish");
        
        // Then
        assertFalse(result);
        assertTrue(expressions.isEmpty());
    }
    
    @Test
    @DisplayName("Should get value by key")
    void shouldGetValueByKey() {
        // Given
        expressions.set("language", "spanish");
        expressions.set("expression", "Hola");
        
        // When
        Object language = expressions.get("language");
        Object expression = expressions.get("expression");
        Object notFound = expressions.get("nonexistent");
        
        // Then
        assertEquals("spanish", language);
        assertEquals("Hola", expression);
        assertNull(notFound);
    }
    
    @Test
    @DisplayName("Should get all key-value pairs")
    void shouldGetAllKeyValuePairs() {
        // Given
        expressions.set("language", "spanish");
        expressions.set("expression", "Hola");
        expressions.set("score", 10);
        
        // When
        Map<String, Object> allPairs = expressions.getAll();
        
        // Then
        assertEquals(3, allPairs.size());
        assertEquals("spanish", allPairs.get("language"));
        assertEquals("Hola", allPairs.get("expression"));
        assertEquals(10, allPairs.get("score"));
        assertNotSame(expressions, allPairs); // Should be deep copy
    }
    
    @Test
    @DisplayName("Should update existing key")
    void shouldUpdateExistingKey() {
        // Given
        expressions.set("language", "spanish");
        
        // When
        boolean result = expressions.update("language", "english");
        
        // Then
        assertTrue(result);
        assertEquals("english", expressions.get("language"));
    }
    
    @Test
    @DisplayName("Should not update non-existent key")
    void shouldNotUpdateNonExistentKey() {
        // When
        boolean result = expressions.update("nonexistent", "value");
        
        // Then
        assertFalse(result);
        assertTrue(expressions.isEmpty());
    }
    
    @Test
    @DisplayName("Should not update with null key")
    void shouldNotUpdateWithNullKey() {
        // When
        boolean result = expressions.update(null, "value");
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should remove key successfully")
    void shouldRemoveKeySuccessfully() {
        // Given
        expressions.set("language", "spanish");
        expressions.set("expression", "Hola");
        assertEquals(2, expressions.size());
        
        // When
        boolean result = expressions.remove("language");
        
        // Then
        assertTrue(result);
        assertEquals(1, expressions.size());
        assertNull(expressions.get("language"));
        assertEquals("Hola", expressions.get("expression"));
    }
    
    @Test
    @DisplayName("Should not remove non-existent key")
    void shouldNotRemoveNonExistentKey() {
        // When
        boolean result = expressions.remove("nonexistent");
        
        // Then
        assertFalse(result);
        assertTrue(expressions.isEmpty());
    }
    
    @Test
    @DisplayName("Should not remove with null key")
    void shouldNotRemoveWithNullKey() {
        // When
        boolean result = expressions.remove(null);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should check if key exists")
    void shouldCheckIfKeyExists() {
        // Given
        expressions.set("language", "spanish");
        
        // When & Then
        assertTrue(expressions.containsKey("language"));
        assertFalse(expressions.containsKey("nonexistent"));
        assertFalse(expressions.containsKey(null));
    }
    
    @Test
    @DisplayName("Should check if value exists")
    void shouldCheckIfValueExists() {
        // Given
        expressions.set("language", "spanish");
        expressions.set("score", 10);
        
        // When & Then
        assertTrue(expressions.containsValue("spanish"));
        assertTrue(expressions.containsValue(10));
        assertFalse(expressions.containsValue("english"));
    }
    
    @Test
    @DisplayName("Should get all keys")
    void shouldGetAllKeys() {
        // Given
        expressions.set("language", "spanish");
        expressions.set("expression", "Hola");
        expressions.set("score", 10);
        
        // When
        Set<String> keys = expressions.getKeys();
        
        // Then
        assertEquals(3, keys.size());
        assertTrue(keys.contains("language"));
        assertTrue(keys.contains("expression"));
        assertTrue(keys.contains("score"));
    }
    
    @Test
    @DisplayName("Should get value with default")
    void shouldGetValueWithDefault() {
        // Given
        expressions.set("language", "spanish");
        
        // When
        Object existingValue = expressions.getOrDefault("language", "default");
        Object defaultValue = expressions.getOrDefault("nonexistent", "default");
        Object nullDefault = expressions.getOrDefault("nonexistent", null);
        
        // Then
        assertEquals("spanish", existingValue);
        assertEquals("default", defaultValue);
        assertNull(nullDefault);
    }
    
    @Test
    @DisplayName("Should merge another map")
    void shouldMergeAnotherMap() {
        // Given
        expressions.set("language", "spanish");
        
        Map<String, Object> otherMap = new HashMap<>();
        otherMap.put("expression", "Hola");
        otherMap.put("score", 10);
        
        // When
        boolean result = expressions.merge(otherMap);
        
        // Then
        assertTrue(result);
        assertEquals(3, expressions.size());
        assertEquals("spanish", expressions.get("language"));
        assertEquals("Hola", expressions.get("expression"));
        assertEquals(10, expressions.get("score"));
    }
    
    @Test
    @DisplayName("Should not merge null map")
    void shouldNotMergeNullMap() {
        // When
        boolean result = expressions.merge(null);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should clear all entries")
    void shouldClearAllEntries() {
        // Given
        expressions.set("language", "spanish");
        expressions.set("expression", "Hola");
        expressions.set("score", 10);
        assertEquals(3, expressions.size());
        
        // When
        expressions.clear();
        
        // Then
        assertTrue(expressions.isEmpty());
        assertEquals(0, expressions.size());
    }
    
    @Test
    @DisplayName("Should create with initial map")
    void shouldCreateWithInitialMap() {
        // Given
        Map<String, Object> initialMap = new HashMap<>();
        initialMap.put("language", "spanish");
        initialMap.put("expression", "Hola");
        
        // When
        Expressions expressionsWithInitial = new ExpressionsImpl(initialMap);
        
        // Then
        assertEquals(2, expressionsWithInitial.size());
        assertEquals("spanish", expressionsWithInitial.get("language"));
        assertEquals("Hola", expressionsWithInitial.get("expression"));
    }
    
    @Test
    @DisplayName("Should return string representation")
    void shouldReturnStringRepresentation() {
        // Given
        expressions.set("language", "spanish");
        expressions.set("expression", "Hola");
        
        // When
        String stringRep = expressions.toString();
        
        // Then
        assertNotNull(stringRep);
        assertTrue(stringRep.contains("language"));
        assertTrue(stringRep.contains("spanish"));
        assertTrue(stringRep.contains("expression"));
        assertTrue(stringRep.contains("Hola"));
    }
}
