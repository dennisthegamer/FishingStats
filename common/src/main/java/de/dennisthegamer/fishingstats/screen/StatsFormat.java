package de.dennisthegamer.fishingstats.screen;

import net.minecraft.client.gui.Font;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Formatting helpers shared by the FishingStats screens. */
public final class StatsFormat {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter TIME_ONLY =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE_SHORT =
            DateTimeFormatter.ofPattern("dd.MM.").withZone(ZoneId.systemDefault());

    private StatsFormat() {}

    public static String dateTime(long epochMs) {
        return DATE_TIME.format(Instant.ofEpochMilli(epochMs));
    }

    public static String time(long epochMs) {
        return TIME_ONLY.format(Instant.ofEpochMilli(epochMs));
    }

    /** Compact form for tight spaces (e.g. the sidebar): "HH:mm" for today, "dd.MM." otherwise. */
    public static String dateTimeShort(long epochMs) {
        ZoneId zone = ZoneId.systemDefault();
        Instant instant = Instant.ofEpochMilli(epochMs);
        boolean isToday = LocalDate.now(zone).equals(instant.atZone(zone).toLocalDate());
        return isToday ? TIME_ONLY.format(instant) : DATE_SHORT.format(instant);
    }

    /** "minecraft:frozen_ocean" -> "Frozen Ocean" */
    public static String prettyId(String id) {
        if (id == null || id.isEmpty()) return "?";
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        String[] words = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    /** "12.3s" for a millisecond duration; "-" if unknown. */
    public static String seconds(long ms) {
        if (ms < 0) return "-";
        return String.format("%.1fs", ms / 1000.0);
    }

    public static String truncate(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);
        while (text.length() > 1 && font.width(text) + ellipsisWidth > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + ellipsis;
    }
}
