package de.dennisthegamer.fishingstats.render;

import de.dennisthegamer.fishingstats.config.FishingStatsConfig;
import de.dennisthegamer.fishingstats.data.FishingSession;
import de.dennisthegamer.fishingstats.tracker.SessionManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.ItemStack;
import org.joml.Matrix3x2fStack;

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
    public static void tick(MinecraftClient client) {
        if (flashTicksRemaining > 0) flashTicksRemaining--;

        FishingSession session = SessionManager.getInstance().getActiveSession();
        boolean fishingNow = client.player != null && client.player.fishHook != null;
        if (session == null) {
            state = new HudState(0, 0, 0, lastCatch, fishingNow);
        } else {
            state = new HudState(session.totalCasts, session.catches.size(),
                    session.treasurePercent(), lastCatch, fishingNow);
        }
    }

    /** Render phase - draws only the snapshot extracted in {@link #tick}. */
    public static void render(DrawContext graphics, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        FishingStatsConfig config = FishingStatsConfig.getInstance();
        if (!config.hudEnabled) return;

        HudState snapshot = state;
        boolean sessionActive = SessionManager.getInstance().getActiveSession() != null;
        if (!config.hudVisibleAlways && !snapshot.fishingNow() && !sessionActive) return;
        if (snapshot.casts() == 0 && !snapshot.fishingNow()) return;

        TextRenderer font = client.textRenderer;
        float scale = config.hudScale;
        Matrix3x2fStack pose = graphics.getMatrices();
        pose.pushMatrix();
        pose.scale(scale, scale);

        int scaledWidth = (int) (graphics.getScaledWindowWidth() / scale);
        int scaledHeight = (int) (graphics.getScaledWindowHeight() / scale);

        boolean compact = config.hudCompact;
        String title = I18n.translate("fishingstats.hud.title");
        String castsLine = I18n.translate("fishingstats.hud.casts", snapshot.casts(), snapshot.catches());
        String treasureLine = I18n.translate("fishingstats.hud.treasure", snapshot.treasurePercent());

        int lineHeight = font.fontHeight + 2;
        boolean hasCatchIcon = !compact && !snapshot.lastCatch().isEmpty();
        int textWidth = compact
                ? font.getWidth(castsLine)
                : Math.max(font.getWidth(title), Math.max(font.getWidth(castsLine), font.getWidth(treasureLine)));
        int hudWidth = Math.max(textWidth + PADDING * 2, hasCatchIcon ? 90 : 0);
        int hudHeight = compact
                ? PADDING * 2 + font.fontHeight
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
            graphics.drawText(font, castsLine, x + PADDING, currentY, 0xFFFFFFFF, true);
            pose.popMatrix();
            return;
        }
        graphics.drawText(font, title, x + (hudWidth - font.getWidth(title)) / 2, currentY, 0xFFFFD700, true);
        currentY += lineHeight;
        graphics.drawText(font, castsLine, x + PADDING, currentY, 0xFFFFFFFF, true);
        currentY += lineHeight;
        graphics.drawText(font, treasureLine, x + PADDING, currentY, 0xFFFFFFFF, true);
        currentY += lineHeight;

        if (hasCatchIcon) {
            graphics.drawItem(snapshot.lastCatch(), x + PADDING, currentY);
            graphics.drawText(font, I18n.translate("fishingstats.hud.last_catch"),
                    x + PADDING + 20, currentY + 4, 0xFFAAAAAA, true);
        }

        pose.popMatrix();
    }
}
