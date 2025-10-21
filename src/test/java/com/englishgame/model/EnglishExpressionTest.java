package com.englishgame.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EnglishExpression Tests")
class EnglishExpressionTest {

    private EnglishExpression englishExpression;
    private SpanishExpression spanishExpression1;
    private SpanishExpression spanishExpression2;

    @BeforeEach
    void setUp() {
        // Create Spanish expressions for translations
        spanishExpression1 = new SpanishExpression("casa", 10, Arrays.asList());
        spanishExpression2 = new SpanishExpression("hogar", 8, Arrays.asList());
        
        // Create English expression with Spanish translations
        englishExpression = new EnglishExpression("house", 5, Arrays.asList(spanishExpression1, spanishExpression2));
    }

    @Test
    @DisplayName("Should create English expression with all fields")
    void shouldCreateEnglishExpressionWithAllFields() {
        assertNotNull(englishExpression);
        assertEquals("house", englishExpression.getExpression());
        assertEquals(5, englishExpression.getScore());
        assertEquals(2, englishExpression.getTranslations().size());
        assertTrue(englishExpression.getTranslations().contains(spanishExpression1));
        assertTrue(englishExpression.getTranslations().contains(spanishExpression2));
    }

    @Test
    @DisplayName("Should create English expression with no-args constructor")
    void shouldCreateEnglishExpressionWithNoArgsConstructor() {
        EnglishExpression emptyExpression = new EnglishExpression();
        
        assertNotNull(emptyExpression);
        assertNull(emptyExpression.getExpression());
        assertEquals(0, emptyExpression.getScore());
        assertNull(emptyExpression.getTranslations());
    }

    @Test
    @DisplayName("Should set and get expression")
    void shouldSetAndGetExpression() {
        englishExpression.setExpression("home");
        assertEquals("home", englishExpression.getExpression());
    }

    @Test
    @DisplayName("Should set and get score")
    void shouldSetAndGetScore() {
        englishExpression.setScore(15);
        assertEquals(15, englishExpression.getScore());
    }

    @Test
    @DisplayName("Should set and get translations")
    void shouldSetAndGetTranslations() {
        SpanishExpression newTranslation = new SpanishExpression("vivienda", 7, Arrays.asList());
        List<SpanishExpression> newTranslations = Arrays.asList(newTranslation);
        
        englishExpression.setTranslations(newTranslations);
        assertEquals(1, englishExpression.getTranslations().size());
        assertTrue(englishExpression.getTranslations().contains(newTranslation));
    }

    @Test
    @DisplayName("Should handle null translations")
    void shouldHandleNullTranslations() {
        englishExpression.setTranslations(null);
        assertNull(englishExpression.getTranslations());
    }

    @Test
    @DisplayName("Should handle empty translations list")
    void shouldHandleEmptyTranslationsList() {
        englishExpression.setTranslations(Arrays.asList());
        assertNotNull(englishExpression.getTranslations());
        assertTrue(englishExpression.getTranslations().isEmpty());
    }

    @Test
    @DisplayName("Should generate correct toString")
    void shouldGenerateCorrectToString() {
        String toString = englishExpression.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("house"));
        assertTrue(toString.contains("5"));
    }

    @Test
    @DisplayName("Should handle negative score")
    void shouldHandleNegativeScore() {
        englishExpression.setScore(-3);
        assertEquals(-3, englishExpression.getScore());
    }

    @Test
    @DisplayName("Should handle zero score")
    void shouldHandleZeroScore() {
        englishExpression.setScore(0);
        assertEquals(0, englishExpression.getScore());
    }

    @Test
    @DisplayName("Should handle large score")
    void shouldHandleLargeScore() {
        englishExpression.setScore(999);
        assertEquals(999, englishExpression.getScore());
    }

    @Test
    @DisplayName("Should handle learned threshold score")
    void shouldHandleLearnedThresholdScore() {
        englishExpression.setScore(15);
        assertEquals(15, englishExpression.getScore());
        // This would be considered "learned" in the game logic
    }

    @Test
    @DisplayName("Should handle score above learned threshold")
    void shouldHandleScoreAboveLearnedThreshold() {
        englishExpression.setScore(20);
        assertEquals(20, englishExpression.getScore());
    }
}
