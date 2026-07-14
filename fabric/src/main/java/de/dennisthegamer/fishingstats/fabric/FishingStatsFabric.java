package de.dennisthegamer.fishingstats.fabric;

import de.dennisthegamer.fishingstats.FishingStatsClient;
import net.fabricmc.api.ClientModInitializer;

/** Fabric client entrypoint - wiring only, all logic lives in the shared sources. */
public final class FishingStatsFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FishingStatsClient.init();
    }
}
