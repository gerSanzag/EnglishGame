package com.englishgame.model;

/**
 * Resultado de evaluar una tirada en Review (learned_words): +1 acierto / -5 fallo, con reglas de reingreso y dominio.
 */
public record LearnedWordsReviewResult(
        LearnedWordsReviewResult.Outcome outcome,
        boolean answeredCorrectly,
        /** Score mostrado al usuario: aprendido hasta 27, o el de práctica si se reincorporó, o 28 al dominar. */
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
        STILL_IN_LEARNED,
        DEMOTED_TO_PRACTICE,
        MASTERED_REMOVED_EVERYWHERE
    }
}
