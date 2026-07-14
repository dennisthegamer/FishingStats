package de.dennisthegamer.fishingstats.neoforge;

import de.dennisthegamer.fishingstats.platform.Platform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.neoforged.fml.loading.FMLPaths;

import java.lang.reflect.Method;
import java.nio.file.Path;

/**
 * NeoForge runs on the runtime version's mojmap names. The resource-id class was renamed
 * between 1.21.10 ({@code ResourceLocation}) and 1.21.11 ({@code Identifier}), and this one
 * jar must cover 1.21.9-1.21.11, so every id access is resolved reflectively. The method
 * names ({@code getKey}, {@code getValue}) stayed stable - only the id class changed.
 */
public final class NeoForgePlatform implements Platform {

    private static final Class<?> ID_CLASS = resolveIdClass();

    private static Class<?> resolveIdClass() {
        try {
            return Class.forName("net.minecraft.resources.Identifier"); // 1.21.11+
        } catch (ClassNotFoundException e) {
            try {
                return Class.forName("net.minecraft.resources.ResourceLocation"); // <=1.21.10
            } catch (ClassNotFoundException e2) {
                throw new IllegalStateException("No resource-id class found", e2);
            }
        }
    }

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public String itemId(Item item) {
        try {
            Object id = BuiltInRegistries.ITEM.getClass()
                    .getMethod("getKey", Object.class)
                    .invoke(BuiltInRegistries.ITEM, item);
            return String.valueOf(id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("itemId failed", e);
        }
    }

    @Override
    public Item itemFromId(String id) {
        try {
            Object rl = ID_CLASS.getMethod("parse", String.class).invoke(null, id);
            Object item = BuiltInRegistries.ITEM.getClass()
                    .getMethod("getValue", ID_CLASS)
                    .invoke(BuiltInRegistries.ITEM, rl);
            return (Item) item;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("itemFromId failed", e);
        }
    }

    @Override
    public String keyId(ResourceKey<?> key) {
        try {
            Method m;
            try {
                m = ResourceKey.class.getMethod("identifier"); // 1.21.11+
            } catch (NoSuchMethodException e) {
                m = ResourceKey.class.getMethod("location"); // <=1.21.10
            }
            return String.valueOf(m.invoke(key));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("keyId failed", e);
        }
    }
}
