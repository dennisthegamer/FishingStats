package de.dennisthegamer.fishingstats.tracker;

import de.dennisthegamer.fishingstats.config.FishingStatsConfig;
import de.dennisthegamer.fishingstats.data.CatchCategory;
import de.dennisthegamer.fishingstats.data.CatchRecord;
import de.dennisthegamer.fishingstats.data.RodEnchantments;
import de.dennisthegamer.fishingstats.mixin.FishingHookAccessor;
import de.dennisthegamer.fishingstats.render.FishingStatsHud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

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
    private Vec3d lastHookPos;
    private boolean biteOpenWater;
    private RodEnchantments rodSnapshot = new RodEnchantments();

    // Pending loot after a retrieve-while-biting
    private long expectingUntilMs;
    private Vec3d expectedPos;
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

    public void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        World level = client.world;
        if (player == null || level == null) {
            reset();
            return;
        }

        long now = System.currentTimeMillis();
        FishingBobberEntity hook = player.fishHook;

        if (hook != null && hook.getId() != hookId) {
            onCast(hook, player, level, now);
        }

        if (hook != null) {
            lastHookPos = hook.getEntityPos();
            boolean biting = ((FishingHookAccessor) hook).fishingStats$isBiting();
            if (biting && !wasBiting) {
                biteTimeMs = now;
                biteOpenWater = OpenWaterCalculator.isOpenWater(level, hook.getBlockPos());
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

    private void onCast(FishingBobberEntity hook, ClientPlayerEntity player, World level, long now) {
        hookId = hook.getId();
        wasBiting = false;
        castTimeMs = now;
        biteTimeMs = -1;
        biteOpenWater = false;
        lastHookPos = hook.getEntityPos();
        rodSnapshot = readRodEnchantments(player);
        SessionManager.getInstance().onCast(now, dimensionId(level));
    }

    private void onHookGone(World level, long now) {
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
                    : OpenWaterCalculator.isOpenWater(level, BlockPos.ofFloored(lastHookPos));
            expectedRod = rodSnapshot;
            candidateItemId = -1;
        }
        hookId = -1;
        wasBiting = false;
    }

    private void watchForLoot(World level, long now) {
        if (now > expectingUntilMs) {
            expectingUntilMs = 0;
            candidateItemId = -1;
            return;
        }

        if (candidateItemId == -1) {
            Box box = new Box(
                    expectedPos.x - LOOT_RADIUS, expectedPos.y - LOOT_RADIUS, expectedPos.z - LOOT_RADIUS,
                    expectedPos.x + LOOT_RADIUS, expectedPos.y + LOOT_RADIUS, expectedPos.z + LOOT_RADIUS);
            for (ItemEntity item : level.getEntitiesByClass(ItemEntity.class, box, item -> true)) {
                if (item.age <= MAX_ITEM_AGE_TICKS) {
                    candidateItemId = item.getId();
                    break;
                }
            }
        }

        if (candidateItemId != -1) {
            Entity entity = level.getEntityById(candidateItemId);
            if (entity instanceof ItemEntity item && !item.getStack().isEmpty()) {
                recordCatch(level, item.getStack(), now);
                expectingUntilMs = 0;
                candidateItemId = -1;
            } else if (entity == null) {
                // candidate despawned before its stack data arrived - keep looking
                candidateItemId = -1;
            }
        }
    }

    private void recordCatch(World level, ItemStack stack, long now) {
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
        BlockPos pos = BlockPos.ofFloored(expectedPos.x, expectedPos.y, expectedPos.z);
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
        if (!stack.isOf(Items.ENCHANTED_BOOK)) return null;
        // For enchanted books this returns the stored enchantments component
        ItemEnchantmentsComponent stored = EnchantmentHelper.getEnchantments(stack);
        if (stored.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        stored.getEnchantmentEntries().forEach(entry -> {
            if (sb.length() > 0) sb.append(", ");
            entry.getKey().getKey().ifPresent(key -> sb.append(key.getValue()));
            sb.append(' ').append(entry.getIntValue());
        });
        return sb.toString();
    }

    private static RodEnchantments readRodEnchantments(ClientPlayerEntity player) {
        ItemStack rod = player.getMainHandStack();
        if (!rod.isOf(Items.FISHING_ROD)) {
            rod = player.getOffHandStack();
        }
        if (!rod.isOf(Items.FISHING_ROD)) return new RodEnchantments();
        try {
            ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(rod);
            int luck = 0, lure = 0, unbreaking = 0, mending = 0;
            for (var entry : enchants.getEnchantmentEntries()) {
                var key = entry.getKey();
                if (key.matchesKey(Enchantments.LUCK_OF_THE_SEA)) luck = entry.getIntValue();
                else if (key.matchesKey(Enchantments.LURE)) lure = entry.getIntValue();
                else if (key.matchesKey(Enchantments.UNBREAKING)) unbreaking = entry.getIntValue();
                else if (key.matchesKey(Enchantments.MENDING)) mending = entry.getIntValue();
            }
            String name = rod.contains(DataComponentTypes.CUSTOM_NAME)
                    ? rod.getName().getString() : null;
            return new RodEnchantments(luck, lure, unbreaking, mending, name);
        } catch (Exception e) {
            return new RodEnchantments();
        }
    }

    private static String dimensionId(World level) {
        return level.getRegistryKey().getValue().getPath();
    }

    private static String biomeId(World level, BlockPos pos) {
        return level.getBiome(pos).getKey()
                .map(key -> key.getValue().toString())
                .orElse("unknown");
    }
}
