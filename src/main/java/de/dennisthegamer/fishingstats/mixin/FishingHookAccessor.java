package de.dennisthegamer.fishingstats.mixin;

import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the bite state of the bobber. The field is fed on the client through the
 * synced DATA_BITING entity data, so it is valid on servers too.
 */
@Mixin(FishingHook.class)
public interface FishingHookAccessor {

    @Accessor("biting")
    boolean fishingStats$isBiting();
}
