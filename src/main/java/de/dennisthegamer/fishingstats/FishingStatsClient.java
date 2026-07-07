package de.dennisthegamer.fishingstats;

import de.dennisthegamer.fishingstats.config.FishingStatsConfig;
import de.dennisthegamer.fishingstats.data.FishingDataStore;
import de.dennisthegamer.fishingstats.render.FishingStatsHud;
import de.dennisthegamer.fishingstats.screen.FishingStatsTabScreen;
import de.dennisthegamer.fishingstats.tracker.FishingTracker;
import de.dennisthegamer.fishingstats.tracker.SessionManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FishingStatsClient implements ClientModInitializer {

    public static final String MOD_ID = "fishingstats";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Same translation key that Identifier-based categories generate on 1.21.9+. */
    private static final String CATEGORY = "key.category.fishingstats.fishingstats";

    private static KeyBinding statsKey;
    private static KeyBinding compactKey;
    private static KeyBinding sessionToggleKey;

    private boolean wasInWorld = false;
    /** True while the session pause was caused by the ESC menu (auto-resumes on close). */
    private boolean escPaused = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("FishingStats loaded!");

        // Load config
        FishingStatsConfig.getInstance();

        // Register keybinds
        statsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fishingstats.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                CATEGORY
        ));
        compactKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fishingstats.toggle_compact",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                CATEGORY
        ));
        sessionToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fishingstats.session_toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                CATEGORY
        ));

        // Register HUD renderer (HudRenderCallback exists on all of 1.21-1.21.8;
        // HudElementRegistry only from 1.21.6)
        HudRenderCallback.EVENT.register(FishingStatsHud::render);

        // Register tick handler
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient client) {
        boolean inWorld = client.player != null && client.world != null;

        if (inWorld && !wasInWorld) {
            FishingDataStore.getInstance().loadFromDisk();
            SessionManager.getInstance().pause();
            String keyName = sessionToggleKey.getBoundKeyLocalizedText().getString();
            FishingStatsConfig config = FishingStatsConfig.getInstance();

            if (config.persistSessions && SessionManager.getInstance().restoreSession()) {
                client.player.sendMessage(
                        Text.translatable("fishingstats.session.restored", keyName)
                                .styled(style -> style.withColor(0x55FF55)),
                        false
                );
                LOGGER.info("World joined - restored saved fishing session");
            } else {
                client.player.sendMessage(
                        Text.translatable("fishingstats.session.press_to_start", keyName)
                                .styled(style -> style.withColor(0xFFD700)),
                        false
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
        boolean pauseScreenOpen = client.currentScreen instanceof GameMenuScreen;
        if (pauseScreenOpen && !escPaused && !SessionManager.getInstance().isPaused()) {
            SessionManager.getInstance().pause();
            escPaused = true;
            LOGGER.info("Fishing session paused (ESC menu)");
        } else if (client.currentScreen == null && escPaused) {
            // Only back in-game: submenus opened from the pause menu keep the pause
            SessionManager.getInstance().resume();
            escPaused = false;
            LOGGER.info("Fishing session resumed (ESC menu closed)");
        }

        FishingTracker.getInstance().tick(client);
        FishingStatsHud.tick(client);

        while (statsKey.wasPressed()) {
            client.setScreen(FishingStatsTabScreen.openLastTab());
        }

        while (compactKey.wasPressed()) {
            FishingStatsConfig config = FishingStatsConfig.getInstance();
            config.hudCompact = !config.hudCompact;
            config.save();
        }

        while (sessionToggleKey.wasPressed()) {
            SessionManager session = SessionManager.getInstance();
            session.togglePause();
            if (session.isPaused()) {
                client.player.sendMessage(
                        Text.translatable("fishingstats.session.paused")
                                .styled(style -> style.withColor(0xFFAA00)),
                        false
                );
            } else {
                client.player.sendMessage(
                        Text.translatable("fishingstats.session.started")
                                .styled(style -> style.withColor(0x55FF55)),
                        false
                );
            }
        }
    }
}
