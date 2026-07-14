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
    public int mending;
    /** Anvil custom name of the rod, null when unnamed. */
    public String name;

    public RodEnchantments() {
    }

    public RodEnchantments(int luckOfTheSea, int lure, int unbreaking, int mending, String name) {
        this.luckOfTheSea = luckOfTheSea;
        this.lure = lure;
        this.unbreaking = unbreaking;
        this.mending = mending;
        this.name = name;
    }

    /** Grouping key for per-rod statistics, e.g. "Opas Angel (LotS 3 / Lure 3 / Mending / Unb 3)". */
    public String comboLabel() {
        StringBuilder sb = new StringBuilder();
        if (luckOfTheSea > 0) sb.append("LotS ").append(luckOfTheSea);
        if (lure > 0) {
            if (sb.length() > 0) sb.append(" / ");
            sb.append("Lure ").append(lure);
        }
        if (mending > 0) {
            if (sb.length() > 0) sb.append(" / ");
            sb.append("Mending");
        }
        if (unbreaking > 0) {
            if (sb.length() > 0) sb.append(" / ");
            sb.append("Unb ").append(unbreaking);
        }
        boolean named = name != null && !name.isBlank();
        if (sb.length() == 0) return named ? name : "-";
        return named ? name + " (" + sb + ")" : sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RodEnchantments other
                && other.luckOfTheSea == luckOfTheSea
                && other.lure == lure
                && other.unbreaking == unbreaking
                && other.mending == mending
                && Objects.equals(other.name, name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(luckOfTheSea, lure, unbreaking, mending, name);
    }
}
