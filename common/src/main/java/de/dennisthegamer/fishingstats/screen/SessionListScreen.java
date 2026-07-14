package de.dennisthegamer.fishingstats.screen;

import de.dennisthegamer.fishingstats.data.FishingDataStore;
import de.dennisthegamer.fishingstats.data.FishingSession;
import de.dennisthegamer.fishingstats.tracker.SessionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
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
        super(Component.translatable("fishingstats.sessions.title"), Tab.SESSIONS);
    }

    private int entryHeight() {
        return (font.lineHeight + 4) * 2 + 2;
    }

    private int listStartY() {
        int y = HEADER_HEIGHT + PADDING;
        if (statusMessage != null && statusMessageTicks > 0) {
            y += font.lineHeight + 4;
        }
        return y;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        Font font = this.font;
        int lineHeight = font.lineHeight + 4;

        renderHeader(graphics, "fishingstats.sessions.title");
        renderSidebar(graphics, mouseX, mouseY);

        // Reset hint at top-left
        String resetText = I18n.get("fishingstats.sessions.reset");
        graphics.drawString(font, resetText, PADDING, 10, 0xFFFF6666, true);

        // Status message
        if (statusMessage != null && statusMessageTicks > 0) {
            graphics.drawString(font, statusMessage,
                    (width - font.width(statusMessage)) / 2, HEADER_HEIGHT + 2, 0xFF55FF55, true);
            statusMessageTicks--;
            if (statusMessageTicks == 0) statusMessage = null;
        }

        List<FishingSession> sessions = FishingDataStore.getInstance().getSessionsNewestFirst();
        int contentX = contentX();
        int listY = listStartY();
        int entryHeight = entryHeight();

        if (sessions.isEmpty()) {
            String empty = I18n.get("fishingstats.sessions.empty");
            graphics.drawString(font, empty, contentX + (width - contentX - font.width(empty)) / 2,
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
                date += " " + I18n.get("fishingstats.sessions.active");
            }
            graphics.drawString(font, date, contentX + PADDING, entryY + 3,
                    session.isActive() ? 0xFF55FF55 : TEXT_COLOR, true);
            String duration = session.formattedDuration();
            graphics.drawString(font, duration, width - font.width(duration) - PADDING, entryY + 3, MUTED_COLOR, true);

            // Line 2: biome · dimension ..... catches + treasure share
            String where = StatsFormat.prettyId(session.biomePrimary.isEmpty() ? "?" : session.biomePrimary)
                    + " · " + StatsFormat.prettyId(session.dimension);
            graphics.drawString(font, where, contentX + PADDING, entryY + 3 + lineHeight, MUTED_COLOR, true);
            String stats = I18n.get("fishingstats.sessions.stats",
                    session.catches.size(), session.treasurePercent());
            graphics.drawString(font, stats, width - font.width(stats) - PADDING, entryY + 3 + lineHeight,
                    session.treasurePercent() > 0 ? TREASURE_COLOR : MUTED_COLOR, true);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (handleSidebarClick(click)) return true;
        if (click.button() != 0) return super.mouseClicked(click, doubled);

        int listY = listStartY();
        if (click.y() >= listY && click.x() >= contentX()) {
            int index = (int) ((click.y() - listY) / entryHeight()) + scrollOffset;
            List<FishingSession> sessions = FishingDataStore.getInstance().getSessionsNewestFirst();
            if (index >= 0 && index < sessions.size()) {
                Minecraft.getInstance().setScreen(new SessionDetailScreen(sessions.get(index).id, this));
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        // R = discard the active session and pause tracking
        if (input.key() == GLFW.GLFW_KEY_R) {
            SessionManager.getInstance().resetSession();
            scrollOffset = 0;
            statusMessage = I18n.get("fishingstats.sessions.reset_confirm");
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
