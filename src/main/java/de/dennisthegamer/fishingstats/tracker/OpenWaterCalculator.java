package de.dennisthegamer.fishingstats.tracker;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Client-side replica of the vanilla open-water check (FishingBobberEntity.isOpenOrWaterAround).
 * The server does not sync its result, but the blocks around the bobber are synced,
 * so the same 5x5 column scan can be evaluated locally.
 */
public final class OpenWaterCalculator {

    private enum ZoneType { ABOVE_WATER, INSIDE_WATER, INVALID }

    private OpenWaterCalculator() {}

    public static boolean isOpenWater(World level, BlockPos hookPos) {
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
    private static ZoneType zoneTypeForLayer(World level, BlockPos center, int yOffset) {
        ZoneType layerType = null;
        BlockPos.Mutable pos = new BlockPos.Mutable();
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

    private static ZoneType zoneTypeForBlock(World level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.isOf(Blocks.LILY_PAD)) {
            return ZoneType.ABOVE_WATER;
        }
        FluidState fluid = state.getFluidState();
        boolean stillWater = fluid.isIn(FluidTags.WATER) && fluid.isStill()
                && state.getCollisionShape(level, pos).isEmpty();
        return stillWater ? ZoneType.INSIDE_WATER : ZoneType.INVALID;
    }
}
