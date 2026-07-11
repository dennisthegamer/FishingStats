package de.dennisthegamer.fishingstats.tracker;

import de.dennisthegamer.fishingstats.config.FishingStatsConfig;
import de.dennisthegamer.fishingstats.data.CatchCategory;
import de.dennisthegamer.fishingstats.data.CatchRecord;
import de.dennisthegamer.fishingstats.data.RodEnchantments;
import de.dennisthegamer.fishingstats.mixin.FishingHookAccessor;
import de.dennisthegamer.fishingstats.render.FishingStatsHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side catch detection. There is no loot-generation hook on the client (that code
 * runs on the server), so the tracker observes what the client can see: the bobber entity
 * owned by the local player (cast), its synced biting flag (bite), and the fresh ItemEntity
 * that flies from the bobber to the player after a retrieve while biting (catch).
 * Works in singleplayer and on servers alike.
 */
public class FishingTracker {

    private static final FishingTracker INSTANCE = new FishingTracker();

    /** How long after a retrieve we wait for the loot item entity to appear. */
    private static final long LOOT_WINDOW_MS = 3000;
    /** Loot item must spawn within this radius of the bobber's last position. */
    private static final double LOOT_RADIUS = 2.5;
    /** Loot item must be at most this many ticks old to count as freshly spawned. */
    private static final int MAX_ITEM_AGE_TICKS = 15;

    // Current cast state
    private int hookId = -1;
    private boolean wasBiting;
    private long castTimeMs;
    private long biteTimeMs = -1;
    private Vec3 lastHookPos;
    private boolean biteOpenWater;
    private RodEnchantments rodSnapshot = new RodEnchantments();

    // Pending loot after a retrieve-while-biting
    private long expectingUntilMs;
    private Vec3 expectedPos;
    private long expectedTimeToBite;
    private boolean expectedOpenWater;
    private RodEnchantments expectedRod;
    private int candidateItemId = -1;

    private FishingTracker() {}

    public static FishingTracker getInstance() {
        return INSTANCE;
    }

    /** Resets all per-world state, e.g. on world leave. */
    public void reset() {
        hookId = -1;
        wasBiting = false;
        expectingUntilMs = 0;
        candidateItemId = -1;
    }

    public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        Level level = client.level;
        if (player == null || level == null) {
            reset();
            return;
        }

        long now = System.currentTimeMillis();
        FishingHook hook = player.fishing;

        if (hook != null && hook.getId() != hookId) {
            onCast(hook, player, now);
        }

        if (hook != null) {
            lastHookPos = hook.position();
            boolean biting = ((FishingHookAccessor) hook).fishingStats$isBiting();
            if (biting && !wasBiting) {
                biteTimeMs = now;
                biteOpenWater = OpenWaterCalculator.isOpenWater(level, hook.blockPosition());
            }
            wasBiting = biting;
        } else if (hookId != -1) {
            onHookGone(level, now);
        }

        if (expectingUntilMs > 0) {
            watchForLoot(level, now);
        }

        SessionManager.getInstance().tick(now);
    }

    private void onCast(FishingHook hook, LocalPlayer player, long now) {
        hookId = hook.getId();
        wasBiting = false;
        castTimeMs = now;
        biteTimeMs = -1;
        biteOpenWater = false;
        lastHookPos = hook.position();
        rodSnapshot = readRodEnchantments(player);
        SessionManager.getInstance().onCast(now, dimensionId(player.level()));
    }

    private void onHookGone(Level level, long now) {
        // Watch for loot after EVERY retrieve, not only when a bite was observed: auto-fishing
        // mods reel in within milliseconds of the bite, so the synced biting flag can vanish
        // between two client ticks and is never sampled. A retrieve without a catch spawns no
        // item, so the fresh ItemEntity near the bobber is itself the reliable catch signal.
        if (lastHookPos != null) {
            expectingUntilMs = now + LOOT_WINDOW_MS;
            expectedPos = lastHookPos;
            expectedTimeToBite = biteTimeMs > 0 ? biteTimeMs - castTimeMs : -1;
            expectedOpenWater = wasBiting
                    ? biteOpenWater
                    : OpenWaterCalculator.isOpenWater(level, BlockPos.containing(lastHookPos));
            expectedRod = rodSnapshot;
            candidateItemId = -1;
        }
        hookId = -1;
        wasBiting = false;
    }

    private void watchForLoot(Level level, long now) {
        if (now > expectingUntilMs) {
            expectingUntilMs = 0;
            candidateItemId = -1;
            return;
        }

        if (candidateItemId == -1) {
            AABB box = new AABB(
                    expectedPos.x - LOOT_RADIUS, expectedPos.y - LOOT_RADIUS, expectedPos.z - LOOT_RADIUS,
                    expectedPos.x + LOOT_RADIUS, expectedPos.y + LOOT_RADIUS, expectedPos.z + LOOT_RADIUS);
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
                if (item.tickCount <= MAX_ITEM_AGE_TICKS) {
                    candidateItemId = item.getId();
                    break;
                }
            }
        }

        if (candidateItemId != -1) {
            Entity entity = level.getEntity(candidateItemId);
            if (entity instanceof ItemEntity item && !item.getItem().isEmpty()) {
                recordCatch(level, item.getItem(), now);
                expectingUntilMs = 0;
                candidateItemId = -1;
            } else if (entity == null) {
                // candidate despawned before its stack data arrived - keep looking
                candidateItemId = -1;
            }
        }
    }

    private void recordCatch(Level level, ItemStack stack, long now) {
        FishingStatsConfig config = FishingStatsConfig.getInstance();
        CatchCategory category = CatchCategory.classify(stack.getItem());
        String rarity = CatchCategory.rarityOf(stack.getItem());

        if (config.trackTreasureOnly && !"treasure".equals(rarity)) {
            FishingStatsHud.onCatch(stack.copy());
            return;
        }

        CatchRecord record = new CatchRecord();
        record.timestamp = now;
        record.itemId = CatchCategory.idOf(stack.getItem());
        record.detail = bookDetail(stack);
        record.category = category.name();
        record.rarity = rarity;
        BlockPos pos = BlockPos.containing(expectedPos.x, expectedPos.y, expectedPos.z);
        record.biome = biomeId(level, pos);
        record.dimension = dimensionId(level);
        record.posX = pos.getX();
        record.posZ = pos.getZ();
        record.openWater = expectedOpenWater;
        record.timeToBiteMs = expectedTimeToBite;
        record.rod = expectedRod != null ? expectedRod : new RodEnchantments();

        SessionManager.getInstance().onCatch(record);
        FishingStatsHud.onCatch(stack.copy());
    }

    /** "minecraft:mending 1, minecraft:unbreaking 3" for enchanted books, otherwise null. */
    private static String bookDetail(ItemStack stack) {
        if (!stack.is(Items.ENCHANTED_BOOK)) return null;
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored == null || stored.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        stored.entrySet().forEach(entry -> {
            if (sb.length() > 0) sb.append(", ");
            entry.getKey().unwrapKey().ifPresent(key -> sb.append(key.identifier()));
            sb.append(' ').append(entry.getIntValue());
        });
        return sb.toString();
    }

    private static RodEnchantments readRodEnchantments(LocalPlayer player) {
        ItemStack rod = player.getMainHandItem();
        if (!rod.is(Items.FISHING_ROD)) {
            rod = player.getOffhandItem();
        }
        if (!rod.is(Items.FISHING_ROD)) return new RodEnchantments();
        try {
            var registry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Holder<Enchantment> luck = registry.getOrThrow(Enchantments.LUCK_OF_THE_SEA);
            Holder<Enchantment> lure = registry.getOrThrow(Enchantments.LURE);
            Holder<Enchantment> unbreaking = registry.getOrThrow(Enchantments.UNBREAKING);
            Holder<Enchantment> mending = registry.getOrThrow(Enchantments.MENDING);
            var customName = rod.get(DataComponents.CUSTOM_NAME);
            return new RodEnchantments(
                    EnchantmentHelper.getItemEnchantmentLevel(luck, rod),
                    EnchantmentHelper.getItemEnchantmentLevel(lure, rod),
                    EnchantmentHelper.getItemEnchantmentLevel(unbreaking, rod),
                    EnchantmentHelper.getItemEnchantmentLevel(mending, rod),
                    customName != null ? customName.getString() : null);
        } catch (Exception e) {
            return new RodEnchantments();
        }
    }

    private static String dimensionId(Level level) {
        return level.dimension().identifier().getPath();
    }

    private static String biomeId(Level level, BlockPos pos) {
        return level.getBiome(pos).unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse("unknown");
    }
}
