package com.englishgame;

/**
 * Build label for logs; replace with generated properties in packaging if needed.
 */
public final class AppVersion {
    private AppVersion() {
    }

    /**
     * Debe cambiar cuando quieras comprobar que estás lanzando este build (véase también el pom.xml).
     */
    public static String getDisplayVersion() {
        return "1.0.4-phrasal-rows";
    }
}
