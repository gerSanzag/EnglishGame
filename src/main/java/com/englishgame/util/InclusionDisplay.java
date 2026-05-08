package com.englishgame.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Formats {@code included_at} epoch millis for table display.
 */
public final class InclusionDisplay {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private InclusionDisplay() {
    }

    public static String formatIncludedAt(long epochMillis) {
        if (epochMillis <= 0) {
            return "—";
        }
        return FMT.format(Instant.ofEpochMilli(epochMillis));
    }
}
