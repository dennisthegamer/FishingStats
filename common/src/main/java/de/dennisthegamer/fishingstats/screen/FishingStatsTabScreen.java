package de.dennisthegamer.fishingstats.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

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

    protected void renderSidebar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(0, HEADER_HEIGHT, SIDEBAR_WIDTH, height, 0x33000000);
        graphics.fill(SIDEBAR_WIDTH, HEADER_HEIGHT, SIDEBAR_WIDTH + 1, height, 0x44FFFFFF);

        int y = HEADER_HEIGHT + PADDING;
        for (Tab t : Tab.values()) {
            int entryTop = y - 2;
            int entryBottom = y + font.lineHeight + 2;
            boolean active = t == tab;
            boolean hovered = mouseX >= 0 && mouseX < SIDEBAR_WIDTH
                    && mouseY >= entryTop && mouseY < entryBottom;
            if (active) {
                graphics.fill(0, entryTop, SIDEBAR_WIDTH, entryBottom, 0x44FFFFFF);
            } else if (hovered) {
                graphics.fill(0, entryTop, SIDEBAR_WIDTH, entryBottom, 0x22FFFFFF);
            }
            graphics.text(font, label(t), PADDING, y, active ? HEADER_COLOR : TEXT_COLOR, true);
            y += font.lineHeight + ENTRY_STEP;
        }
    }

    /** Subclasses call this FIRST in mouseClicked so sidebar clicks never reach the content. */
    protected boolean handleSidebarClick(MouseButtonEvent event) {
        if (event.x() >= SIDEBAR_WIDTH || event.y() < HEADER_HEIGHT) return false;
        if (event.button() != 0) return true;

        int y = HEADER_HEIGHT + PADDING;
        for (Tab t : Tab.values()) {
            if (event.y() >= y - 2 && event.y() < y + font.lineHeight + 2) {
                if (t != tab) {
                    Minecraft.getInstance().gui.setScreen(createScreen(t));
                }
                return true;
            }
            y += font.lineHeight + ENTRY_STEP;
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
