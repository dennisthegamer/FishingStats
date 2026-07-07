package de.dennisthegamer.fishingstats;

import de.dennisthegamer.fishingstats.config.FishingStatsConfig;
import de.dennisthegamer.fishingstats.data.FishingDataStore;
import de.dennisthegamer.fishingstats.render.FishingStatsHud;
import de.dennisthegamer.fishingstats.screen.FishingStatsTabScreen;
import de.dennisthegamer.fishingstats.tracker.FishingTracker;
import de.dennisthegamer.fishingstats.tracker.SessionManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FishingStatsClient implements ClientModInitializer {

    public static final String MOD_ID = "fishingstats";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(MOD_ID, MOD_ID));

    private static KeyMapping statsKey;
    private static KeyMapping compactKey;
    private static KeyMapping sessionToggleKey;

    private boolean wasInWorld = false;
    /** True while the session pause was caused by the ESC menu (auto-resumes on close). */
    private boolean escPaused = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("FishingStats loaded!");

        // Load config
        FishingStatsConfig.getInstance();

        // Register keybinds
        statsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.fishingstats.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                CATEGORY
        ));
        compactKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.fishingstats.toggle_compact",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                CATEGORY
        ));
        sessionToggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.fishingstats.session_toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                CATEGORY
        ));

        // Register HUD renderer
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.BOSS_BAR,
                Identifier.fromNamespaceAndPath(MOD_ID, "hud"),
                FishingStatsHud::render
        );

        // Register tick handler
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(Minecraft client) {
        boolean inWorld = client.player != null && client.level != null;

        if (inWorld && !wasInWorld) {
            FishingDataStore.getInstance().loadFromDisk();
            SessionManager.getInstance().pause();
            String keyName = sessionToggleKey.getTranslatedKeyMessage().getString();
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
        boolean pauseScreenOpen = client.screen instanceof PauseScreen;
        if (pauseScreenOpen && !escPaused && !SessionManager.getInstance().isPaused()) {
            SessionManager.getInstance().pause();
            escPaused = true;
            LOGGER.info("Fishing session paused (ESC menu)");
        } else if (client.screen == null && escPaused) {
            // Only back in-game: submenus opened from the pause menu keep the pause
            SessionManager.getInstance().resume();
            escPaused = false;
            LOGGER.info("Fishing session resumed (ESC menu closed)");
        }

        FishingTracker.getInstance().tick(client);
        FishingStatsHud.tick(client);

        while (statsKey.consumeClick()) {
            client.setScreen(FishingStatsTabScreen.openLastTab());
        }

        while (compactKey.consumeClick()) {
            FishingStatsConfig config = FishingStatsConfig.getInstance();
            config.hudCompact = !config.hudCompact;
            config.save();
        }

        while (sessionToggleKey.consumeClick()) {
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
