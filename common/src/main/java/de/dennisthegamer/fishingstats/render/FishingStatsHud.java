package de.dennisthegamer.fishingstats.render;

import de.dennisthegamer.fishingstats.config.FishingStatsConfig;
import de.dennisthegamer.fishingstats.data.FishingSession;
import de.dennisthegamer.fishingstats.hud.position.HudPlacement;
import de.dennisthegamer.fishingstats.tracker.SessionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;

/**
 * Two-phase HUD (plan section 7): {@link #tick} extracts the session state on the game
 * tick, {@link #render} only draws the extracted snapshot on the HUD layer.
 */
public class FishingStatsHud {

    private static final int PADDING = 6;
    private static final int MARGIN = 10;
    private static final int FLASH_DURATION = 15;

    /** Immutable snapshot extracted in the tick phase. */
    private record HudState(int casts, int catches, int treasurePercent, ItemStack lastCatch, boolean fishingNow) {}

    private static HudState state = new HudState(0, 0, 0, ItemStack.EMPTY, false);
    private static ItemStack lastCatch = ItemStack.EMPTY;
    private static int flashTicksRemaining = 0;

    /** Called by the tracker for every detected catch (also in treasure-only mode). */
    public static void onCatch(ItemStack stack) {
        lastCatch = stack;
        flashTicksRemaining = FLASH_DURATION;
    }

    /** Data extraction phase - runs on the client tick, never during rendering. */
    public static void tick(Minecraft client) {
        if (flashTicksRemaining > 0) flashTicksRemaining--;

        FishingSession session = SessionManager.getInstance().getActiveSession();
        boolean fishingNow = client.player != null && client.player.fishing != null;
        if (session == null) {
            state = new HudState(0, 0, 0, lastCatch, fishingNow);
        } else {
            state = new HudState(session.totalCasts, session.catches.size(),
                    session.treasurePercent(), lastCatch, fishingNow);
        }
    }

    /** Ergebnis der Größenberechnung; von render(), measureBox() und draw() geteilt. */
    private record Layout(int width, int height, boolean compact, boolean hasCatchIcon) {}

    private static Layout computeLayout(FishingStatsConfig config, HudState snapshot, Font font) {
        boolean compact = config.hudCompact;
        String title = I18n.get("fishingstats.hud.title");
        String castsLine = I18n.get("fishingstats.hud.casts", snapshot.casts(), snapshot.catches());
        String treasureLine = I18n.get("fishingstats.hud.treasure", snapshot.treasurePercent());

        int lineHeight = font.lineHeight + 2;
        boolean hasCatchIcon = !compact && !snapshot.lastCatch().isEmpty();
        int textWidth = compact
                ? font.width(castsLine)
                : Math.max(font.width(title), Math.max(font.width(castsLine), font.width(treasureLine)));
        int hudWidth = Math.max(textWidth + PADDING * 2, hasCatchIcon ? 90 : 0);
        int hudHeight = compact
                ? PADDING * 2 + font.lineHeight
                : PADDING * 2 + lineHeight * 3 + (hasCatchIcon ? 20 : 0);
        return new Layout(hudWidth, hudHeight, compact, hasCatchIcon);
    }

    /** Zeichnet die Box an (x,y). {@code allowFlash} nur fürs Live-HUD (nicht in der Vorschau). */
    private static void draw(GuiGraphics graphics, int x, int y, Layout layout,
                             HudState snapshot, Font font, FishingStatsConfig config, boolean allowFlash) {
        int hudWidth = layout.width();
        int hudHeight = layout.height();

        int bgColor = ((int) (config.hudOpacity * 255) << 24);
        graphics.fill(x, y, x + hudWidth, y + hudHeight, bgColor);

        if (allowFlash && flashTicksRemaining > 0) {
            float alpha = (float) flashTicksRemaining / FLASH_DURATION;
            int flashAlpha = (int) (alpha * 80);
            graphics.fill(x - 1, y - 1, x + hudWidth + 1, y + hudHeight + 1, (flashAlpha << 24) | 0xFFD700);
        }

        int lineHeight = font.lineHeight + 2;
        int currentY = y + PADDING;
        if (layout.compact()) {
            String castsLine = I18n.get("fishingstats.hud.casts", snapshot.casts(), snapshot.catches());
            graphics.drawString(font, castsLine, x + PADDING, currentY, 0xFFFFFFFF, true);
            return;
        }
        String title = I18n.get("fishingstats.hud.title");
        String castsLine = I18n.get("fishingstats.hud.casts", snapshot.casts(), snapshot.catches());
        String treasureLine = I18n.get("fishingstats.hud.treasure", snapshot.treasurePercent());
        graphics.drawString(font, title, x + (hudWidth - font.width(title)) / 2, currentY, 0xFFFFD700, true);
        currentY += lineHeight;
        graphics.drawString(font, castsLine, x + PADDING, currentY, 0xFFFFFFFF, true);
        currentY += lineHeight;
        graphics.drawString(font, treasureLine, x + PADDING, currentY, 0xFFFFFFFF, true);
        currentY += lineHeight;

        if (layout.hasCatchIcon()) {
            graphics.renderItem(snapshot.lastCatch(), x + PADDING, currentY);
            graphics.drawString(font, I18n.get("fishingstats.hud.last_catch"),
                    x + PADDING + 20, currentY + 4, 0xFFAAAAAA, true);
        }
    }

    /** Render phase - draws only the snapshot extracted in {@link #tick}. */
    public static void render(GuiGraphics graphics, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) return;

        FishingStatsConfig config = FishingStatsConfig.getInstance();
        if (!config.hudEnabled) return;

        HudState snapshot = state;
        boolean sessionActive = SessionManager.getInstance().getActiveSession() != null;
        if (!config.hudVisibleAlways && !snapshot.fishingNow() && !sessionActive) return;
        if (snapshot.casts() == 0 && !snapshot.fishingNow()) return;

        Font font = client.font;
        float scale = config.hudScale;
        Object pose = null;
        if (scale != 1.0f) {
            pose = HudPose.push(graphics, scale);
            if (pose == null) scale = 1.0f;
        }

        int scaledWidth = (int) (graphics.guiWidth() / scale);
        int scaledHeight = (int) (graphics.guiHeight() / scale);

        Layout layout = computeLayout(config, snapshot, font);
        int x = config.hudPlacement.resolveX(scaledWidth, layout.width());
        int y = config.hudPlacement.resolveY(scaledHeight, layout.height());

        draw(graphics, x, y, layout, snapshot, font, config, true);
        HudPose.pop(pose);
    }

    // --- Editor-Vorschau -----------------------------------------------------

    /** Beispiel-Snapshot, damit die Box im Editor immer sichtbar/realistisch groß ist. */
    private static HudState sampleState() {
        return new HudState(42, 7, 15, ItemStack.EMPTY, false);
    }

    /** Box-Maße {width,height} der Beispiel-Box (scale-unabhängig, Font-basiert). */
    public static int[] measureBox() {
        FishingStatsConfig config = FishingStatsConfig.getInstance();
        Font font = Minecraft.getInstance().font;
        Layout layout = computeLayout(config, sampleState(), font);
        return new int[] { layout.width(), layout.height() };
    }

    /** Zeichnet die Beispiel-Box im Editor an skalierter Position (x,y). */
    public static void drawPreview(GuiGraphics graphics, int x, int y, float scale) {
        FishingStatsConfig config = FishingStatsConfig.getInstance();
        Font font = Minecraft.getInstance().font;
        Object pose = null;
        if (scale != 1.0f) {
            pose = HudPose.push(graphics, scale);
            if (pose == null) scale = 1.0f;
        }
        HudState snapshot = sampleState();
        Layout layout = computeLayout(config, snapshot, font);
        draw(graphics, x, y, layout, snapshot, font, config, false);
        HudPose.pop(pose);
    }
}
