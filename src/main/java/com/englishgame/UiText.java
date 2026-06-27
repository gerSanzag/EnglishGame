package com.englishgame;

/**
 * UI copy: English in {@link AppGameMode#DEFINITION}, Spanish in {@link AppGameMode#CLASSIC}.
 */
public final class UiText {

    private UiText() {
    }

    public static String t(AppGameMode mode, String english, String spanish) {
        return mode == AppGameMode.DEFINITION ? english : spanish;
    }

    public static boolean isEnglishUi(AppGameMode mode) {
        return mode == AppGameMode.DEFINITION;
    }
}
