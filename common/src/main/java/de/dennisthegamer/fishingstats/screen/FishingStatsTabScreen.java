package de.dennisthegamer.fishingstats.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Base for the FishingStats window: navigation sidebar on the left switching between
 * session list and overall statistics. Mirrors TradeTrackerTabScreen from TradeTracker.
 */
public abstract class FishingStatsTabScreen extends Screen {

    public enum Tab { SESSIONS, STATS }

    protected static final int SIDEBAR_WIDTH = 90;
    protected static final int HEADER_HEIGHT = 30;
    protected static final int PADDING = 6;
    private static final int ENTRY_STEP = 8;

    protected static final int BG_COLOR = 0xCC000000;
    protected static final int HEADER_COLOR = 0xFFFFD700;
    protected static final int TEXT_COLOR = 0xFFFFFFFF;
    protected static final int MUTED_COLOR = 0xFFAAAAAA;
    protected static final int HOVER_COLOR = 0x22FFFFFF;
    protected static final int FISH_COLOR = 0xFF55AAFF;
    protected static final int TREASURE_COLOR = 0xFFFFD700;
    protected static final int JUNK_COLOR = 0xFF888888;

    private static Tab lastTab = Tab.SESSIONS;

    /** One clickable line in the sidebar. indent 0 = top level, 1 = nested under a tab. */
    protected record SidebarEntry(String label, int indent, boolean active, Runnable onClick) {}

    /** An entry together with its hit rectangle. Layout is computed once and shared. */
    private record SidebarRow(SidebarEntry entry, int top, int bottom) {}

    private int sidebarScroll = 0;

    private final Tab tab;

    protected FishingStatsTabScreen(Component title, Tab tab) {
        super(title);
        this.tab = tab;
        lastTab = tab;
    }

    public static Screen openLastTab() {
        return createScreen(lastTab);
    }

    private static Screen createScreen(Tab tab) {
        return tab == Tab.STATS ? new OverallStatsScreen() : new SessionListScreen();
    }

    protected int contentX() {
        return SIDEBAR_WIDTH + 1;
    }

    /** Entries shown indented under the given tab. Default: none. */
    protected List<SidebarEntry> subEntries(Tab parentTab) {
        return List.of();
    }

    /** True while a sub-entry is selected, so the parent tab is not drawn as active too. */
    protected boolean hasActiveSubEntry() {
        return false;
    }

    private List<SidebarEntry> sidebarEntries() {
        List<SidebarEntry> entries = new ArrayList<>();
        for (Tab t : Tab.values()) {
            entries.add(new SidebarEntry(label(t), 0, t == tab && !hasActiveSubEntry(),
                    () -> {
                        if (t != tab) Minecraft.getInstance().setScreen(createScreen(t));
                    }));
            entries.addAll(subEntries(t));
        }
        return entries;
    }

    /**
     * The single source of truth for sidebar geometry. Rendering, hit test and scrolling
     * all read from this, so a row can never be drawn somewhere else than it is clicked.
     */
    private List<SidebarRow> layoutRows() {
        List<SidebarRow> rows = new ArrayList<>();
        int y = HEADER_HEIGHT + PADDING - sidebarScroll;
        for (SidebarEntry entry : sidebarEntries()) {
            rows.add(new SidebarRow(entry, y - 2, y + font.lineHeight + 2));
            y += font.lineHeight + ENTRY_STEP;
        }
        return rows;
    }

    /** Shortens a label to the given pixel width, appending an ellipsis when it does not fit. */
    private String trimToWidth(String label, int maxWidth) {
        if (font.width(label) <= maxWidth) return label;
        StringBuilder sb = new StringBuilder();
        for (char c : label.toCharArray()) {
            if (font.width(sb.toString() + c + "…") > maxWidth) break;
            sb.append(c);
        }
        return sb + "…";
    }

    /** Scrolls the sidebar. Returns true when the cursor was over it. */
    protected boolean handleSidebarScroll(double mouseX, double scrollY) {
        if (mouseX >= SIDEBAR_WIDTH) return false;
        int contentHeight = sidebarEntries().size() * (font.lineHeight + ENTRY_STEP);
        int maxScroll = Math.max(0, contentHeight - (height - HEADER_HEIGHT - PADDING * 2));
        sidebarScroll = Math.max(0, Math.min(sidebarScroll - (int) (scrollY * 12), maxScroll));
        return true;
    }

    /** Scrolls the active entry into view; call from init() when sub-entries exist. */
    protected void revealActiveSidebarEntry() {
        for (SidebarRow row : layoutRows()) {
            if (!row.entry().active()) continue;
            if (row.top() < HEADER_HEIGHT + PADDING) {
                sidebarScroll -= HEADER_HEIGHT + PADDING - row.top();
            } else if (row.bottom() > height) {
                sidebarScroll += row.bottom() - height;
            }
            return;
        }
    }

    protected void renderSidebar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(0, HEADER_HEIGHT, SIDEBAR_WIDTH, height, 0x33000000);
        graphics.fill(SIDEBAR_WIDTH, HEADER_HEIGHT, SIDEBAR_WIDTH + 1, height, 0x44FFFFFF);

        for (SidebarRow row : layoutRows()) {
            // Rows scrolled out of the sidebar are skipped rather than clipped: the sidebar
            // only holds single text lines, so dropping whole rows is enough and avoids
            // relying on a scissor API that differs between versions.
            if (row.bottom() <= HEADER_HEIGHT || row.top() >= height) continue;

            SidebarEntry entry = row.entry();
            boolean hovered = mouseX >= 0 && mouseX < SIDEBAR_WIDTH
                    && mouseY >= row.top() && mouseY < row.bottom();
            if (entry.active()) {
                graphics.fill(0, row.top(), SIDEBAR_WIDTH, row.bottom(), 0x44FFFFFF);
            } else if (hovered) {
                graphics.fill(0, row.top(), SIDEBAR_WIDTH, row.bottom(), 0x22FFFFFF);
            }
            int x = PADDING + entry.indent() * 6;
            String label = trimToWidth(entry.label(), SIDEBAR_WIDTH - x - PADDING);
            graphics.text(font, label, x, row.top() + 2,
                    entry.active() ? HEADER_COLOR : TEXT_COLOR, true);
        }
    }

    /** Subclasses call this FIRST in mouseClicked so sidebar clicks never reach the content. */
    protected boolean handleSidebarClick(MouseButtonEvent event) {
        if (event.x() >= SIDEBAR_WIDTH || event.y() < HEADER_HEIGHT) return false;
        if (event.button() != 0) return true;

        for (SidebarRow row : layoutRows()) {
            if (row.bottom() <= HEADER_HEIGHT || row.top() >= height) continue;
            if (event.y() >= row.top() && event.y() < row.bottom()) {
                row.entry().onClick().run();
                return true;
            }
        }
        return true;
    }

    protected void renderHeader(GuiGraphicsExtractor graphics, String titleKey) {
        graphics.fill(0, 0, width, height, BG_COLOR);
        graphics.fill(0, 0, width, HEADER_HEIGHT, 0xDD000000);
        String title = I18n.get(titleKey);
        graphics.text(font, title, (width - font.width(title)) / 2, 10, HEADER_COLOR, true);
    }

    private String label(Tab t) {
        return I18n.get(t == Tab.STATS
                ? "fishingstats.tab.stats" : "fishingstats.tab.sessions");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
