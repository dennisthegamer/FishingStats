package de.dennisthegamer.fishingstats.screen;

import de.dennisthegamer.fishingstats.data.FishingDataStore;
import de.dennisthegamer.fishingstats.data.FishingSession;
import de.dennisthegamer.fishingstats.tracker.SessionManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Main screen: all fishing sessions, newest first (plan section 3.1).
 * Per row: date, duration, primary biome, total catches, treasure share.
 */
public class SessionListScreen extends FishingStatsTabScreen {

    private int scrollOffset = 0;

    // Status message for reset feedback
    private String statusMessage;
    private int statusMessageTicks;

    public SessionListScreen() {
        super(Text.translatable("fishingstats.sessions.title"), Tab.SESSIONS);
    }

    private int entryHeight() {
        return (textRenderer.fontHeight + 4) * 2 + 2;
    }

    private int listStartY() {
        int y = HEADER_HEIGHT + PADDING;
        if (statusMessage != null && statusMessageTicks > 0) {
            y += textRenderer.fontHeight + 4;
        }
        return y;
    }

    @Override
    public void render(DrawContext graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        TextRenderer font = this.textRenderer;
        int lineHeight = font.fontHeight + 4;

        renderHeader(graphics, "fishingstats.sessions.title");
        renderSidebar(graphics, mouseX, mouseY);

        // Reset hint at top-left
        String resetText = I18n.translate("fishingstats.sessions.reset");
        graphics.drawText(font, resetText, PADDING, 10, 0xFFFF6666, true);

        // Status message
        if (statusMessage != null && statusMessageTicks > 0) {
            graphics.drawText(font, statusMessage,
                    (width - font.getWidth(statusMessage)) / 2, HEADER_HEIGHT + 2, 0xFF55FF55, true);
            statusMessageTicks--;
            if (statusMessageTicks == 0) statusMessage = null;
        }

        List<FishingSession> sessions = FishingDataStore.getInstance().getSessionsNewestFirst();
        int contentX = contentX();
        int listY = listStartY();
        int entryHeight = entryHeight();

        if (sessions.isEmpty()) {
            String empty = I18n.translate("fishingstats.sessions.empty");
            graphics.drawText(font, empty, contentX + (width - contentX - font.getWidth(empty)) / 2,
                    height / 2, MUTED_COLOR, true);
            return;
        }

        for (int i = scrollOffset; i < sessions.size(); i++) {
            int entryY = listY + (i - scrollOffset) * entryHeight;
            if (entryY + entryHeight > height) break;

            FishingSession session = sessions.get(i);
            boolean hovered = mouseX >= contentX && mouseY >= entryY && mouseY < entryY + entryHeight;
            if (hovered) {
                graphics.fill(contentX, entryY, width, entryY + entryHeight, HOVER_COLOR);
            }
            graphics.fill(contentX + PADDING, entryY, width - PADDING, entryY + 1, 0x22FFFFFF);

            // Line 1: date + [active] ..... duration
            String date = StatsFormat.dateTime(session.startTime);
            if (session.isActive()) {
                date += " " + I18n.translate("fishingstats.sessions.active");
            }
            graphics.drawText(font, date, contentX + PADDING, entryY + 3,
                    session.isActive() ? 0xFF55FF55 : TEXT_COLOR, true);
            String duration = session.formattedDuration();
            graphics.drawText(font, duration, width - font.getWidth(duration) - PADDING, entryY + 3, MUTED_COLOR, true);

            // Line 2: biome · dimension ..... catches + treasure share
            String where = StatsFormat.prettyId(session.biomePrimary.isEmpty() ? "?" : session.biomePrimary)
                    + " · " + StatsFormat.prettyId(session.dimension);
            graphics.drawText(font, where, contentX + PADDING, entryY + 3 + lineHeight, MUTED_COLOR, true);
            String stats = I18n.translate("fishingstats.sessions.stats",
                    session.catches.size(), session.treasurePercent());
            graphics.drawText(font, stats, width - font.getWidth(stats) - PADDING, entryY + 3 + lineHeight,
                    session.treasurePercent() > 0 ? TREASURE_COLOR : MUTED_COLOR, true);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (handleSidebarClick(click)) return true;
        if (click.button() != 0) return super.mouseClicked(click, doubled);

        int listY = listStartY();
        if (click.y() >= listY && click.x() >= contentX()) {
            int index = (int) ((click.y() - listY) / entryHeight()) + scrollOffset;
            List<FishingSession> sessions = FishingDataStore.getInstance().getSessionsNewestFirst();
            if (index >= 0 && index < sessions.size()) {
                MinecraftClient.getInstance().setScreen(new SessionDetailScreen(sessions.get(index).id, this));
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        // R = discard the active session and pause tracking
        if (input.key() == GLFW.GLFW_KEY_R) {
            SessionManager.getInstance().resetSession();
            scrollOffset = 0;
            statusMessage = I18n.translate("fishingstats.sessions.reset_confirm");
            statusMessageTicks = 60;
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int size = FishingDataStore.getInstance().getSessions().size();
        scrollOffset = Math.max(0, Math.min(scrollOffset - (int) scrollY, Math.max(0, size - 3)));
        return true;
    }
}
