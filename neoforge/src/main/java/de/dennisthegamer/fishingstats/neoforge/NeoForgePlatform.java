package de.dennisthegamer.fishingstats.neoforge;

import de.dennisthegamer.fishingstats.platform.Platform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public final class NeoForgePlatform implements Platform {

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    // 1.21.6-1.21.8 has no ResourceLocation rename in range, so direct calls are safe.

    @Override
    public String itemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private java.util.Map<ResourceLocation, Item> itemById;

    @Override
    public Item itemFromId(String id) {
        // Version-agnostic reverse lookup built from the stable getKey(); avoids
        // Registry.get()/getValue() which changed signature at 1.21.2.
        if (itemById == null) {
            java.util.Map<ResourceLocation, Item> m = new java.util.HashMap<>();
            for (Item item : BuiltInRegistries.ITEM) {
                m.put(BuiltInRegistries.ITEM.getKey(item), item);
            }
            itemById = m;
        }
        return itemById.get(ResourceLocation.parse(id));
    }

    @Override
    public String keyId(ResourceKey<?> key) {
        return key.location().toString();
    }
}
