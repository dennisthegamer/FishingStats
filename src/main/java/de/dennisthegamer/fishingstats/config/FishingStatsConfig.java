package de.dennisthegamer.fishingstats.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FishingStatsConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(
            FabricLoader.getInstance().getConfigDir().toFile(),
            "fishingstats.json"
    );

    private static FishingStatsConfig INSTANCE = null;

    // HUD Settings
    public boolean hudEnabled = true;
    /** Single-line HUD (casts and catches only); toggled via keybind or config screen. */
    public boolean hudCompact = false;
    public String hudPosition = "TOP_LEFT";
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
                    return config;
                }
            } catch (IOException e) {
                System.err.println("Failed to load FishingStats config: " + e.getMessage());
            }
        }
        FishingStatsConfig config = new FishingStatsConfig();
        config.save();
        return config;
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
