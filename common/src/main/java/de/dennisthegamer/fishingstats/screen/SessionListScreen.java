package de.dennisthegamer.fishingstats.screen;

import de.dennisthegamer.fishingstats.data.FishingDataStore;
import de.dennisthegamer.fishingstats.data.FishingSession;
import de.dennisthegamer.fishingstats.tracker.SessionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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

    /** Minimum width of the click target on the right of each row that deletes the session,
     *  so the hitbox never becomes tiny even if the glyph itself were narrower than this. */
    private static final int MIN_DELETE_COLUMN = 14;

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

    /** True when a row starting at entryY is fully drawn by render() (which breaks the
     *  render loop once a row's bottom would exceed the screen). Clicks are bound by the
     *  same check so the leftover strip below the last visible row can never resolve to the
     *  next, undrawn session. */
    private boolean rowFullyVisible(int entryY, int entryHeight) {
        return entryY + entryHeight <= height;
    }

    /** Width of the click target on the right of each row that deletes the session. Derived
     *  from the same glyph measurement the drawing uses (width - PADDING - font.width("✕")),
     *  so a wider "✕" under Force Unicode Font can never extend past its own hitbox. */
    private int deleteColumnWidth() {
        return Math.max(MIN_DELETE_COLUMN, font.width("✕") + PADDING);
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
        int deleteColumn = deleteColumnWidth();

        if (sessions.isEmpty()) {
            String empty = I18n.get("fishingstats.sessions.empty");
            graphics.drawString(font, empty, contentX + (width - contentX - font.width(empty)) / 2,
                    height / 2, MUTED_COLOR, true);
            return;
        }

        for (int i = scrollOffset; i < sessions.size(); i++) {
            int entryY = listY + (i - scrollOffset) * entryHeight;
            if (!rowFullyVisible(entryY, entryHeight)) break;

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
            graphics.drawString(font, duration, width - font.width(duration) - PADDING - deleteColumn,
                    entryY + 3, MUTED_COLOR, true);

            // Line 2: biome · dimension ..... catches + treasure share
            String where = StatsFormat.prettyId(session.biomePrimary.isEmpty() ? "?" : session.biomePrimary)
                    + " · " + StatsFormat.prettyId(session.dimension);
            graphics.drawString(font, where, contentX + PADDING, entryY + 3 + lineHeight, MUTED_COLOR, true);
            String stats = I18n.get("fishingstats.sessions.stats",
                    session.catches.size(), session.treasurePercent());
            graphics.drawString(font, stats, width - font.width(stats) - PADDING - deleteColumn,
                    entryY + 3 + lineHeight,
                    session.treasurePercent() > 0 ? TREASURE_COLOR : MUTED_COLOR, true);

            // Delete cross, vertically centred in the row
            boolean armed = session.id == pendingDeleteId;
            String cross = "✕";
            int crossX = width - PADDING - font.width(cross);
            int crossY = entryY + (entryHeight - font.lineHeight) / 2;
            boolean crossHovered = mouseX >= width - PADDING - deleteColumn
                    && mouseY >= entryY && mouseY < entryY + entryHeight;
            graphics.drawString(font, cross, crossX, crossY,
                    armed ? 0xFFFF5555 : (crossHovered ? 0xFFFFFFFF : MUTED_COLOR), true);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (handleSidebarClick(mouseX, mouseY, button)) {
            pendingDeleteId = -1;
            return true;
        }
        if (button != 0) {
            pendingDeleteId = -1;
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int listY = listStartY();
        if (mouseY >= listY && mouseX >= contentX()) {
            int entryHeight = entryHeight();
            int index = (int) ((mouseY - listY) / entryHeight) + scrollOffset;
            int entryY = listY + (index - scrollOffset) * entryHeight;
            List<FishingSession> sessions = FishingDataStore.getInstance().getSessionsNewestFirst();
            if (index >= 0 && index < sessions.size() && rowFullyVisible(entryY, entryHeight)) {
                FishingSession session = sessions.get(index);
                if (mouseX >= width - PADDING - deleteColumnWidth()) {
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
                Minecraft.getInstance().setScreen(new SessionDetailScreen(session.id, this));
                return true;
            }
        }
        pendingDeleteId = -1;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // R = discard the active session and pause tracking
        if (keyCode == GLFW.GLFW_KEY_R) {
            SessionManager.getInstance().resetSession();
            scrollOffset = 0;
            statusMessage = I18n.get("fishingstats.sessions.reset_confirm");
            statusMessageTicks = 60;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (handleSidebarScroll(mouseX, scrollY)) return true;
        int size = FishingDataStore.getInstance().getSessions().size();
        scrollOffset = Math.max(0, Math.min(scrollOffset - (int) scrollY, Math.max(0, size - 3)));
        return true;
    }
}
