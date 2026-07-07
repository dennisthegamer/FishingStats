package de.dennisthegamer.fishingstats.data;

import java.util.Objects;

/**
 * Snapshot of the rod's fishing-relevant enchantments at catch time. Kept as its own
 * object (mirrors the separate table in the plan) so more enchantments can be added
 * without touching the catch record itself.
 */
public class RodEnchantments {

    public int luckOfTheSea;
    public int lure;
    public int unbreaking;

    public RodEnchantments() {
    }

    public RodEnchantments(int luckOfTheSea, int lure, int unbreaking) {
        this.luckOfTheSea = luckOfTheSea;
        this.lure = lure;
        this.unbreaking = unbreaking;
    }

    /** Grouping key for per-enchant-combination statistics, e.g. "LotS 3 / Lure 2". */
    public String comboLabel() {
        if (luckOfTheSea == 0 && lure == 0) return "-";
        StringBuilder sb = new StringBuilder();
        if (luckOfTheSea > 0) sb.append("LotS ").append(luckOfTheSea);
        if (lure > 0) {
            if (sb.length() > 0) sb.append(" / ");
            sb.append("Lure ").append(lure);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RodEnchantments other
                && other.luckOfTheSea == luckOfTheSea
                && other.lure == lure
                && other.unbreaking == unbreaking;
    }

    @Override
    public int hashCode() {
        return Objects.hash(luckOfTheSea, lure, unbreaking);
    }
}
