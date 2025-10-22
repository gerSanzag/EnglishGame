package com.englishgame.service.implementations;

import com.englishgame.model.SpanishExpression;
import com.englishgame.model.EnglishExpression;
import com.englishgame.service.interfaces.ScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Dynamic Penalty System Tests")
class DynamicPenaltyTest {

    private ScoreService scoreService;
    private SpanishExpression spanishExpression;

    @BeforeEach
    void setUp() {
        scoreService = new ScoreServiceImpl();
        
        // Create English expressions with different scores
        EnglishExpression lowScore = new EnglishExpression("house", 2, Arrays.asList());
        EnglishExpression mediumScore = new EnglishExpression("home", 7, Arrays.asList());
        EnglishExpression highScore = new EnglishExpression("dwelling", 12, Arrays.asList());
        
        spanishExpression = new SpanishExpression("casa", 10, 
                Arrays.asList(lowScore, mediumScore, highScore));
    }

    @Test
    @DisplayName("Should apply light penalty (2 points) to low score expressions")
    void shouldApplyLightPenaltyToLowScoreExpressions() {
        // Get the low score expression (score = 2)
        EnglishExpression lowScoreExpr = spanishExpression.getTranslations().get(0);
        assertEquals(2, lowScoreExpr.getScore());
        
        // Apply penalty
        List<EnglishExpression> result = scoreService.applyPenaltyToAllTranslations(spanishExpression, 0);
        
        // Check that low score expression got 2 points penalty
        EnglishExpression updatedLowScore = result.get(0);
        assertEquals(0, updatedLowScore.getScore()); // 2 - 2 = 0
        assertEquals("house", updatedLowScore.getExpression());
    }

    @Test
    @DisplayName("Should apply medium penalty (3 points) to medium score expressions")
    void shouldApplyMediumPenaltyToMediumScoreExpressions() {
        // Get the medium score expression (score = 7)
        EnglishExpression mediumScoreExpr = spanishExpression.getTranslations().get(1);
        assertEquals(7, mediumScoreExpr.getScore());
        
        // Apply penalty
        List<EnglishExpression> result = scoreService.applyPenaltyToAllTranslations(spanishExpression, 0);
        
        // Check that medium score expression got 3 points penalty
        EnglishExpression updatedMediumScore = result.get(1);
        assertEquals(4, updatedMediumScore.getScore()); // 7 - 3 = 4
        assertEquals("home", updatedMediumScore.getExpression());
    }

    @Test
    @DisplayName("Should apply heavy penalty (5 points) to high score expressions")
    void shouldApplyHeavyPenaltyToHighScoreExpressions() {
        // Get the high score expression (score = 12)
        EnglishExpression highScoreExpr = spanishExpression.getTranslations().get(2);
        assertEquals(12, highScoreExpr.getScore());
        
        // Apply penalty
        List<EnglishExpression> result = scoreService.applyPenaltyToAllTranslations(spanishExpression, 0);
        
        // Check that high score expression got 5 points penalty
        EnglishExpression updatedHighScore = result.get(2);
        assertEquals(7, updatedHighScore.getScore()); // 12 - 5 = 7
        assertEquals("dwelling", updatedHighScore.getExpression());
    }

    @Test
    @DisplayName("Should handle score exactly at 5 points")
    void shouldHandleScoreExactlyAtFivePoints() {
        // Create expression with score exactly 5
        EnglishExpression exactFive = new EnglishExpression("exact", 5, Arrays.asList());
        SpanishExpression testExpression = new SpanishExpression("test", 5, Arrays.asList(exactFive));
        
        List<EnglishExpression> result = scoreService.applyPenaltyToAllTranslations(testExpression, 0);
        
        // Should get medium penalty (3 points)
        EnglishExpression updated = result.get(0);
        assertEquals(2, updated.getScore()); // 5 - 3 = 2
    }

    @Test
    @DisplayName("Should handle score exactly at 10 points")
    void shouldHandleScoreExactlyAtTenPoints() {
        // Create expression with score exactly 10
        EnglishExpression exactTen = new EnglishExpression("exact", 10, Arrays.asList());
        SpanishExpression testExpression = new SpanishExpression("test", 10, Arrays.asList(exactTen));
        
        List<EnglishExpression> result = scoreService.applyPenaltyToAllTranslations(testExpression, 0);
        
        // Should get medium penalty (3 points)
        EnglishExpression updated = result.get(0);
        assertEquals(7, updated.getScore()); // 10 - 3 = 7
    }

    @Test
    @DisplayName("Should not allow negative scores")
    void shouldNotAllowNegativeScores() {
        // Create expression with very low score
        EnglishExpression veryLow = new EnglishExpression("verylow", 1, Arrays.asList());
        SpanishExpression testExpression = new SpanishExpression("test", 1, Arrays.asList(veryLow));
        
        List<EnglishExpression> result = scoreService.applyPenaltyToAllTranslations(testExpression, 0);
        
        // Should get light penalty (2 points) but not go below 0
        EnglishExpression updated = result.get(0);
        assertEquals(0, updated.getScore()); // max(0, 1 - 2) = 0
    }

    @Test
    @DisplayName("Should apply different penalties to mixed score expressions")
    void shouldApplyDifferentPenaltiesToMixedScoreExpressions() {
        List<EnglishExpression> result = scoreService.applyPenaltyToAllTranslations(spanishExpression, 0);
        
        // Check all three expressions got different penalties
        assertEquals(0, result.get(0).getScore()); // 2 - 2 = 0 (light penalty)
        assertEquals(4, result.get(1).getScore());   // 7 - 3 = 4 (medium penalty)
        assertEquals(7, result.get(2).getScore());  // 12 - 5 = 7 (heavy penalty)
    }

    @Test
    @DisplayName("Should handle zero score expressions")
    void shouldHandleZeroScoreExpressions() {
        EnglishExpression zeroScore = new EnglishExpression("zero", 0, Arrays.asList());
        SpanishExpression testExpression = new SpanishExpression("test", 0, Arrays.asList(zeroScore));
        
        List<EnglishExpression> result = scoreService.applyPenaltyToAllTranslations(testExpression, 0);
        
        // Should get light penalty (2 points) but stay at 0
        EnglishExpression updated = result.get(0);
        assertEquals(0, updated.getScore()); // max(0, 0 - 2) = 0
    }
}
