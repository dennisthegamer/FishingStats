package de.dennisthegamer.fishingstats.tracker;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.tags.FluidTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Client-side replica of the vanilla open-water check (FishingHook.isOpenOrWaterAround).
 * The server does not sync its result, but the blocks around the bobber are synced,
 * so the same 5x5 column scan can be evaluated locally.
 */
public final class OpenWaterCalculator {

    private enum ZoneType { ABOVE_WATER, INSIDE_WATER, INVALID }

    private OpenWaterCalculator() {}

    public static boolean isOpenWater(Level level, BlockPos hookPos) {
        ZoneType previous = ZoneType.INVALID;
        for (int yOffset = -1; yOffset <= 2; yOffset++) {
            ZoneType current = zoneTypeForLayer(level, hookPos, yOffset);
            switch (current) {
                case INVALID -> {
                    return false;
                }
                // the lowest layer must be water, and water may never sit above air
                case ABOVE_WATER -> {
                    if (previous == ZoneType.INVALID) return false;
                }
                case INSIDE_WATER -> {
                    if (previous == ZoneType.ABOVE_WATER) return false;
                }
            }
            previous = current;
        }
        return true;
    }

    /** A 5x5 layer is valid only if every block in it has the same zone type. */
    private static ZoneType zoneTypeForLayer(Level level, BlockPos center, int yOffset) {
        ZoneType layerType = null;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                pos.set(center.getX() + x, center.getY() + yOffset, center.getZ() + z);
                ZoneType blockType = zoneTypeForBlock(level, pos);
                if (blockType == ZoneType.INVALID) return ZoneType.INVALID;
                if (layerType == null) {
                    layerType = blockType;
                } else if (blockType != layerType) {
                    return ZoneType.INVALID;
                }
            }
        }
        return layerType == null ? ZoneType.INVALID : layerType;
    }

    private static ZoneType zoneTypeForBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.is(Blocks.LILY_PAD)) {
            return ZoneType.ABOVE_WATER;
        }
        FluidState fluid = state.getFluidState();
        boolean stillWater = fluid.is(FluidTags.WATER) && fluid.isSource()
                && state.getCollisionShape(level, pos).isEmpty();
        return stillWater ? ZoneType.INSIDE_WATER : ZoneType.INVALID;
    }
}
