package com.englishgame.service.interfaces;

import com.englishgame.model.SpanishExpression;
import com.englishgame.model.EnglishExpression;

/**
 * Service interface for managing scoring system
 * Handles score calculations, penalties, and learning progression
 */
public interface ScoreService {
    
    /**
     * Adds points to an English expression
     * @param englishExpression the English expression to update
     * @param points number of points to add
     * @return updated English expression
     */
    EnglishExpression addPoints(EnglishExpression englishExpression, int points);
    
    /**
     * Subtracts points from an English expression
     * @param englishExpression the English expression to update
     * @param points number of points to subtract
     * @return updated English expression
     */
    EnglishExpression subtractPoints(EnglishExpression englishExpression, int points);
    
    /**
     * Adds points to a Spanish expression
     * @param spanishExpression the Spanish expression to update
     * @param points number of points to add
     * @return updated Spanish expression
     */
    SpanishExpression addPoints(SpanishExpression spanishExpression, int points);
    
    /**
     * Subtracts points from a Spanish expression
     * @param spanishExpression the Spanish expression to update
     * @param points number of points to subtract
     * @return updated Spanish expression
     */
    SpanishExpression subtractPoints(SpanishExpression spanishExpression, int points);
    
    /**
     * Applies penalty to all English expressions in a Spanish expression's translations
     * @param spanishExpression the Spanish expression containing the translations
     * @param penaltyPoints number of penalty points to subtract
     * @return list of updated English expressions
     */
    java.util.List<EnglishExpression> applyPenaltyToAllTranslations(SpanishExpression spanishExpression, int penaltyPoints);
    
    /**
     * Checks if an English expression has reached the learned threshold
     * @param englishExpression the English expression to check
     * @return true if learned, false otherwise
     */
    boolean isLearned(EnglishExpression englishExpression);
    
    /**
     * Checks if a Spanish expression has reached the learned threshold
     * @param spanishExpression the Spanish expression to check
     * @return true if learned, false otherwise
     */
    boolean isLearned(SpanishExpression spanishExpression);
    
    /**
     * Gets the current score of an English expression
     * @param englishExpression the English expression
     * @return current score
     */
    int getScore(EnglishExpression englishExpression);
    
    /**
     * Gets the current score of a Spanish expression
     * @param spanishExpression the Spanish expression
     * @return current score
     */
    int getScore(SpanishExpression spanishExpression);
    
    /**
     * Resets the score of an English expression to zero
     * @param englishExpression the English expression to reset
     * @return updated English expression
     */
    EnglishExpression resetScore(EnglishExpression englishExpression);
    
    /**
     * Resets the score of a Spanish expression to zero
     * @param spanishExpression the Spanish expression to reset
     * @return updated Spanish expression
     */
    SpanishExpression resetScore(SpanishExpression spanishExpression);
    
    /**
     * Gets the learning progress percentage of an English expression
     * @param englishExpression the English expression
     * @return progress percentage (0-100)
     */
    double getLearningProgress(EnglishExpression englishExpression);
    
    /**
     * Gets the learning progress percentage of a Spanish expression
     * @param spanishExpression the Spanish expression
     * @return progress percentage (0-100)
     */
    double getLearningProgress(SpanishExpression spanishExpression);
    
    /**
     * Gets the number of points needed to reach the learned threshold
     * @param currentScore current score
     * @return points needed to reach threshold
     */
    int getPointsNeededToLearn(int currentScore);
    
    /**
     * Gets the learned threshold value
     * @return the learned threshold (15)
     */
    int getLearnedThreshold();
}
