package de.dennisthegamer.fishingstats.screen;

import de.dennisthegamer.fishingstats.data.CatchCategory;
import de.dennisthegamer.fishingstats.data.CatchRecord;
import de.dennisthegamer.fishingstats.data.FishingDataStore;
import de.dennisthegamer.fishingstats.data.FishingSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Detail view of one session (plan section 3.2): header facts, fish/treasure/junk bar
 * and the catch list aggregated by category with real item icons instead of plain text.
 */
public class SessionDetailScreen extends Screen {

    private static final int PADDING = 6;
    private static final int BAR_HEIGHT = 12;
    private static final int ROW_HEIGHT = 20;

    private final int sessionId;
    private final Screen parent;

    public SessionDetailScreen(int sessionId, Screen parent) {
        super(Component.translatable("fishingstats.detail.title"));
        this.sessionId = sessionId;
        this.parent = parent;
    }

    private FishingSession session() {
        for (FishingSession s : FishingDataStore.getInstance().getSessions()) {
            if (s.id == sessionId) return s;
        }
        return null;
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        Font font = this.font;
        int lineHeight = font.lineHeight + 4;
        graphics.fill(0, 0, width, height, 0xCC000000);

        FishingSession session = session();
        if (session == null) {
            graphics.text(font, I18n.get("fishingstats.detail.gone"), PADDING, PADDING, 0xFFFF5555, true);
            return;
        }

        // === Header ===
        String title = I18n.get("fishingstats.detail.header", StatsFormat.dateTime(session.startTime));
        graphics.text(font, title, (width - font.width(title)) / 2, PADDING, 0xFFFFD700, true);
        String backHint = I18n.get("fishingstats.detail.back");
        graphics.text(font, backHint, width - font.width(backHint) - PADDING, PADDING, 0xFFAAAAAA, true);

        int y = PADDING + lineHeight + 4;
        String timeRange = session.isActive()
                ? I18n.get("fishingstats.detail.running", StatsFormat.time(session.startTime), session.formattedDuration())
                : I18n.get("fishingstats.detail.timerange",
                        StatsFormat.time(session.startTime), StatsFormat.time(session.endTime), session.formattedDuration());
        graphics.text(font, timeRange, PADDING, y, 0xFFFFFFFF, true);
        y += lineHeight;

        String where = I18n.get("fishingstats.detail.where",
                StatsFormat.prettyId(session.biomePrimary.isEmpty() ? "?" : session.biomePrimary),
                StatsFormat.prettyId(session.dimension));
        graphics.text(font, where, PADDING, y, 0xFFAAAAAA, true);
        y += lineHeight;

        String casts = I18n.get("fishingstats.detail.casts", session.totalCasts, session.catches.size());
        graphics.text(font, casts, PADDING, y, 0xFFAAAAAA, true);
        y += lineHeight + 4;

        // === Stacked rarity bar: fish / treasure / junk ===
        y = renderRarityBar(graphics, font, session, y, lineHeight);
        y += 6;

        // === Aggregated catch list with item icons ===
        graphics.text(font, I18n.get("fishingstats.detail.catches"), PADDING, y, 0xFFFFD700, true);
        y += lineHeight;

        for (DisplayRow row : buildRows(session)) {
            if (y + ROW_HEIGHT > height) break;

            boolean hovered = mouseY >= y && mouseY < y + ROW_HEIGHT && mouseX < width / 2;
            if (hovered) {
                graphics.fill(0, y, width / 2, y + ROW_HEIGHT, 0x22FFFFFF);
                if (row.tooltipCategory != null) {
                    graphics.setComponentTooltipForNextFrame(font,
                            breakdownTooltip(session, row.tooltipCategory), mouseX, mouseY);
                }
            }

            graphics.item(row.icon, PADDING, y + 2);
            graphics.text(font, row.label, PADDING + 22, y + 6, 0xFFFFFFFF, true);
            String amount = "×" + row.count;
            graphics.text(font, amount, width / 2 - font.width(amount) - PADDING, y + 6, 0xFFFFD700, true);
            y += ROW_HEIGHT;
        }

        if (session.catches.isEmpty()) {
            graphics.text(font, I18n.get("fishingstats.detail.no_catches"), PADDING, y, 0xFFAAAAAA, true);
        }
    }

    private int renderRarityBar(GuiGraphicsExtractor graphics, Font font, FishingSession session,
                                int y, int lineHeight) {
        int total = session.catches.size();
        int barWidth = width - PADDING * 2;

        int fish = session.countByRarity("fish");
        int treasure = session.countByRarity("treasure");
        int junk = total - fish - treasure;

        graphics.fill(PADDING, y, PADDING + barWidth, y + BAR_HEIGHT, 0xFF333333);
        if (total > 0) {
            int fishW = Math.round(barWidth * (float) fish / total);
            int treasureW = Math.round(barWidth * (float) treasure / total);
            int x = PADDING;
            graphics.fill(x, y, x + fishW, y + BAR_HEIGHT, 0xFF55AAFF);
            x += fishW;
            graphics.fill(x, y, x + treasureW, y + BAR_HEIGHT, 0xFFFFD700);
            x += treasureW;
            graphics.fill(x, y, PADDING + barWidth, y + BAR_HEIGHT, 0xFF888888);
        }
        y += BAR_HEIGHT + 3;

        String legend = I18n.get("fishingstats.detail.legend",
                percent(fish, total), percent(treasure, total), percent(junk, total));
        graphics.text(font, legend, PADDING, y, 0xFFAAAAAA, true);
        return y + lineHeight;
    }

    private static int percent(int part, int total) {
        return total == 0 ? 0 : Math.round(100f * part / total);
    }

    /** One row in the catch list; tooltipCategory is null for rows that are already a single item. */
    private record DisplayRow(ItemStack icon, String label, int count, CatchCategory tooltipCategory) {}

    /**
     * Fish are listed per species (Cod, Salmon, ...) - only four variants exist, so the
     * aggregation argument from the plan (made for enchanted books) does not apply to them.
     * All other categories stay aggregated with a breakdown tooltip.
     */
    private List<DisplayRow> buildRows(FishingSession session) {
        Map<String, Integer> fishCounts = new LinkedHashMap<>();
        Map<CatchCategory, Integer> categoryCounts = new LinkedHashMap<>();
        for (CatchRecord c : session.catches) {
            CatchCategory category = c.categoryEnum();
            if (category == CatchCategory.FISH) {
                fishCounts.merge(c.itemId, 1, Integer::sum);
            } else {
                categoryCounts.merge(category, 1, Integer::sum);
            }
        }

        List<DisplayRow> rows = new ArrayList<>();
        fishCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(entry -> {
                    ItemStack icon = new ItemStack(CatchCategory.itemById(entry.getKey()));
                    rows.add(new DisplayRow(icon, icon.getItemName().getString(), entry.getValue(), null));
                });
        for (CatchCategory category : CatchCategory.values()) {
            int count = categoryCounts.getOrDefault(category, 0);
            if (count == 0) continue;
            rows.add(new DisplayRow(category.icon(), I18n.get(category.translationKey()), count, category));
        }
        return rows;
    }

    /** Per-item breakdown for the hovered category; books additionally list their enchantments. */
    private List<Component> breakdownTooltip(FishingSession session, CatchCategory category) {
        Map<String, Integer> itemCounts = new LinkedHashMap<>();
        List<String> bookDetails = new ArrayList<>();
        for (CatchRecord c : session.catches) {
            if (c.categoryEnum() != category) continue;
            itemCounts.merge(c.itemId, 1, Integer::sum);
            if (category == CatchCategory.BOOKS && c.detail != null && bookDetails.size() < 10) {
                bookDetails.add(c.detail);
            }
        }
        List<Component> lines = new ArrayList<>();
        itemCounts.forEach((itemId, count) ->
                lines.add(Component.literal(StatsFormat.prettyId(itemId) + " ×" + count)));
        for (String detail : bookDetails) {
            lines.add(Component.literal("  " + detail).withStyle(style -> style.withColor(0xAAAAAA)));
        }
        return lines;
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            Minecraft.getInstance().setScreen(parent);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
