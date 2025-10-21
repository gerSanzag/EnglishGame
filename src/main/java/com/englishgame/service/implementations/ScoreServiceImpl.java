package com.englishgame.service.implementations;

import com.englishgame.model.SpanishExpression;
import com.englishgame.model.EnglishExpression;
import com.englishgame.service.interfaces.ScoreService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of ScoreService for managing scoring system
 * Handles score calculations, penalties, and learning progression
 */
@Slf4j
public class ScoreServiceImpl implements ScoreService {
    
    private static final int LEARNED_THRESHOLD = 15;
    private static final int DEFAULT_PENALTY = 5;
    private static final int CORRECT_ANSWER_POINTS = 1;
    
    @Override
    public EnglishExpression addPoints(EnglishExpression englishExpression, int points) {
        if (englishExpression == null) {
            log.warn("Cannot add points to null English expression");
            return null;
        }
        
        int newScore = englishExpression.getScore() + points;
        englishExpression.setScore(newScore);
        
        log.debug("Added {} points to English expression '{}'. New score: {}", 
                points, englishExpression.getExpression(), newScore);
        
        return englishExpression;
    }
    
    @Override
    public EnglishExpression subtractPoints(EnglishExpression englishExpression, int points) {
        if (englishExpression == null) {
            log.warn("Cannot subtract points from null English expression");
            return null;
        }
        
        int newScore = Math.max(0, englishExpression.getScore() - points);
        englishExpression.setScore(newScore);
        
        log.debug("Subtracted {} points from English expression '{}'. New score: {}", 
                points, englishExpression.getExpression(), newScore);
        
        return englishExpression;
    }
    
    @Override
    public SpanishExpression addPoints(SpanishExpression spanishExpression, int points) {
        if (spanishExpression == null) {
            log.warn("Cannot add points to null Spanish expression");
            return null;
        }
        
        int newScore = spanishExpression.getScore() + points;
        spanishExpression.setScore(newScore);
        
        log.debug("Added {} points to Spanish expression '{}'. New score: {}", 
                points, spanishExpression.getExpression(), newScore);
        
        return spanishExpression;
    }
    
    @Override
    public SpanishExpression subtractPoints(SpanishExpression spanishExpression, int points) {
        if (spanishExpression == null) {
            log.warn("Cannot subtract points from null Spanish expression");
            return null;
        }
        
        int newScore = Math.max(0, spanishExpression.getScore() - points);
        spanishExpression.setScore(newScore);
        
        log.debug("Subtracted {} points from Spanish expression '{}'. New score: {}", 
                points, spanishExpression.getExpression(), newScore);
        
        return spanishExpression;
    }
    
    @Override
    public List<EnglishExpression> applyPenaltyToAllTranslations(SpanishExpression spanishExpression, int penaltyPoints) {
        if (spanishExpression == null || spanishExpression.getTranslations() == null) {
            log.warn("Cannot apply penalty to null Spanish expression or translations");
            return new ArrayList<>();
        }
        
        List<EnglishExpression> updatedExpressions = new ArrayList<>();
        
        for (EnglishExpression englishExpr : spanishExpression.getTranslations()) {
            EnglishExpression updated = subtractPoints(englishExpr, penaltyPoints);
            updatedExpressions.add(updated);
        }
        
        log.debug("Applied {} penalty points to {} English expressions", 
                penaltyPoints, updatedExpressions.size());
        
        return updatedExpressions;
    }
    
    @Override
    public boolean isLearned(EnglishExpression englishExpression) {
        if (englishExpression == null) {
            return false;
        }
        
        boolean learned = englishExpression.getScore() >= LEARNED_THRESHOLD;
        log.debug("English expression '{}' learned status: {} (score: {})", 
                englishExpression.getExpression(), learned, englishExpression.getScore());
        
        return learned;
    }
    
    @Override
    public boolean isLearned(SpanishExpression spanishExpression) {
        if (spanishExpression == null) {
            return false;
        }
        
        boolean learned = spanishExpression.getScore() >= LEARNED_THRESHOLD;
        log.debug("Spanish expression '{}' learned status: {} (score: {})", 
                spanishExpression.getExpression(), learned, spanishExpression.getScore());
        
        return learned;
    }
    
    @Override
    public int getScore(EnglishExpression englishExpression) {
        if (englishExpression == null) {
            return 0;
        }
        return englishExpression.getScore();
    }
    
    @Override
    public int getScore(SpanishExpression spanishExpression) {
        if (spanishExpression == null) {
            return 0;
        }
        return spanishExpression.getScore();
    }
    
    @Override
    public EnglishExpression resetScore(EnglishExpression englishExpression) {
        if (englishExpression == null) {
            log.warn("Cannot reset score of null English expression");
            return null;
        }
        
        englishExpression.setScore(0);
        log.debug("Reset score of English expression '{}' to 0", englishExpression.getExpression());
        
        return englishExpression;
    }
    
    @Override
    public SpanishExpression resetScore(SpanishExpression spanishExpression) {
        if (spanishExpression == null) {
            log.warn("Cannot reset score of null Spanish expression");
            return null;
        }
        
        spanishExpression.setScore(0);
        log.debug("Reset score of Spanish expression '{}' to 0", spanishExpression.getExpression());
        
        return spanishExpression;
    }
    
    @Override
    public double getLearningProgress(EnglishExpression englishExpression) {
        if (englishExpression == null) {
            return 0.0;
        }
        
        double progress = (double) englishExpression.getScore() / LEARNED_THRESHOLD * 100;
        progress = Math.min(100.0, Math.max(0.0, progress));
        
        log.debug("Learning progress for '{}': {:.1f}%", 
                englishExpression.getExpression(), progress);
        
        return progress;
    }
    
    @Override
    public double getLearningProgress(SpanishExpression spanishExpression) {
        if (spanishExpression == null) {
            return 0.0;
        }
        
        double progress = (double) spanishExpression.getScore() / LEARNED_THRESHOLD * 100;
        progress = Math.min(100.0, Math.max(0.0, progress));
        
        log.debug("Learning progress for '{}': {:.1f}%", 
                spanishExpression.getExpression(), progress);
        
        return progress;
    }
    
    @Override
    public int getPointsNeededToLearn(int currentScore) {
        int pointsNeeded = LEARNED_THRESHOLD - currentScore;
        return Math.max(0, pointsNeeded);
    }
    
    @Override
    public int getLearnedThreshold() {
        return LEARNED_THRESHOLD;
    }
}
