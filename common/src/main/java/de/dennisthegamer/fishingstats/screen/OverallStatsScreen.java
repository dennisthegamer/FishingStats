package de.dennisthegamer.fishingstats.screen;

import de.dennisthegamer.fishingstats.data.CatchCategory;
import de.dennisthegamer.fishingstats.data.CatchRecord;
import de.dennisthegamer.fishingstats.data.FishingDataStore;
import de.dennisthegamer.fishingstats.data.FishingSession;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Overall statistics across all sessions (plan section 3.3): catch rates per enchantment
 * combination, time-to-bite histogram, rarity log and current-vs-average comparison.
 */
public class OverallStatsScreen extends FishingStatsTabScreen {

    private static final int[] HISTOGRAM_BUCKETS_SECONDS = {5, 10, 15, 20, 25, 30};
    private static final int SECTION_GAP = 10;

    private int scrollOffset = 0;
    private int contentHeight = 0;

    public OverallStatsScreen() {
        super(Component.translatable("fishingstats.stats.title"), Tab.STATS);
    }

    private List<CatchRecord> allCatches() {
        List<CatchRecord> all = new ArrayList<>();
        for (FishingSession s : FishingDataStore.getInstance().getSessions()) {
            all.addAll(s.catches);
        }
        return all;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        Font font = this.font;
        int lineHeight = font.lineHeight + 4;

        renderHeader(graphics, "fishingstats.stats.title");
        renderSidebar(graphics, mouseX, mouseY);

        List<CatchRecord> catches = allCatches();
        int contentX = contentX() + PADDING;

        if (catches.isEmpty()) {
            String empty = I18n.get("fishingstats.stats.empty");
            graphics.drawString(font, empty, contentX + (width - contentX - font.width(empty)) / 2,
                    height / 2, MUTED_COLOR, true);
            return;
        }

        graphics.enableScissor(contentX() + 1, HEADER_HEIGHT, width, height);
        int y = HEADER_HEIGHT + PADDING - scrollOffset;

        y = renderEnchantTable(graphics, font, catches, contentX, y, lineHeight);
        y += SECTION_GAP;
        y = renderHistogram(graphics, font, catches, contentX, y, lineHeight);
        y += SECTION_GAP;
        y = renderComparison(graphics, font, contentX, y, lineHeight);
        y += SECTION_GAP;
        y = renderRareFinds(graphics, font, catches, contentX, y, lineHeight);

        contentHeight = y + scrollOffset - HEADER_HEIGHT;
        graphics.disableScissor();
    }

    // === Section 1: catch rates grouped by enchantment combination ===
    private int renderEnchantTable(GuiGraphics graphics, Font font, List<CatchRecord> catches,
                                   int x, int y, int lineHeight) {
        graphics.drawString(font, I18n.get("fishingstats.stats.by_enchants"), x, y, HEADER_COLOR, true);
        y += lineHeight;

        Map<String, int[]> byCombo = new LinkedHashMap<>(); // label -> [fish, treasure, junk]
        for (CatchRecord c : catches) {
            String label = c.rod != null ? c.rod.comboLabel() : "-";
            int[] counts = byCombo.computeIfAbsent(label, k -> new int[3]);
            switch (c.rarity == null ? "junk" : c.rarity) {
                case "fish" -> counts[0]++;
                case "treasure" -> counts[1]++;
                default -> counts[2]++;
            }
        }

        int labelWidth = 0;
        for (String label : byCombo.keySet()) {
            labelWidth = Math.max(labelWidth, font.width(label));
        }
        int ratesX = x + Math.max(110, labelWidth + 24);

        for (Map.Entry<String, int[]> entry : byCombo.entrySet()) {
            int[] counts = entry.getValue();
            int total = counts[0] + counts[1] + counts[2];
            graphics.drawString(font, entry.getKey(), x, y, TEXT_COLOR, true);
            String rates = I18n.get("fishingstats.stats.rates",
                    total, pct(counts[0], total), pct(counts[1], total), pct(counts[2], total));
            graphics.drawString(font, rates, ratesX, y, MUTED_COLOR, true);
            y += lineHeight;
        }
        return y;
    }

    // === Section 2: time-to-bite histogram ===
    private int renderHistogram(GuiGraphics graphics, Font font, List<CatchRecord> catches,
                                int x, int y, int lineHeight) {
        graphics.drawString(font, I18n.get("fishingstats.stats.time_to_bite"), x, y, HEADER_COLOR, true);
        y += lineHeight;

        int bucketCount = HISTOGRAM_BUCKETS_SECONDS.length + 1;
        int[] buckets = new int[bucketCount];
        long sum = 0;
        int known = 0;
        for (CatchRecord c : catches) {
            if (c.timeToBiteMs < 0) continue;
            known++;
            sum += c.timeToBiteMs;
            int bucket = bucketCount - 1;
            for (int i = 0; i < HISTOGRAM_BUCKETS_SECONDS.length; i++) {
                if (c.timeToBiteMs < HISTOGRAM_BUCKETS_SECONDS[i] * 1000L) {
                    bucket = i;
                    break;
                }
            }
            buckets[bucket]++;
        }

        if (known == 0) {
            graphics.drawString(font, I18n.get("fishingstats.stats.no_bite_data"), x, y, MUTED_COLOR, true);
            return y + lineHeight;
        }

        int max = 1;
        for (int b : buckets) max = Math.max(max, b);

        // Bars scale with the available width; headroom above them keeps the
        // count labels from running into the section title
        int available = width - x - PADDING;
        int barWidth = Math.max(20, Math.min(48, available / bucketCount - 6));
        int barMaxHeight = 60;
        int chartTop = y + font.lineHeight + 2;
        int chartBottom = chartTop + barMaxHeight;
        for (int i = 0; i < bucketCount; i++) {
            int barX = x + i * (barWidth + 6);
            int barH = Math.round(barMaxHeight * (float) buckets[i] / max);
            graphics.fill(barX, chartBottom - barH, barX + barWidth, chartBottom, FISH_COLOR);
            if (buckets[i] > 0) {
                String count = String.valueOf(buckets[i]);
                graphics.drawString(font, count, barX + (barWidth - font.width(count)) / 2,
                        chartBottom - barH - font.lineHeight - 1, TEXT_COLOR, true);
            }
            String label = i < HISTOGRAM_BUCKETS_SECONDS.length
                    ? "<" + HISTOGRAM_BUCKETS_SECONDS[i]
                    : HISTOGRAM_BUCKETS_SECONDS[HISTOGRAM_BUCKETS_SECONDS.length - 1] + "+";
            graphics.drawString(font, label, barX + (barWidth - font.width(label)) / 2,
                    chartBottom + 2, MUTED_COLOR, true);
        }
        y = chartBottom + lineHeight + 2;

        graphics.drawString(font, I18n.get("fishingstats.stats.avg_bite", StatsFormat.seconds(sum / known)),
                x, y, MUTED_COLOR, true);
        y += lineHeight;

        // Average per lure level - checks whether Lure actually shortens the wait
        long[] lureSum = new long[4];
        int[] lureCount = new int[4];
        for (CatchRecord c : catches) {
            if (c.timeToBiteMs < 0 || c.rod == null) continue;
            int lure = Math.max(0, Math.min(3, c.rod.lure));
            lureSum[lure] += c.timeToBiteMs;
            lureCount[lure]++;
        }
        StringBuilder lureLine = new StringBuilder();
        for (int i = 0; i <= 3; i++) {
            if (lureCount[i] == 0) continue;
            if (lureLine.length() > 0) lureLine.append("   ");
            lureLine.append(I18n.get("fishingstats.stats.lure_avg", i,
                    StatsFormat.seconds(lureSum[i] / lureCount[i])));
        }
        if (lureLine.length() > 0) {
            graphics.drawString(font, lureLine.toString(), x, y, MUTED_COLOR, true);
            y += lineHeight;
        }
        return y;
    }

    // === Section 3: latest session vs. average ===
    private int renderComparison(GuiGraphics graphics, Font font, int x, int y, int lineHeight) {
        graphics.drawString(font, I18n.get("fishingstats.stats.comparison"), x, y, HEADER_COLOR, true);
        y += lineHeight;

        List<FishingSession> sessions = FishingDataStore.getInstance().getSessionsNewestFirst();
        if (sessions.isEmpty()) return y;
        FishingSession latest = sessions.get(0);

        double totalHours = 0;
        int totalCatches = 0;
        int totalTreasure = 0;
        for (FishingSession s : sessions) {
            totalHours += s.durationMs() / 3_600_000.0;
            totalCatches += s.catches.size();
            totalTreasure += s.countByRarity("treasure");
        }

        double latestHours = latest.durationMs() / 3_600_000.0;
        String latestRate = latestHours > 0.001 ? String.format("%.1f", latest.catches.size() / latestHours) : "-";
        String avgRate = totalHours > 0.001 ? String.format("%.1f", totalCatches / totalHours) : "-";
        graphics.drawString(font, I18n.get("fishingstats.stats.catches_per_hour", latestRate, avgRate),
                x, y, TEXT_COLOR, true);
        y += lineHeight;

        int avgTreasurePct = totalCatches == 0 ? 0 : Math.round(100f * totalTreasure / totalCatches);
        graphics.drawString(font, I18n.get("fishingstats.stats.treasure_share",
                latest.treasurePercent(), avgTreasurePct), x, y, TEXT_COLOR, true);
        return y + lineHeight;
    }

    // === Section 4: rarity log - rare finds are never aggregated ===
    private int renderRareFinds(GuiGraphics graphics, Font font, List<CatchRecord> catches,
                                int x, int y, int lineHeight) {
        graphics.drawString(font, I18n.get("fishingstats.stats.rare_finds"), x, y, HEADER_COLOR, true);
        y += lineHeight;

        boolean any = false;
        for (int i = catches.size() - 1; i >= 0; i--) {
            CatchRecord c = catches.get(i);
            if (c.itemId == null || !CatchCategory.isRareFind(CatchCategory.itemById(c.itemId))) continue;
            any = true;

            graphics.renderItem(new ItemStack(CatchCategory.itemById(c.itemId)), x, y - 2);
            String line = I18n.get("fishingstats.stats.rare_entry",
                    StatsFormat.prettyId(c.itemId), StatsFormat.dateTime(c.timestamp), c.posX, c.posZ);
            graphics.drawString(font, line, x + 22, y + 2, TREASURE_COLOR, true);
            y += ROW_STEP;
        }
        if (!any) {
            graphics.drawString(font, I18n.get("fishingstats.stats.no_rare_finds"), x, y, MUTED_COLOR, true);
            y += lineHeight;
        }
        return y;
    }

    private static final int ROW_STEP = 20;

    private static int pct(int part, int total) {
        return total == 0 ? 0 : Math.round(100f * part / total);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (handleSidebarClick(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (handleSidebarScroll(mouseX, scrollY)) return true;
        int maxScroll = Math.max(0, contentHeight - (height - HEADER_HEIGHT) + PADDING * 2);
        scrollOffset = Math.max(0, Math.min(scrollOffset - (int) (scrollY * 12), maxScroll));
        return true;
    }
}
