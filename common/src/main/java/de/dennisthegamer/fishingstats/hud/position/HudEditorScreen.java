package de.dennisthegamer.fishingstats.hud.position;

import de.dennisthegamer.fishingstats.config.FishingStatsConfig;
import de.dennisthegamer.fishingstats.render.FishingStatsHud;
import de.dennisthegamer.hudlib.position.HudAnchor;
import de.dennisthegamer.hudlib.position.HudPlacement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Vollbild-Editor: das echte HUD (Beispiel-Box) wird an der aktuellen Arbeitskopie gezeichnet
 * und kann mit der Maus verschoben werden. Nicht pausierend, Welt bleibt sichtbar
 * (renderBackground überschrieben). Confirm schreibt in die Config, Cancel verwirft.
 */
public class HudEditorScreen extends Screen {

    private final Screen parent;
    private HudPlacement working;

    // Letzte gezeichnete Box (in skaliertem Raum) für Maus-Trefferprüfung.
    private float scale = 1.0f;
    private int scaledW, scaledH, boxX, boxY, boxW, boxH;
    private boolean dragging;
    private int grabDX, grabDY;

    public HudEditorScreen(Screen parent) {
        super(Component.translatable("fishingstats.hud.editor.title"));
        this.parent = parent;
        this.working = FishingStatsConfig.getInstance().hudPlacement;
    }

    public void applyWorking(HudPlacement placement) {
        this.working = placement;
    }

    public HudPlacement working() {
        return working;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height - 52;
        addRenderableWidget(Button.builder(Component.translatable("fishingstats.hud.editor.confirm"), b -> confirm())
                .bounds(cx - 154, y, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("fishingstats.hud.editor.presets"), b -> openPresets())
                .bounds(cx + 4, y, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(cx - 75, y + 24, 150, 20).build());
    }

    private void confirm() {
        FishingStatsConfig config = FishingStatsConfig.getInstance();
        config.hudPlacement = working;
        config.save();
        Minecraft.getInstance().setScreen(parent);
    }

    private void openPresets() {
        Minecraft.getInstance().setScreen(new HudPresetScreen(this));
    }

    @Override
    public void onClose() {
        // Cancel: nichts persistiert, einfach zurück.
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        FishingStatsConfig config = FishingStatsConfig.getInstance();
        this.scale = config.hudScale <= 0 ? 1.0f : config.hudScale;
        int[] wh = FishingStatsHud.measureBox();
        this.boxW = wh[0];
        this.boxH = wh[1];
        this.scaledW = (int) (this.width / scale);
        this.scaledH = (int) (this.height / scale);
        this.boxX = working.resolveX(scaledW, boxW);
        this.boxY = working.resolveY(scaledH, boxH);

        FishingStatsHud.drawPreview(graphics, boxX, boxY, scale);

        // Dezenter Rahmen um die aktive Box (in Bildschirm-Pixeln).
        int px = (int) (boxX * scale), py = (int) (boxY * scale);
        int pw = (int) (boxW * scale), ph = (int) (boxH * scale);
        graphics.renderOutline(px - 1, py - 1, pw + 2, ph + 2, 0xFFFFD700);

        graphics.drawCenteredString(this.font,
                Component.translatable("fishingstats.hud.editor.hint"),
                this.width / 2, 24, 0xFFFFFFFF);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // In-game: kein Abdunkeln, die laufende Welt bleibt sichtbar (wie im Referenz-Screenshot).
        // Ohne geladene Welt (vom Titelbildschirm aus geöffnet) gäbe das einen komplett schwarzen
        // Screen, daher dort den normalen Menü-Hintergrund (Panorama/Blur) zeichnen.
        if (Minecraft.getInstance().level == null) {
            super.renderBackground(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0) {
            int smx = (int) (mouseX / scale);
            int smy = (int) (mouseY / scale);
            if (smx >= boxX && smx <= boxX + boxW && smy >= boxY && smy <= boxY + boxH) {
                dragging = true;
                grabDX = smx - boxX;
                grabDY = smy - boxY;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 0) {
            int smx = (int) (mouseX / scale);
            int smy = (int) (mouseY / scale);
            int desiredX = smx - grabDX;
            int desiredY = smy - grabDY;
            int centerX = desiredX + boxW / 2;
            int centerY = desiredY + boxH / 2;
            HudAnchor anchor = HudAnchor.fromCenter(centerX, centerY, scaledW, scaledH);
            working = HudPlacement.fromAbsolute(anchor, desiredX, desiredY, scaledW, scaledH, boxW, boxH);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
