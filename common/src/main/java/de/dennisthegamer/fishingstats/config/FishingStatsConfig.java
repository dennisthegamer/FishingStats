package de.dennisthegamer.fishingstats.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.dennisthegamer.fishingstats.hud.position.HudPlacement;
import de.dennisthegamer.fishingstats.hud.position.HudPositionMigration;
import de.dennisthegamer.fishingstats.hud.position.HudPreset;
import de.dennisthegamer.fishingstats.platform.Platforms;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FishingStatsConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(
            Platforms.get().getConfigDir().toFile(),
            "fishingstats.json"
    );

    private static FishingStatsConfig INSTANCE = null;

    // HUD Settings
    public boolean hudEnabled = true;
    /** Single-line HUD (casts and catches only); toggled via keybind or config screen. */
    public boolean hudCompact = false;
    /** @deprecated Legacy-4-Ecken-Feld; nur noch zum Migrieren gelesen. Wird nach load() genullt. */
    @Deprecated
    public String hudPosition = "TOP_LEFT";
    /** Freie HUD-Position (Anker + Offset). Nach {@link #load()} immer non-null. */
    public HudPlacement hudPlacement = null;
    /** Vom Nutzer gespeicherte Positions-Slots. */
    public List<HudPreset> hudSlots = new ArrayList<>();
    public boolean hudVisibleAlways = false;
    public float hudOpacity = 0.6f;
    public float hudScale = 1.0f;

    // Tracking Settings
    /** When true only treasure catches are stored; fish and junk still update the HUD. */
    public boolean trackTreasureOnly = false;

    // Session Settings
    /** Minutes without a cast after which the session is closed automatically. */
    public int sessionSplitMinutes = 10;
    /** Keep the active session across world leaves and restore it on the next join. */
    public boolean persistSessions = false;

    public FishingStatsConfig() {
    }

    public static FishingStatsConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static FishingStatsConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                FishingStatsConfig config = GSON.fromJson(reader, FishingStatsConfig.class);
                if (config != null) {
                    config.migrateHudPosition();
                    return config;
                }
            } catch (IOException e) {
                System.err.println("Failed to load FishingStats config: " + e.getMessage());
            }
        }
        FishingStatsConfig config = new FishingStatsConfig();
        config.migrateHudPosition();
        config.save();
        return config;
    }

    /**
     * Einmalige Migration: befüllt {@link #hudPlacement} aus dem Legacy-{@link #hudPosition},
     * falls noch nicht gesetzt, und stoppt das Persistieren des Legacy-Feldes.
     * Gson serialisiert null-Felder standardmäßig nicht, daher verschwindet {@code hudPosition}
     * beim nächsten {@link #save()} aus der JSON.
     */
    public void migrateHudPosition() {
        if (hudPlacement == null) {
            hudPlacement = HudPositionMigration.fromLegacy(hudPosition);
        }
        hudPosition = null;
        if (hudSlots == null) {
            hudSlots = new ArrayList<>();
        }
    }

    public void save() {
        try {
            if (!CONFIG_FILE.getParentFile().mkdirs() && !CONFIG_FILE.getParentFile().exists()) {
                System.err.println("Failed to create config directory");
                return;
            }
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save FishingStats config: " + e.getMessage());
        }
    }

    public HudPosition getHudPosition() {
        try {
            return HudPosition.valueOf(hudPosition);
        } catch (IllegalArgumentException e) {
            return HudPosition.TOP_LEFT;
        }
    }

    public enum HudPosition {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }
}
