package de.dennisthegamer.fishingstats.render;

import de.dennisthegamer.fishingstats.config.FishingStatsConfig;
import de.dennisthegamer.fishingstats.data.FishingSession;
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

        int x = switch (config.getHudPosition()) {
            case TOP_LEFT, BOTTOM_LEFT -> MARGIN;
            case TOP_RIGHT, BOTTOM_RIGHT -> scaledWidth - hudWidth - MARGIN;
        };
        int y = switch (config.getHudPosition()) {
            case TOP_LEFT, TOP_RIGHT -> MARGIN;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> scaledHeight - hudHeight - MARGIN;
        };

        int bgColor = ((int) (config.hudOpacity * 255) << 24);
        graphics.fill(x, y, x + hudWidth, y + hudHeight, bgColor);

        if (flashTicksRemaining > 0) {
            float alpha = (float) flashTicksRemaining / FLASH_DURATION;
            int flashAlpha = (int) (alpha * 80);
            graphics.fill(x - 1, y - 1, x + hudWidth + 1, y + hudHeight + 1, (flashAlpha << 24) | 0xFFD700);
        }

        int currentY = y + PADDING;
        if (compact) {
            graphics.drawString(font, castsLine, x + PADDING, currentY, 0xFFFFFFFF, true);
            HudPose.pop(pose);
            return;
        }
        graphics.drawString(font, title, x + (hudWidth - font.width(title)) / 2, currentY, 0xFFFFD700, true);
        currentY += lineHeight;
        graphics.drawString(font, castsLine, x + PADDING, currentY, 0xFFFFFFFF, true);
        currentY += lineHeight;
        graphics.drawString(font, treasureLine, x + PADDING, currentY, 0xFFFFFFFF, true);
        currentY += lineHeight;

        if (hasCatchIcon) {
            graphics.renderItem(snapshot.lastCatch(), x + PADDING, currentY);
            graphics.drawString(font, I18n.get("fishingstats.hud.last_catch"),
                    x + PADDING + 20, currentY + 4, 0xFFAAAAAA, true);
        }

        HudPose.pop(pose);
    }
}
