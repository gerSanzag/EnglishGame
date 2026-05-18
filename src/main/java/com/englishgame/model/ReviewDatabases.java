package com.englishgame.model;

import java.util.List;

/**
 * Bases de datos usadas solo en el flujo Review (no aparecen en juego, View Words ni Manage Data).
 */
public final class ReviewDatabases {

    public static final String LEARNED_WORDS_KEY = "learned_words";
    public static final String WORDS_DEFINITELY_LEARNED_KEY = "words_definitely_learned";

    public static final String LEARNED_WORDS_DISPLAY = "Learned words";
    public static final String WORDS_DEFINITELY_LEARNED_DISPLAY = "Words definitely learned";

    /** Al alcanzar esta puntuación en {@link #LEARNED_WORDS_KEY} pasa a {@link #WORDS_DEFINITELY_LEARNED_KEY}. */
    public static final int LEARNED_REVIEW_GRADUATE_SCORE = 28;
    /** Puntuación de dominio final en {@link #WORDS_DEFINITELY_LEARNED_KEY}. */
    public static final int DEFINITELY_REVIEW_MASTER_SCORE = 35;

    /** Campo en {@code database_metadata} de {@link #WORDS_DEFINITELY_LEARNED_KEY}: total histórico dominado (35). */
    public static final String METADATA_DEFINITELY_MASTERED_TOTAL = "definitely_mastered_total";

    private ReviewDatabases() {
    }

    public static boolean isReviewDatabaseKey(String databaseKey) {
        if (databaseKey == null || databaseKey.isBlank()) {
            return false;
        }
        String t = databaseKey.trim();
        return LEARNED_WORDS_KEY.equalsIgnoreCase(t) || WORDS_DEFINITELY_LEARNED_KEY.equalsIgnoreCase(t);
    }

    public static String displayNameForKey(String databaseKey) {
        if (databaseKey != null && WORDS_DEFINITELY_LEARNED_KEY.equalsIgnoreCase(databaseKey.trim())) {
            return WORDS_DEFINITELY_LEARNED_DISPLAY;
        }
        return LEARNED_WORDS_DISPLAY;
    }

    public static String keyForDisplayName(String displayName) {
        if (WORDS_DEFINITELY_LEARNED_DISPLAY.equals(displayName)) {
            return WORDS_DEFINITELY_LEARNED_KEY;
        }
        return LEARNED_WORDS_KEY;
    }

    public static List<String> allKeys() {
        return List.of(LEARNED_WORDS_KEY, WORDS_DEFINITELY_LEARNED_KEY);
    }

    public static List<String> allDisplayNames() {
        return List.of(LEARNED_WORDS_DISPLAY, WORDS_DEFINITELY_LEARNED_DISPLAY);
    }
}
