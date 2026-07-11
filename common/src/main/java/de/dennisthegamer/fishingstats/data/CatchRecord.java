package de.dennisthegamer.fishingstats.data;

/** One caught item. itemId keeps the exact variant even though the UI aggregates by category. */
public class CatchRecord {

    public long timestamp;
    public String itemId;
    /** Extra detail for enchanted books: stored enchantment ids + levels. */
    public String detail;
    public String category;
    public String rarity;
    public String biome;
    public String dimension;
    public int posX;
    public int posZ;
    public boolean openWater;
    /** Milliseconds between cast landing in water and the bite; -1 if unknown. */
    public long timeToBiteMs = -1;
    public RodEnchantments rod = new RodEnchantments();

    public CatchCategory categoryEnum() {
        try {
            return CatchCategory.valueOf(category);
        } catch (IllegalArgumentException | NullPointerException e) {
            return CatchCategory.JUNK;
        }
    }
}
