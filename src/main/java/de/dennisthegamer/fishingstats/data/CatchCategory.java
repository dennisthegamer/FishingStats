package de.dennisthegamer.fishingstats.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Set;

/**
 * Display category for the aggregated catch list (plan section 4.1). Catches are grouped
 * by category, not by exact item variant, so ten differently-enchanted books collapse
 * into one "Books" row while the exact item id stays in the stored record.
 */
public enum CatchCategory {
    FISH("fish"),
    BOOKS("books"),
    EQUIPMENT("equipment"),
    OTHER_TREASURE("other_treasure"),
    JUNK("junk");

    private final String key;

    CatchCategory(String key) {
        this.key = key;
    }

    /** Translation key for the row label, e.g. fishingstats.category.fish */
    public String translationKey() {
        return "fishingstats.category." + key;
    }

    private static final Set<Item> FISH_ITEMS = Set.of(
            Items.COD, Items.SALMON, Items.PUFFERFISH, Items.TROPICAL_FISH);

    private static final Set<Item> EQUIPMENT_ITEMS = Set.of(
            Items.FISHING_ROD, Items.BOW, Items.NAME_TAG, Items.SADDLE);

    private static final Set<Item> OTHER_TREASURE_ITEMS = Set.of(
            Items.NAUTILUS_SHELL, Items.LILY_PAD);

    /** Vanilla fishing loot table treasure entries (for the fish/treasure/junk rarity split). */
    private static final Set<Item> TREASURE_ITEMS = Set.of(
            Items.BOW, Items.ENCHANTED_BOOK, Items.FISHING_ROD,
            Items.NAME_TAG, Items.NAUTILUS_SHELL, Items.SADDLE);

    /** Rare individual finds that go to the rarity log un-aggregated (plan section 3.3). */
    private static final Set<Item> RARE_FINDS = Set.of(
            Items.NAUTILUS_SHELL, Items.NAME_TAG, Items.SADDLE);

    public static CatchCategory classify(Item item) {
        if (FISH_ITEMS.contains(item)) return FISH;
        if (item == Items.ENCHANTED_BOOK) return BOOKS;
        if (EQUIPMENT_ITEMS.contains(item)) return EQUIPMENT;
        if (OTHER_TREASURE_ITEMS.contains(item)) return OTHER_TREASURE;
        return JUNK;
    }

    /** fish / treasure / junk following the vanilla loot table split. */
    public static String rarityOf(Item item) {
        if (FISH_ITEMS.contains(item)) return "fish";
        if (TREASURE_ITEMS.contains(item)) return "treasure";
        return "junk";
    }

    public static boolean isRareFind(Item item) {
        return RARE_FINDS.contains(item);
    }

    /** Fixed icon per category for the aggregated list rows. */
    public ItemStack icon() {
        return new ItemStack(switch (this) {
            case FISH -> Items.COD;
            case BOOKS -> Items.ENCHANTED_BOOK;
            case EQUIPMENT -> Items.FISHING_ROD;
            case OTHER_TREASURE -> Items.NAUTILUS_SHELL;
            case JUNK -> Items.LEATHER_BOOTS;
        });
    }

    public static Item itemById(String itemId) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId));
    }

    public static String idOf(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }
}
