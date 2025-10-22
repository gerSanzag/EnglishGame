package com.englishgame.service.implementations;

import com.englishgame.model.SpanishExpression;
import com.englishgame.model.EnglishExpression;
import com.englishgame.service.interfaces.ScoreService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        return Optional.ofNullable(englishExpression)
                .map(expr -> {
                    int newScore = expr.getScore() + points;
                    expr.setScore(newScore);
                    log.debug("Added {} points to English expression '{}'. New score: {}", 
                            points, expr.getExpression(), newScore);
                    return expr;
                })
                .orElseGet(() -> {
                    log.warn("Cannot add points to null English expression");
                    return null;
                });
    }
    
    @Override
    public EnglishExpression subtractPoints(EnglishExpression englishExpression, int points) {
        return Optional.ofNullable(englishExpression)
                .map(expr -> {
                    int newScore = Math.max(0, expr.getScore() - points);
                    expr.setScore(newScore);
                    log.debug("Subtracted {} points from English expression '{}'. New score: {}", 
                            points, expr.getExpression(), newScore);
                    return expr;
                })
                .orElseGet(() -> {
                    log.warn("Cannot subtract points from null English expression");
                    return null;
                });
    }
    
    @Override
    public SpanishExpression addPoints(SpanishExpression spanishExpression, int points) {
        return Optional.ofNullable(spanishExpression)
                .map(expr -> {
                    int newScore = expr.getScore() + points;
                    expr.setScore(newScore);
                    log.debug("Added {} points to Spanish expression '{}'. New score: {}", 
                            points, expr.getExpression(), newScore);
                    return expr;
                })
                .orElseGet(() -> {
                    log.warn("Cannot add points to null Spanish expression");
                    return null;
                });
    }
    
    @Override
    public SpanishExpression subtractPoints(SpanishExpression spanishExpression, int points) {
        return Optional.ofNullable(spanishExpression)
                .map(expr -> {
                    int newScore = Math.max(0, expr.getScore() - points);
                    expr.setScore(newScore);
                    log.debug("Subtracted {} points from Spanish expression '{}'. New score: {}", 
                            points, expr.getExpression(), newScore);
                    return expr;
                })
                .orElseGet(() -> {
                    log.warn("Cannot subtract points from null Spanish expression");
                    return null;
                });
    }
    
    @Override
    public List<EnglishExpression> applyPenaltyToAllTranslations(SpanishExpression spanishExpression, int penaltyPoints) {
        return Optional.ofNullable(spanishExpression)
                .map(SpanishExpression::getTranslations)
                .map(translations -> translations.stream()
                        .map(englishExpr -> {
                            int dynamicPenalty = calculateDynamicPenalty(englishExpr.getScore());
                            return subtractPoints(englishExpr, dynamicPenalty);
                        })
                        .collect(java.util.stream.Collectors.toList()))
                .map(updatedExpressions -> {
                    log.debug("Applied dynamic penalty to {} English expressions", updatedExpressions.size());
                    return updatedExpressions;
                })
                .orElseGet(() -> {
                    log.warn("Cannot apply penalty to null Spanish expression or translations");
                    return new ArrayList<>();
                });
    }
    
    /**
     * Calculates dynamic penalty based on current score
     * @param currentScore the current score of the expression
     * @return penalty points to subtract
     */
    private int calculateDynamicPenalty(int currentScore) {
        if (currentScore < 5) {
            return 2; // Light penalty for low scores
        } else if (currentScore <= 10) {
            return 3; // Medium penalty for medium scores
        } else {
            return 5; // Heavy penalty for high scores
        }
    }
    
    @Override
    public boolean isLearned(EnglishExpression englishExpression) {
        return Optional.ofNullable(englishExpression)
                .map(expr -> {
                    boolean learned = expr.getScore() >= LEARNED_THRESHOLD;
                    log.debug("English expression '{}' learned status: {} (score: {})", 
                            expr.getExpression(), learned, expr.getScore());
                    return learned;
                })
                .orElse(false);
    }
    
    @Override
    public boolean isLearned(SpanishExpression spanishExpression) {
        return Optional.ofNullable(spanishExpression)
                .map(expr -> {
                    boolean learned = expr.getScore() >= LEARNED_THRESHOLD;
                    log.debug("Spanish expression '{}' learned status: {} (score: {})", 
                            expr.getExpression(), learned, expr.getScore());
                    return learned;
                })
                .orElse(false);
    }
    
    @Override
    public int getScore(EnglishExpression englishExpression) {
        return Optional.ofNullable(englishExpression)
                .map(EnglishExpression::getScore)
                .orElse(0);
    }
    
    @Override
    public int getScore(SpanishExpression spanishExpression) {
        return Optional.ofNullable(spanishExpression)
                .map(SpanishExpression::getScore)
                .orElse(0);
    }
    
    @Override
    public EnglishExpression resetScore(EnglishExpression englishExpression) {
        return Optional.ofNullable(englishExpression)
                .map(expr -> {
                    expr.setScore(0);
                    log.debug("Reset score of English expression '{}' to 0", expr.getExpression());
                    return expr;
                })
                .orElseGet(() -> {
                    log.warn("Cannot reset score of null English expression");
                    return null;
                });
    }
    
    @Override
    public SpanishExpression resetScore(SpanishExpression spanishExpression) {
        return Optional.ofNullable(spanishExpression)
                .map(expr -> {
                    expr.setScore(0);
                    log.debug("Reset score of Spanish expression '{}' to 0", expr.getExpression());
                    return expr;
                })
                .orElseGet(() -> {
                    log.warn("Cannot reset score of null Spanish expression");
                    return null;
                });
    }
    
    @Override
    public double getLearningProgress(EnglishExpression englishExpression) {
        return Optional.ofNullable(englishExpression)
                .map(expr -> {
                    double progress = (double) expr.getScore() / LEARNED_THRESHOLD * 100;
                    progress = Math.min(100.0, Math.max(0.0, progress));
                    log.debug("Learning progress for '{}': {:.1f}%", 
                            expr.getExpression(), progress);
                    return progress;
                })
                .orElse(0.0);
    }
    
    @Override
    public double getLearningProgress(SpanishExpression spanishExpression) {
        return Optional.ofNullable(spanishExpression)
                .map(expr -> {
                    double progress = (double) expr.getScore() / LEARNED_THRESHOLD * 100;
                    progress = Math.min(100.0, Math.max(0.0, progress));
                    log.debug("Learning progress for '{}': {:.1f}%", 
                            expr.getExpression(), progress);
                    return progress;
                })
                .orElse(0.0);
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
