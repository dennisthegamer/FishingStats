package de.dennisthegamer.fishingstats.neoforge;

import de.dennisthegamer.fishingstats.FishingStatsClient;
import de.dennisthegamer.fishingstats.config.FishingStatsConfigScreen;
import de.dennisthegamer.fishingstats.render.FishingStatsHud;
import de.dennisthegamer.hudlib.neoforge.HudLibNeoForge;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/** NeoForge client entrypoint - wiring only, all logic lives in the shared sources. */
@Mod(value = FishingStatsClient.MOD_ID, dist = Dist.CLIENT)
public final class FishingStatsNeoForge {

    public FishingStatsNeoForge(ModContainer container, IEventBus modBus) {
        FishingStatsClient.init();

        // Register keybinds
        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            event.registerCategory(FishingStatsClient.CATEGORY);
            event.register(FishingStatsClient.STATS_KEY);
            event.register(FishingStatsClient.COMPACT_KEY);
            event.register(FishingStatsClient.SESSION_TOGGLE_KEY);
        });

        // Register HUD renderer (HudLib kapselt registerAbove/BOSS_OVERLAY)
        modBus.addListener((RegisterGuiLayersEvent event) -> HudLibNeoForge.register(
                event,
                Identifier.fromNamespaceAndPath(FishingStatsClient.MOD_ID, "hud"),
                FishingStatsHud::render
        ));

        // Register tick handler
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) ->
                FishingStatsClient.onTick(Minecraft.getInstance()));

        // Config screen (equivalent of the ModMenu integration on Fabric; needs YACL installed)
        if (ModList.get().isLoaded("yet_another_config_lib_v3")) {
            container.registerExtensionPoint(IConfigScreenFactory.class,
                            (modContainer, parent) -> FishingStatsConfigScreen.create(parent));
        }
    }
}
