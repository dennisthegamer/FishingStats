package de.dennisthegamer.fishingstats.mixin;

import net.minecraft.entity.projectile.FishingBobberEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the bite state of the bobber. The field is fed on the client through the
 * synced CAUGHT_FISH entity data, so it is valid on servers too.
 */
@Mixin(FishingBobberEntity.class)
public interface FishingHookAccessor {

    @Accessor("caughtFish")
    boolean fishingStats$isBiting();
}
