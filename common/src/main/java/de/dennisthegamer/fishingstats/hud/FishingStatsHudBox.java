package de.dennisthegamer.fishingstats.hud;

import de.dennisthegamer.fishingstats.config.FishingStatsConfig;
import de.dennisthegamer.fishingstats.render.FishingStatsHud;
import de.dennisthegamer.hudlib.ui.HudBoxProvider;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Adaptiert das FishingStats-HUD (Größe/Skalierung/Vorschau-Zeichnung) an die hudlib-ui-Schnittstelle. */
public class FishingStatsHudBox implements HudBoxProvider {

    @Override
    public int width() {
        return FishingStatsHud.measureBox()[0];
    }

    @Override
    public int height() {
        return FishingStatsHud.measureBox()[1];
    }

    @Override
    public float scale() {
        float s = FishingStatsConfig.getInstance().hudScale;
        return s <= 0 ? 1f : s;
    }

    @Override
    public void drawSample(GuiGraphicsExtractor graphics, int x, int y, float scale) {
        FishingStatsHud.drawPreview(graphics, x, y, scale);
    }
}
