package de.dennisthegamer.fishingstats;

import de.dennisthegamer.fishingstats.config.FishingStatsConfig;
import de.dennisthegamer.fishingstats.data.FishingDataStore;
import de.dennisthegamer.fishingstats.platform.Platforms;
import de.dennisthegamer.fishingstats.render.FishingStatsHud;
import de.dennisthegamer.fishingstats.screen.FishingStatsTabScreen;
import de.dennisthegamer.fishingstats.tracker.FishingTracker;
import de.dennisthegamer.fishingstats.tracker.SessionManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.LevelResource;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared (loader-free) client logic. The loader modules register the keybinds, the
 * HUD layer and the end-of-tick hook and delegate to {@link #init()} / {@link #onTick}.
 */
public final class FishingStatsClient {

    public static final String MOD_ID = "fishingstats";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Running Minecraft version; persisted sessions are only restored on a match. */
    public static final String GAME_VERSION = Platforms.get().getMinecraftVersion();

    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(MOD_ID, MOD_ID));

    public static final KeyMapping STATS_KEY = new KeyMapping(
            "key.fishingstats.open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            CATEGORY
    );
    public static final KeyMapping COMPACT_KEY = new KeyMapping(
            "key.fishingstats.toggle_compact",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            CATEGORY
    );
    public static final KeyMapping SESSION_TOGGLE_KEY = new KeyMapping(
            "key.fishingstats.session_toggle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            CATEGORY
    );

    private static boolean wasInWorld = false;
    /** True while the session pause was caused by the ESC menu (auto-resumes on close). */
    private static boolean escPaused = false;

    private FishingStatsClient() {
    }

    /** Called once from each loader's client entrypoint. */
    public static void init() {
        LOGGER.info("FishingStats loaded!");

        // Load config
        FishingStatsConfig.getInstance();
    }

    /**
     * Stable identity of the joined world: the level directory in singleplayer, the
     * server address in multiplayer. Empty if neither is known - such sessions are
     * never restored.
     */
    private static String worldKey(Minecraft client) {
        IntegratedServer server = client.getSingleplayerServer();
        if (server != null) {
            return "local:" + server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        }
        ServerData data = client.getCurrentServer();
        if (data != null && data.ip != null && !data.ip.isEmpty()) {
            return "server:" + data.ip;
        }
        return "";
    }

    /** End-of-client-tick hook, wired up by the loader modules. */
    public static void onTick(Minecraft client) {
        boolean inWorld = client.player != null && client.level != null;

        if (inWorld && !wasInWorld) {
            FishingDataStore.getInstance().loadFromDisk();
            SessionManager.getInstance().setWorld(worldKey(client));
            SessionManager.getInstance().pause();
            String keyName = SESSION_TOGGLE_KEY.getTranslatedKeyMessage().getString();
            FishingStatsConfig config = FishingStatsConfig.getInstance();

            if (config.persistSessions && SessionManager.getInstance().restoreSession()) {
                client.player.sendSystemMessage(
                        Component.translatable("fishingstats.session.restored", keyName)
                                .withStyle(style -> style.withColor(0x55FF55))
                );
                LOGGER.info("World joined - restored saved fishing session");
            } else {
                client.player.sendSystemMessage(
                        Component.translatable("fishingstats.session.press_to_start", keyName)
                                .withStyle(style -> style.withColor(0xFFD700))
                );
                LOGGER.info("World joined - fishing tracker ready (paused)");
            }
        } else if (!inWorld && wasInWorld) {
            SessionManager.getInstance().pause();
            if (FishingStatsConfig.getInstance().persistSessions) {
                LOGGER.info("World left - fishing session kept for restore");
            } else {
                SessionManager.getInstance().endSession();
                LOGGER.info("World left - fishing session closed");
            }
            FishingTracker.getInstance().reset();
            escPaused = false;
        }
        wasInWorld = inWorld;

        if (!inWorld) return;

        // Pause tracking while the ESC/pause menu is open and resume once it closes.
        // A session paused manually (keybind) is untouched: escPaused is only set when
        // the ESC menu pauses a running session, so only that pause is auto-resumed.
        boolean pauseScreenOpen = client.gui.screen() instanceof PauseScreen;
        if (pauseScreenOpen && !escPaused && !SessionManager.getInstance().isPaused()) {
            SessionManager.getInstance().pause();
            escPaused = true;
            LOGGER.info("Fishing session paused (ESC menu)");
        } else if (client.gui.screen() == null && escPaused) {
            // Only back in-game: submenus opened from the pause menu keep the pause
            SessionManager.getInstance().resume();
            escPaused = false;
            LOGGER.info("Fishing session resumed (ESC menu closed)");
        }

        FishingTracker.getInstance().tick(client);
        FishingStatsHud.tick(client);

        while (STATS_KEY.consumeClick()) {
            client.gui.setScreen(FishingStatsTabScreen.openLastTab());
        }

        while (COMPACT_KEY.consumeClick()) {
            FishingStatsConfig config = FishingStatsConfig.getInstance();
            config.hudCompact = !config.hudCompact;
            config.save();
        }

        while (SESSION_TOGGLE_KEY.consumeClick()) {
            SessionManager session = SessionManager.getInstance();
            session.togglePause();
            if (session.isPaused()) {
                client.player.sendSystemMessage(
                        Component.translatable("fishingstats.session.paused")
                                .withStyle(style -> style.withColor(0xFFAA00))
                );
            } else {
                client.player.sendSystemMessage(
                        Component.translatable("fishingstats.session.started")
                                .withStyle(style -> style.withColor(0x55FF55))
                );
            }
        }
    }
}
