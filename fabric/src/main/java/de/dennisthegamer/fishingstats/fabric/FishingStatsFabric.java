package de.dennisthegamer.fishingstats.fabric;

import de.dennisthegamer.fishingstats.FishingStatsClient;
import de.dennisthegamer.fishingstats.render.FishingStatsHud;
import de.dennisthegamer.hudlib.fabric.HudLibFabric;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.resources.Identifier;

/** Fabric client entrypoint - wiring only, all logic lives in the shared sources. */
public final class FishingStatsFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FishingStatsClient.init();

        // Register keybinds
        KeyMappingHelper.registerKeyMapping(FishingStatsClient.STATS_KEY);
        KeyMappingHelper.registerKeyMapping(FishingStatsClient.COMPACT_KEY);
        KeyMappingHelper.registerKeyMapping(FishingStatsClient.SESSION_TOGGLE_KEY);

        // Register HUD renderer (HudLib kapselt HudElementRegistry/attachElementAfter)
        HudLibFabric.register(
                Identifier.fromNamespaceAndPath(FishingStatsClient.MOD_ID, "hud"),
                FishingStatsHud::render
        );

        // Register tick handler
        ClientTickEvents.END_CLIENT_TICK.register(FishingStatsClient::onTick);
    }
}
