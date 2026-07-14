package de.dennisthegamer.fishingstats.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes {@code ItemEntity.age} (private) so the tracker can ignore old floor loot. */
@Mixin(ItemEntity.class)
public interface ItemEntityAccessor {

    @Accessor("age")
    int fishingStats$getAge();
}
