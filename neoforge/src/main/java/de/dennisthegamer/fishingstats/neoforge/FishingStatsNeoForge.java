package de.dennisthegamer.fishingstats.neoforge;

import de.dennisthegamer.fishingstats.FishingStatsClient;
import de.dennisthegamer.fishingstats.config.FishingStatsConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** NeoForge client entrypoint - wiring only, all logic lives in the shared sources. */
@Mod(value = FishingStatsClient.MOD_ID, dist = Dist.CLIENT)
public final class FishingStatsNeoForge {

    public FishingStatsNeoForge(ModContainer container) {
        FishingStatsClient.init();

        // Config screen (equivalent of the ModMenu integration on Fabric; needs YACL installed)
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, parent) -> FishingStatsConfigScreen.create(parent));
    }
}
