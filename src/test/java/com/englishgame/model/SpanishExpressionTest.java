package com.englishgame.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SpanishExpression Tests")
class SpanishExpressionTest {

    private SpanishExpression spanishExpression;
    private EnglishExpression englishExpression1;
    private EnglishExpression englishExpression2;

    @BeforeEach
    void setUp() {
        // Create English expressions for translations
        englishExpression1 = new EnglishExpression("house", 5, Arrays.asList());
        englishExpression2 = new EnglishExpression("home", 3, Arrays.asList());
        
        // Create Spanish expression with English translations
        spanishExpression = new SpanishExpression("casa", 10, Arrays.asList(englishExpression1, englishExpression2));
    }

    @Test
    @DisplayName("Should create Spanish expression with all fields")
    void shouldCreateSpanishExpressionWithAllFields() {
        assertNotNull(spanishExpression);
        assertEquals("casa", spanishExpression.getExpression());
        assertEquals(10, spanishExpression.getScore());
        assertEquals(2, spanishExpression.getTranslations().size());
        assertTrue(spanishExpression.getTranslations().contains(englishExpression1));
        assertTrue(spanishExpression.getTranslations().contains(englishExpression2));
    }

    @Test
    @DisplayName("Should create Spanish expression with no-args constructor")
    void shouldCreateSpanishExpressionWithNoArgsConstructor() {
        SpanishExpression emptyExpression = new SpanishExpression();
        
        assertNotNull(emptyExpression);
        assertNull(emptyExpression.getExpression());
        assertEquals(0, emptyExpression.getScore());
        assertNotNull(emptyExpression.getTranslations());
        assertTrue(emptyExpression.getTranslations().isEmpty());
    }

    @Test
    @DisplayName("Should set and get expression")
    void shouldSetAndGetExpression() {
        spanishExpression.setExpression("hogar");
        assertEquals("hogar", spanishExpression.getExpression());
    }

    @Test
    @DisplayName("Should set and get score")
    void shouldSetAndGetScore() {
        spanishExpression.setScore(15);
        assertEquals(15, spanishExpression.getScore());
    }

    @Test
    @DisplayName("Should set and get translations")
    void shouldSetAndGetTranslations() {
        EnglishExpression newTranslation = new EnglishExpression("dwelling", 2, Arrays.asList());
        List<EnglishExpression> newTranslations = Arrays.asList(newTranslation);
        
        spanishExpression.setTranslations(newTranslations);
        assertEquals(1, spanishExpression.getTranslations().size());
        assertTrue(spanishExpression.getTranslations().contains(newTranslation));
    }

    @Test
    @DisplayName("Should handle null translations")
    void shouldHandleNullTranslations() {
        spanishExpression.setTranslations(null);
        assertNull(spanishExpression.getTranslations());
    }

    @Test
    @DisplayName("Should handle empty translations list")
    void shouldHandleEmptyTranslationsList() {
        spanishExpression.setTranslations(Arrays.asList());
        assertNotNull(spanishExpression.getTranslations());
        assertTrue(spanishExpression.getTranslations().isEmpty());
    }

    @Test
    @DisplayName("Should generate correct toString")
    void shouldGenerateCorrectToString() {
        String toString = spanishExpression.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("casa"));
        assertTrue(toString.contains("10"));
    }

    @Test
    @DisplayName("Should handle negative score")
    void shouldHandleNegativeScore() {
        spanishExpression.setScore(-5);
        assertEquals(-5, spanishExpression.getScore());
    }

    @Test
    @DisplayName("Should handle zero score")
    void shouldHandleZeroScore() {
        spanishExpression.setScore(0);
        assertEquals(0, spanishExpression.getScore());
    }

    @Test
    @DisplayName("Should handle large score")
    void shouldHandleLargeScore() {
        spanishExpression.setScore(1000);
        assertEquals(1000, spanishExpression.getScore());
    }
}
