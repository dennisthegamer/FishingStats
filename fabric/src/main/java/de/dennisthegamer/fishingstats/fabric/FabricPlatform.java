package de.dennisthegamer.fishingstats.fabric;

import de.dennisthegamer.fishingstats.platform.Platform;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import java.nio.file.Path;

public final class FabricPlatform implements Platform {

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    // On Fabric these are remapped to intermediary names, which are stable across 1.21.9-1.21.11.

    @Override
    public String itemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    @Override
    public Item itemFromId(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }

    @Override
    public String keyId(ResourceKey<?> key) {
        return key.identifier().toString();
    }
}
