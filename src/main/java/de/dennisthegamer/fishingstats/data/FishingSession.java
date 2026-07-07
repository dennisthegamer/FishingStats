package de.dennisthegamer.fishingstats.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** One fishing session: starts with the first cast, ends after configurable inactivity. */
public class FishingSession {

    public int id;
    public long startTime;
    /** 0 while the session is still active. */
    public long endTime;
    /** Total milliseconds spent paused (ESC menu / manual pause); excluded from the duration. */
    public long pausedMs;
    /**
     * Start of the currently open pause segment, 0 if not paused. Persisted so that a
     * session restored via persistSessions books the whole offline gap as paused time.
     */
    public long pauseStartMs;
    public String biomePrimary = "";
    public String dimension = "";
    /**
     * Identity of the world this session belongs to ("local:<world dir>" or
     * "server:<address>"). A persisted session is only restored in the same world.
     */
    public String worldId = "";
    /** Minecraft version the session was recorded with; restore requires a match. */
    public String gameVersion = "";
    public int totalCasts;
    public List<CatchRecord> catches = new ArrayList<>();

    public boolean isActive() {
        return endTime == 0;
    }

    public long durationMs() {
        long end = endTime == 0
                ? (pauseStartMs > 0 ? pauseStartMs : System.currentTimeMillis())
                : endTime;
        return Math.max(0, end - startTime - pausedMs);
    }

    public String formattedDuration() {
        long totalSeconds = durationMs() / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) return String.format("%d:%02d:%02d", hours, minutes, seconds);
        return String.format("%d:%02d", minutes, seconds);
    }

    public int countByRarity(String rarity) {
        int n = 0;
        for (CatchRecord c : catches) {
            if (rarity.equals(c.rarity)) n++;
        }
        return n;
    }

    /** Treasure share in percent (0-100) of all catches; 0 if no catches. */
    public int treasurePercent() {
        if (catches.isEmpty()) return 0;
        return Math.round(100f * countByRarity("treasure") / catches.size());
    }

    public Map<CatchCategory, Integer> countsByCategory() {
        Map<CatchCategory, Integer> counts = new HashMap<>();
        for (CatchRecord c : catches) {
            counts.merge(c.categoryEnum(), 1, Integer::sum);
        }
        return counts;
    }

    /** Most frequent biome across catches, falling back to the biome of the first cast. */
    public void updatePrimaryBiome() {
        Map<String, Integer> counts = new HashMap<>();
        for (CatchRecord c : catches) {
            if (c.biome != null && !c.biome.isEmpty()) {
                counts.merge(c.biome, 1, Integer::sum);
            }
        }
        counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(e -> biomePrimary = e.getKey());
    }
}
