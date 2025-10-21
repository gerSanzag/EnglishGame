package com.englishgame.service.implementations;

import com.englishgame.model.SpanishExpression;
import com.englishgame.model.EnglishExpression;
import com.englishgame.service.interfaces.ScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ScoreServiceImpl Tests")
class ScoreServiceImplTest {

    private ScoreService scoreService;
    private SpanishExpression spanishExpression;
    private EnglishExpression englishExpression;

    @BeforeEach
    void setUp() {
        scoreService = new ScoreServiceImpl();
        
        // Create test expressions
        spanishExpression = new SpanishExpression("casa", 10, Arrays.asList());
        englishExpression = new EnglishExpression("house", 5, Arrays.asList());
    }

    @Test
    @DisplayName("Should add points to English expression")
    void shouldAddPointsToEnglishExpression() {
        EnglishExpression result = scoreService.addPoints(englishExpression, 3);
        
        assertNotNull(result);
        assertEquals(8, result.getScore());
        assertEquals("house", result.getExpression());
    }

    @Test
    @DisplayName("Should subtract points from English expression")
    void shouldSubtractPointsFromEnglishExpression() {
        EnglishExpression result = scoreService.subtractPoints(englishExpression, 2);
        
        assertNotNull(result);
        assertEquals(3, result.getScore());
        assertEquals("house", result.getExpression());
    }

    @Test
    @DisplayName("Should not allow negative scores when subtracting")
    void shouldNotAllowNegativeScoresWhenSubtracting() {
        EnglishExpression result = scoreService.subtractPoints(englishExpression, 10);
        
        assertNotNull(result);
        assertEquals(0, result.getScore());
    }

    @Test
    @DisplayName("Should add points to Spanish expression")
    void shouldAddPointsToSpanishExpression() {
        SpanishExpression result = scoreService.addPoints(spanishExpression, 5);
        
        assertNotNull(result);
        assertEquals(15, result.getScore());
        assertEquals("casa", result.getExpression());
    }

    @Test
    @DisplayName("Should subtract points from Spanish expression")
    void shouldSubtractPointsFromSpanishExpression() {
        SpanishExpression result = scoreService.subtractPoints(spanishExpression, 3);
        
        assertNotNull(result);
        assertEquals(7, result.getScore());
        assertEquals("casa", result.getExpression());
    }

    @Test
    @DisplayName("Should handle null English expression when adding points")
    void shouldHandleNullEnglishExpressionWhenAddingPoints() {
        EnglishExpression result = scoreService.addPoints((EnglishExpression) null, 5);
        
        assertNull(result);
    }

    @Test
    @DisplayName("Should handle null Spanish expression when adding points")
    void shouldHandleNullSpanishExpressionWhenAddingPoints() {
        SpanishExpression result = scoreService.addPoints((SpanishExpression) null, 5);
        
        assertNull(result);
    }

    @Test
    @DisplayName("Should check if English expression is learned")
    void shouldCheckIfEnglishExpressionIsLearned() {
        // Set score to learned threshold
        englishExpression.setScore(15);
        assertTrue(scoreService.isLearned(englishExpression));
        
        // Set score above threshold
        englishExpression.setScore(20);
        assertTrue(scoreService.isLearned(englishExpression));
        
        // Set score below threshold
        englishExpression.setScore(10);
        assertFalse(scoreService.isLearned(englishExpression));
    }

    @Test
    @DisplayName("Should check if Spanish expression is learned")
    void shouldCheckIfSpanishExpressionIsLearned() {
        // Set score to learned threshold
        spanishExpression.setScore(15);
        assertTrue(scoreService.isLearned(spanishExpression));
        
        // Set score above threshold
        spanishExpression.setScore(25);
        assertTrue(scoreService.isLearned(spanishExpression));
        
        // Set score below threshold
        spanishExpression.setScore(5);
        assertFalse(scoreService.isLearned(spanishExpression));
    }

    @Test
    @DisplayName("Should get current score of English expression")
    void shouldGetCurrentScoreOfEnglishExpression() {
        assertEquals(5, scoreService.getScore(englishExpression));
        
        englishExpression.setScore(12);
        assertEquals(12, scoreService.getScore(englishExpression));
    }

    @Test
    @DisplayName("Should get current score of Spanish expression")
    void shouldGetCurrentScoreOfSpanishExpression() {
        assertEquals(10, scoreService.getScore(spanishExpression));
        
        spanishExpression.setScore(8);
        assertEquals(8, scoreService.getScore(spanishExpression));
    }

    @Test
    @DisplayName("Should reset score of English expression")
    void shouldResetScoreOfEnglishExpression() {
        EnglishExpression result = scoreService.resetScore(englishExpression);
        
        assertNotNull(result);
        assertEquals(0, result.getScore());
        assertEquals("house", result.getExpression());
    }

    @Test
    @DisplayName("Should reset score of Spanish expression")
    void shouldResetScoreOfSpanishExpression() {
        SpanishExpression result = scoreService.resetScore(spanishExpression);
        
        assertNotNull(result);
        assertEquals(0, result.getScore());
        assertEquals("casa", result.getExpression());
    }

    @Test
    @DisplayName("Should calculate learning progress correctly")
    void shouldCalculateLearningProgressCorrectly() {
        // Test with score 0
        englishExpression.setScore(0);
        assertEquals(0.0, scoreService.getLearningProgress(englishExpression), 0.01);
        
        // Test with score 7.5 (50% of 15)
        englishExpression.setScore(7);
        assertEquals(46.67, scoreService.getLearningProgress(englishExpression), 0.01);
        
        // Test with score 15 (100%)
        englishExpression.setScore(15);
        assertEquals(100.0, scoreService.getLearningProgress(englishExpression), 0.01);
        
        // Test with score above threshold (should cap at 100%)
        englishExpression.setScore(20);
        assertEquals(100.0, scoreService.getLearningProgress(englishExpression), 0.01);
    }

    @Test
    @DisplayName("Should get points needed to learn")
    void shouldGetPointsNeededToLearn() {
        assertEquals(10, scoreService.getPointsNeededToLearn(5));
        assertEquals(0, scoreService.getPointsNeededToLearn(15));
        assertEquals(0, scoreService.getPointsNeededToLearn(20));
        assertEquals(15, scoreService.getPointsNeededToLearn(0));
    }

    @Test
    @DisplayName("Should get learned threshold")
    void shouldGetLearnedThreshold() {
        assertEquals(15, scoreService.getLearnedThreshold());
    }
}
