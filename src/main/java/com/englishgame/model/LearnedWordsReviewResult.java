package com.englishgame.model;

/**
 * Resultado de evaluar una tirada en Review ({@code learned_words} / {@code words_definitely_learned}).
 */
public record LearnedWordsReviewResult(
        LearnedWordsReviewResult.Outcome outcome,
        boolean answeredCorrectly,
        /** Puntuación tras la tirada (o la de práctica si se reincorporó). */
        int scoreAfter,
        String expectedEnglish,
        String userEntered,
        /**
         * Clave canónica de la BBDD de práctica donde se reinsertó la tarjeta (solo {@link Outcome#DEMOTED_TO_PRACTICE});
         * {@code null} en el resto de resultados.
         */
        String restoredToPracticeDatabase
) {
    public enum Outcome {
        /** Sigue en la misma BBDD de review en la que se jugó la tirada. */
        STILL_IN_LEARNED,
        DEMOTED_TO_PRACTICE,
        /** Dominio final en {@code words_definitely_learned} (puntuación 35): purga en todas las BBDD. */
        MASTERED_REMOVED_EVERYWHERE,
        /** {@code learned_words} alcanzó 28: pasa a {@code words_definitely_learned} con 28. */
        PROMOTED_TO_DEFINITELY_LEARNED,
        /** Fallo en {@code words_definitely_learned}: vuelve a {@code learned_words} con el score penalizado. */
        RETURNED_TO_LEARNED
    }
}
