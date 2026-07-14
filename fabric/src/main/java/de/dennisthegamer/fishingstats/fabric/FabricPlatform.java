package de.dennisthegamer.fishingstats.fabric;

import de.dennisthegamer.fishingstats.platform.Platform;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.nio.file.Path;

public final class FabricPlatform implements Platform {

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    // 1.21.6-1.21.8 has no ResourceLocation rename in range, so direct calls are safe.

    @Override
    public String itemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    @Override
    public Item itemFromId(String id) {
        return BuiltInRegistries.ITEM.getValue(ResourceLocation.parse(id));
    }

    @Override
    public String keyId(ResourceKey<?> key) {
        return key.location().toString();
    }
}
