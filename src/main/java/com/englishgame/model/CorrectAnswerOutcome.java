package com.englishgame.model;

/**
 * Result of scoring a correct translation, including whether it was just promoted to learned words.
 */
public record CorrectAnswerOutcome(EnglishExpression englishExpression, boolean promotedToLearnedWords) {
}
