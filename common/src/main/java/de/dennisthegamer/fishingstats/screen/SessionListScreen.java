package de.dennisthegamer.fishingstats.screen;

import de.dennisthegamer.fishingstats.data.FishingDataStore;
import de.dennisthegamer.fishingstats.data.FishingSession;
import de.dennisthegamer.fishingstats.tracker.SessionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Main screen: all fishing sessions, newest first (plan section 3.1).
 * Per row: date, duration, primary biome, total catches, treasure share.
 */
public class SessionListScreen extends FishingStatsTabScreen {

    private int scrollOffset = 0;

    /** Width of the click target on the right of each row that deletes the session. */
    private static final int DELETE_COLUMN = 14;

    /** Id of the session whose delete cross is armed; -1 when nothing is armed. */
    private int pendingDeleteId = -1;

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
        // The status line is reserved unconditionally: if the row offset changed when a
        // message appears, arming a delete would shift every row down and the confirming
        // second click would land on the neighbouring session.
        return HEADER_HEIGHT + PADDING + font.lineHeight + 4;
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        Font font = this.font;
        int lineHeight = font.lineHeight + 4;

        renderHeader(graphics, "fishingstats.sessions.title");
        renderSidebar(graphics, mouseX, mouseY);

        // Reset hint at top-left
        String resetText = I18n.get("fishingstats.sessions.reset");
        graphics.text(font, resetText, PADDING, 10, 0xFFFF6666, true);

        // Status message
        if (statusMessage != null && statusMessageTicks > 0) {
            graphics.text(font, statusMessage,
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
            graphics.text(font, empty, contentX + (width - contentX - font.width(empty)) / 2,
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
            // Only the session the manager is tracking counts as active in the UI - an
            // open session from another world (persistSessions) is merely restorable
            boolean isCurrent = session == SessionManager.getInstance().getActiveSession();
            if (isCurrent) {
                date += " " + I18n.get("fishingstats.sessions.active");
            }
            graphics.text(font, date, contentX + PADDING, entryY + 3,
                    isCurrent ? 0xFF55FF55 : TEXT_COLOR, true);
            String duration = session.formattedDuration();
            graphics.text(font, duration, width - font.width(duration) - PADDING - DELETE_COLUMN,
                    entryY + 3, MUTED_COLOR, true);

            // Line 2: biome · dimension ..... catches + treasure share
            String where = StatsFormat.prettyId(session.biomePrimary.isEmpty() ? "?" : session.biomePrimary)
                    + " · " + StatsFormat.prettyId(session.dimension);
            graphics.text(font, where, contentX + PADDING, entryY + 3 + lineHeight, MUTED_COLOR, true);
            String stats = I18n.get("fishingstats.sessions.stats",
                    session.catches.size(), session.treasurePercent());
            graphics.text(font, stats, width - font.width(stats) - PADDING - DELETE_COLUMN,
                    entryY + 3 + lineHeight,
                    session.treasurePercent() > 0 ? TREASURE_COLOR : MUTED_COLOR, true);

            // Delete cross, vertically centred in the row
            boolean armed = session.id == pendingDeleteId;
            String cross = "✕";
            int crossX = width - PADDING - font.width(cross);
            int crossY = entryY + (entryHeight - font.lineHeight) / 2;
            boolean crossHovered = mouseX >= width - PADDING - DELETE_COLUMN
                    && mouseY >= entryY && mouseY < entryY + entryHeight;
            graphics.text(font, cross, crossX, crossY,
                    armed ? 0xFFFF5555 : (crossHovered ? 0xFFFFFFFF : MUTED_COLOR), true);
        }
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean consumed) {
        if (consumed) return false;
        if (handleSidebarClick(event)) {
            pendingDeleteId = -1;
            return true;
        }
        if (event.button() != 0) return super.mouseClicked(event, consumed);

        int listY = listStartY();
        if (event.y() >= listY && event.x() >= contentX()) {
            int index = (int) ((event.y() - listY) / entryHeight()) + scrollOffset;
            List<FishingSession> sessions = FishingDataStore.getInstance().getSessionsNewestFirst();
            if (index >= 0 && index < sessions.size()) {
                FishingSession session = sessions.get(index);
                if (event.x() >= width - PADDING - DELETE_COLUMN) {
                    if (pendingDeleteId == session.id) {
                        SessionManager.getInstance().deleteSession(session.id);
                        pendingDeleteId = -1;
                        scrollOffset = 0;
                        statusMessage = I18n.get("fishingstats.sessions.deleted", session.id);
                    } else {
                        pendingDeleteId = session.id;
                        statusMessage = I18n.get("fishingstats.sessions.delete_arm");
                    }
                    statusMessageTicks = 100;
                    return true;
                }
                pendingDeleteId = -1;
                Minecraft.getInstance().gui.setScreen(new SessionDetailScreen(session.id, this));
                return true;
            }
        }
        pendingDeleteId = -1;
        return super.mouseClicked(event, consumed);
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        // R = discard the active session and pause tracking
        if (event.key() == GLFW.GLFW_KEY_R) {
            SessionManager.getInstance().resetSession();
            scrollOffset = 0;
            statusMessage = I18n.get("fishingstats.sessions.reset_confirm");
            statusMessageTicks = 60;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (handleSidebarScroll(mouseX, scrollY)) return true;
        int size = FishingDataStore.getInstance().getSessions().size();
        scrollOffset = Math.max(0, Math.min(scrollOffset - (int) scrollY, Math.max(0, size - 3)));
        return true;
    }
}
