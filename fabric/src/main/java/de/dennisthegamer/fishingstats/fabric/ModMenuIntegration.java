package de.dennisthegamer.fishingstats.fabric;

import de.dennisthegamer.fishingstats.config.FishingStatsConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return FishingStatsConfigScreen::create;
    }
}
