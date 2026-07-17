package de.dennisthegamer.fishingstats.render;

import de.dennisthegamer.fishingstats.config.FishingStatsConfig;
import de.dennisthegamer.fishingstats.data.FishingSession;
import de.dennisthegamer.fishingstats.tracker.SessionManager;
import de.dennisthegamer.hudlib.color.HudColors;
import de.dennisthegamer.hudlib.color.HudFlash;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

/**
 * Two-phase HUD (plan section 7): {@link #tick} extracts the session state on the game
 * tick, {@link #render} only draws the extracted snapshot on the HUD layer.
 */
public class FishingStatsHud {

    private static final int PADDING = 6;
    private static final int FLASH_DURATION = 15;

    /**
     * Immutable snapshot extracted in the tick phase. {@code duration} is empty when no session
     * runs; it lives in the snapshot rather than being read from {@link SessionManager} while
     * drawing, so the editor preview can supply a fixed one and keep a stable box width.
     */
    private record HudState(int casts, int catches, int treasurePercent, ItemStack lastCatch,
                            boolean fishingNow, String duration, boolean paused) {}

    private static HudState state = new HudState(0, 0, 0, ItemStack.EMPTY, false, "", false);
    private static ItemStack lastCatch = ItemStack.EMPTY;
    private static final HudFlash flash = new HudFlash(FLASH_DURATION);

    /** Called by the tracker for every detected catch (also in treasure-only mode). */
    public static void onCatch(ItemStack stack) {
        lastCatch = stack;
        flash.trigger(HudColors.GOLD);
    }

    /** Data extraction phase - runs on the client tick, never during rendering. */
    public static void tick(Minecraft client) {
        flash.tick();

        FishingSession session = SessionManager.getInstance().getActiveSession();
        boolean fishingNow = client.player != null && client.player.fishing != null;
        if (session == null) {
            state = new HudState(0, 0, 0, lastCatch, fishingNow, "", false);
        } else {
            state = new HudState(session.totalCasts, session.catches.size(),
                    session.treasurePercent(), lastCatch, fishingNow,
                    session.formattedDuration(), SessionManager.getInstance().isPaused());
        }
    }

    /** " · 12:34", " · 12:34 ⏸" oder " · keine Session" — nie leer. */
    private static String statusSuffix(HudState snapshot) {
        String status = snapshot.duration().isEmpty()
                ? I18n.get("fishingstats.hud.no_session")
                : snapshot.duration() + (snapshot.paused() ? " " + (char) 0x23F8 : "");  // pause glyph
        return " " + (char) 0x00B7 + " " + status;                                       // middle dot
    }

    // Die Session-Zeit hängt im Titel; kompakt gibt es keinen Titel, dort an der Stats-Zeile.
    // computeLayout() und draw() MÜSSEN dieselben Strings sehen, sonst passt die Box nicht zum Text.

    private static String titleText(HudState snapshot, boolean compact) {
        String title = I18n.get("fishingstats.hud.title");
        return compact ? title : title + statusSuffix(snapshot);
    }

    private static String castsText(HudState snapshot, boolean compact) {
        String casts = I18n.get("fishingstats.hud.casts", snapshot.casts(), snapshot.catches());
        return compact ? casts + statusSuffix(snapshot) : casts;
    }

    /** Ergebnis der Größenberechnung; von render(), measureBox() und draw() geteilt. */
    private record Layout(int width, int height, boolean compact, boolean hasCatchIcon) {}

    private static Layout computeLayout(FishingStatsConfig config, HudState snapshot, Font font) {
        boolean compact = config.hudCompact;
        String title = titleText(snapshot, compact);
        String castsLine = castsText(snapshot, compact);
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

        int bgColor = HudColors.backgroundColor(config.hudOpacity);
        graphics.fill(x, y, x + hudWidth, y + hudHeight, bgColor);

        if (allowFlash && flash.isActive()) {
            float alpha = flash.progress();
            int flashAlpha = (int) (alpha * 80);
            graphics.fill(x - 1, y - 1, x + hudWidth + 1, y + hudHeight + 1, (flashAlpha << 24) | flash.color());
        }

        int lineHeight = font.lineHeight + 2;
        int currentY = y + PADDING;
        if (layout.compact()) {
            String castsLine = castsText(snapshot, true);
            graphics.drawString(font, castsLine, x + PADDING, currentY, 0xFFFFFFFF, true);
            return;
        }
        String title = titleText(snapshot, false);
        String castsLine = castsText(snapshot, false);
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
        // Der Schalter regiert BEIDE Ausstiege. Vorher fragte der zweite ihn nicht und versteckte
        // das HUD vor dem ersten Auswurf (casts() == 0), obwohl "Immer sichtbar" an war.
        if (!config.hudVisibleAlways) {
            if (!snapshot.fishingNow() && !sessionActive) return;
            // !fishingNow MUSS bleiben: sonst verschwaende das HUD bei Schalter-aus genau im
            // Moment des ersten Auswurfs, weil casts() dann noch 0 ist.
            if (snapshot.casts() == 0 && !snapshot.fishingNow()) return;
        }

        Font font = client.font;
        float scale = config.hudScale;
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.scale(scale, scale);

        int scaledWidth = (int) (graphics.guiWidth() / scale);
        int scaledHeight = (int) (graphics.guiHeight() / scale);

        Layout layout = computeLayout(config, snapshot, font);
        int x = config.hudPlacement.resolveX(scaledWidth, layout.width());
        int y = config.hudPlacement.resolveY(scaledHeight, layout.height());

        draw(graphics, x, y, layout, snapshot, font, config, true);
        pose.popMatrix();
    }

    // --- Editor-Vorschau -----------------------------------------------------

    /**
     * Beispiel-Snapshot, damit die Box im Editor immer sichtbar/realistisch groß ist.
     * Feste Dauer statt der laufenden: sonst änderte die Box im Editor beim Ziehen ihre Breite.
     */
    private static HudState sampleState() {
        return new HudState(42, 7, 15, ItemStack.EMPTY, false, "12:34", false);
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
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.scale(scale, scale);
        HudState snapshot = sampleState();
        Layout layout = computeLayout(config, snapshot, font);
        draw(graphics, x, y, layout, snapshot, font, config, false);
        pose.popMatrix();
    }
}
