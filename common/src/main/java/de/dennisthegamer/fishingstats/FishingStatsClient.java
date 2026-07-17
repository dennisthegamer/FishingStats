package de.dennisthegamer.fishingstats;

import de.dennisthegamer.fishingstats.config.FishingStatsConfig;
import de.dennisthegamer.fishingstats.data.FishingDataStore;
import de.dennisthegamer.fishingstats.render.FishingStatsHud;
import de.dennisthegamer.fishingstats.screen.FishingStatsTabScreen;
import de.dennisthegamer.fishingstats.tracker.FishingTracker;
import de.dennisthegamer.fishingstats.tracker.SessionManager;
import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared (loader-free) client logic. Keybinds, the HUD layer and the end-of-tick hook are
 * registered here through Architectury API so that the same code runs on Fabric and NeoForge.
 *
 * <p>The keybinds use a vanilla {@link KeyMapping.Category} on purpose: registering a custom
 * category needs the {@code Identifier} class, whose mojmap name differs between 1.21.10
 * ({@code ResourceLocation}) and 1.21.11 ({@code Identifier}). A direct reference would crash
 * the NeoForge jar on 1.21.9/1.21.10, so this range avoids it entirely.
 */
public final class FishingStatsClient {

    public static final String MOD_ID = "fishingstats";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final KeyMapping STATS_KEY = new KeyMapping(
            "key.fishingstats.open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "key.categories.misc"
    );
    public static final KeyMapping COMPACT_KEY = new KeyMapping(
            "key.fishingstats.toggle_compact",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.misc"
    );
    public static final KeyMapping SESSION_TOGGLE_KEY = new KeyMapping(
            "key.fishingstats.session_toggle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.misc"
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

        // Register keybinds (Architectury handles both loaders)
        KeyMappingRegistry.register(STATS_KEY);
        KeyMappingRegistry.register(COMPACT_KEY);
        KeyMappingRegistry.register(SESSION_TOGGLE_KEY);

        // Register HUD renderer (Architectury RENDER_HUD is cross-version safe - no Identifier)
        ClientGuiEvent.RENDER_HUD.register((graphics, tickCounter) -> FishingStatsHud.render(graphics, tickCounter));

        // Register tick handler
        ClientTickEvent.CLIENT_POST.register(FishingStatsClient::onTick);
    }

    /** End-of-client-tick hook. */
    public static void onTick(Minecraft client) {
        boolean inWorld = client.player != null && client.level != null;

        if (inWorld && !wasInWorld) {
            FishingDataStore.getInstance().loadFromDisk();
            SessionManager.getInstance().pause();
            String keyName = SESSION_TOGGLE_KEY.getTranslatedKeyMessage().getString();
            FishingStatsConfig config = FishingStatsConfig.getInstance();

            if (config.persistSessions && SessionManager.getInstance().restoreSession()) {
                send(client.player,
                        Component.translatable("fishingstats.session.restored", keyName)
                                .withStyle(style -> style.withColor(0x55FF55))
                );
                LOGGER.info("World joined - restored saved fishing session");
            } else {
                send(client.player,
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
        boolean pauseScreenOpen = client.screen instanceof PauseScreen;
        if (pauseScreenOpen && !escPaused && !SessionManager.getInstance().isPaused()) {
            SessionManager.getInstance().pause();
            escPaused = true;
            LOGGER.info("Fishing session paused (ESC menu)");
        } else if (client.screen == null && escPaused) {
            SessionManager.getInstance().resume();
            escPaused = false;
            LOGGER.info("Fishing session resumed (ESC menu closed)");
        }

        FishingTracker.getInstance().tick(client);
        FishingStatsHud.tick(client);

        while (STATS_KEY.consumeClick()) {
            client.setScreen(FishingStatsTabScreen.openLastTab());
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
                send(client.player,
                        Component.translatable("fishingstats.session.paused")
                                .withStyle(style -> style.withColor(0xFFAA00))
                );
            } else {
                // Die Taste heisst "Session starten" und der Chat meldet es — also auch eine anlegen.
                session.startIfNone(System.currentTimeMillis(), FishingTracker.dimensionId(client.level));
                send(client.player,
                        Component.translatable("fishingstats.session.started")
                                .withStyle(style -> style.withColor(0x55FF55))
                );
            }
        }
    }

    /** Client-side chat message (Yarn sendMessage(text, false) == mojmap displayClientMessage). */
    private static void send(net.minecraft.world.entity.player.Player player, Component message) {
        player.displayClientMessage(message, false);
    }
}
