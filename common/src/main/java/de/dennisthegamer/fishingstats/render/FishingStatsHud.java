package de.dennisthegamer.fishingstats.render;

import de.dennisthegamer.fishingstats.config.FishingStatsConfig;
import de.dennisthegamer.fishingstats.data.FishingSession;
import de.dennisthegamer.fishingstats.tracker.SessionManager;
import de.dennisthegamer.hudlib.effect.HudEffects;
import de.dennisthegamer.hudlib.ui.HudPanel;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;

/**
 * Two-phase HUD (plan section 7): {@link #tick} extracts the session state on the game
 * tick, {@link #render} only draws the extracted snapshot on the HUD layer.
 * Position/Skalierung/Hintergrund laufen über HudLibs {@link HudPanel}; die Effekt-Timer
 * über {@link HudEffects}. computeLayout/drawContent werden von Live-Render, Editor-Vorschau
 * und measureBox geteilt.
 */
public class FishingStatsHud {

    private static final int PADDING = 6;
    private static final int FLASH_DURATION = 15;

    private static final HudEffects EFFECTS = new HudEffects(FLASH_DURATION);

    /** Immutable snapshot extracted in the tick phase. */
    private record HudState(int casts, int catches, int treasurePercent, ItemStack lastCatch, boolean fishingNow) {}

    /** Vorberechnete Zeilen + Boxmaße; von Live-Render, Vorschau und measureBox geteilt. */
    private record Layout(String title, String castsLine, String treasureLine,
                          boolean compact, boolean hasCatchIcon, ItemStack lastCatch,
                          int width, int height) {}

    private static HudState state = new HudState(0, 0, 0, ItemStack.EMPTY, false);
    private static ItemStack lastCatch = ItemStack.EMPTY;

    /** Called by the tracker for every detected catch (also in treasure-only mode). */
    public static void onCatch(ItemStack stack) {
        lastCatch = stack;
        EFFECTS.triggerFlash();
    }

    /** Data extraction phase - runs on the client tick, never during rendering. */
    public static void tick(Minecraft client) {
        EFFECTS.tick();

        FishingSession session = SessionManager.getInstance().getActiveSession();
        boolean fishingNow = client.player != null && client.player.fishing != null;
        if (session == null) {
            state = new HudState(0, 0, 0, lastCatch, fishingNow);
        } else {
            state = new HudState(session.totalCasts, session.catches.size(),
                    session.treasurePercent(), lastCatch, fishingNow);
        }
    }

    private static Layout computeLayout(Font font) {
        FishingStatsConfig config = FishingStatsConfig.getInstance();
        HudState snapshot = state;

        boolean compact = config.hudCompact;
        String title = I18n.get("fishingstats.hud.title");
        String castsLine = I18n.get("fishingstats.hud.casts", snapshot.casts(), snapshot.catches());
        String treasureLine = I18n.get("fishingstats.hud.treasure", snapshot.treasurePercent());

        // Running session time in the title ("Name - 12:34"); compact appends it to the
        // stats line. Pause symbol (U+23F8) only while paused - no "active" text.
        // Ohne Session steht dort "keine Session".
        var activeSession = SessionManager.getInstance().getActiveSession();
        String dot = " " + (char) 0x00B7 + " ";                                           // middle dot
        String status = activeSession != null
                ? activeSession.formattedDuration()
                        + (SessionManager.getInstance().isPaused() ? " " + (char) 0x23F8 : "") // pause glyph
                : I18n.get("fishingstats.hud.no_session");
        if (compact) castsLine = castsLine + dot + status;
        else title = title + dot + status;

        int lineHeight = font.lineHeight + 2;
        boolean hasCatchIcon = !compact && !snapshot.lastCatch().isEmpty();
        int textWidth = compact
                ? font.width(castsLine)
                : Math.max(font.width(title), Math.max(font.width(castsLine), font.width(treasureLine)));
        int hudWidth = Math.max(textWidth + PADDING * 2, hasCatchIcon ? 90 : 0);
        int hudHeight = compact
                ? PADDING * 2 + font.lineHeight
                : PADDING * 2 + lineHeight * 3 + (hasCatchIcon ? 20 : 0);
        return new Layout(title, castsLine, treasureLine, compact, hasCatchIcon,
                snapshot.lastCatch(), hudWidth, hudHeight);
    }

    /** Breite/Höhe der aktuellen Box (für den HudBoxProvider des Editors). */
    public static int[] measureBox() {
        Layout layout = computeLayout(Minecraft.getInstance().font);
        return new int[] { layout.width(), layout.height() };
    }

    /** Render phase - draws only the snapshot extracted in {@link #tick}. */
    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gui.hud.isHidden()) return;

        FishingStatsConfig config = FishingStatsConfig.getInstance();
        if (!config.hudEnabled) return;

        HudState snapshot = state;
        boolean sessionActive = SessionManager.getInstance().getActiveSession() != null;
        // Der Schalter regiert BEIDE Ausstiege. Vorher fragte der zweite ihn nicht und versteckte
        // das HUD vor dem ersten Auswurf (casts() == 0), obwohl "Immer sichtbar" an war.
        if (!config.hudVisibleAlways) {
            if (!snapshot.fishingNow() && !sessionActive) return;
            // !fishingNow MUSS bleiben: sonst verschwaende das HUD bei Schalter-aus genau im
            // Moment des ersten Auswurfs, weil casts() dann noch 0 ist.
            if (snapshot.casts() == 0 && !snapshot.fishingNow()) return;
        }

        Font font = client.font;
        Layout layout = computeLayout(font);
        HudPanel.draw(graphics, config.getHudPlacement(), layout.width(), layout.height(),
                config.hudScale, config.hudOpacity,
                (g, x, y) -> drawContent(g, x, y, layout, font));
    }

    /** Editor-Vorschau: zeichnet die Box an expliziten (skalierten) Koordinaten. */
    public static void drawPreview(GuiGraphicsExtractor graphics, int x, int y, float scale) {
        Font font = Minecraft.getInstance().font;
        Layout layout = computeLayout(font);
        HudPanel.drawAt(graphics, x, y, layout.width(), layout.height(),
                scale, FishingStatsConfig.getInstance().hudOpacity,
                (g, bx, by) -> drawContent(g, bx, by, layout, font));
    }

    private static void drawContent(GuiGraphicsExtractor graphics, int x, int y, Layout layout, Font font) {
        if (EFFECTS.isFlashing()) {
            int flashAlpha = (int) (EFFECTS.flashAlpha() * 80);
            graphics.fill(x - 1, y - 1, x + layout.width() + 1, y + layout.height() + 1,
                    (flashAlpha << 24) | EFFECTS.flashColor());
        }

        int lineHeight = font.lineHeight + 2;
        int currentY = y + PADDING;
        if (layout.compact()) {
            graphics.text(font, layout.castsLine(), x + PADDING, currentY, 0xFFFFFFFF, true);
            return;
        }
        graphics.text(font, layout.title(),
                x + (layout.width() - font.width(layout.title())) / 2, currentY, 0xFFFFD700, true);
        currentY += lineHeight;
        graphics.text(font, layout.castsLine(), x + PADDING, currentY, 0xFFFFFFFF, true);
        currentY += lineHeight;
        graphics.text(font, layout.treasureLine(), x + PADDING, currentY, 0xFFFFFFFF, true);
        currentY += lineHeight;

        if (layout.hasCatchIcon()) {
            graphics.item(layout.lastCatch(), x + PADDING, currentY);
            graphics.text(font, I18n.get("fishingstats.hud.last_catch"),
                    x + PADDING + 20, currentY + 4, 0xFFAAAAAA, true);
        }
    }
}
