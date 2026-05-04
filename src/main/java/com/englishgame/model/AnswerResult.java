package com.englishgame.model;

/**
 * Outcome of {@link com.englishgame.controller.GameController#processAnswer(String)}.
 */
public record AnswerResult(boolean correct, String newlyLearnedEnglishWord) {

    public static AnswerResult incorrect() {
        return new AnswerResult(false, null);
    }

    public boolean isNewlyLearned() {
        return correct && newlyLearnedEnglishWord != null && !newlyLearnedEnglishWord.isBlank();
    }
}
