package com.englishgame;

/**
 * Build label for logs; replace with generated properties in packaging if needed.
 */
public final class AppVersion {
    private AppVersion() {
    }

    public static String getDisplayVersion() {
        return "local";
    }
}
