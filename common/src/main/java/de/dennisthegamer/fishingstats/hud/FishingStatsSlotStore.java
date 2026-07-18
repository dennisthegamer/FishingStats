package de.dennisthegamer.fishingstats.hud;

import de.dennisthegamer.fishingstats.config.FishingStatsConfig;
import de.dennisthegamer.hudlib.position.HudPreset;
import de.dennisthegamer.hudlib.ui.HudSlotStore;

import java.util.List;

/** Bildet die HUD-Preset-Slots von {@link FishingStatsConfig} auf die hudlib-ui-Schnittstelle ab. */
public class FishingStatsSlotStore implements HudSlotStore {

    @Override
    public List<HudPreset> slots() {
        return FishingStatsConfig.getInstance().hudSlots;
    }

    @Override
    public void save() {
        FishingStatsConfig.getInstance().save();
    }
}
