package de.dennisthegamer.fishingstats.platform;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import java.nio.file.Path;

/**
 * Loader abstraction: everything the shared code needs from Fabric Loader / NeoForge.
 * One implementation per loader module, registered via {@code META-INF/services}.
 *
 * <p>The id helpers exist because the resource-id class was renamed between 1.21.10
 * ({@code ResourceLocation}) and 1.21.11 ({@code Identifier}). A NeoForge jar bakes the
 * mojmap name it was compiled against, so touching {@code Registry.getKey} /
 * {@code ResourceKey.identifier()} directly in shared code crashes on the other patch.
 * Fabric remaps to intermediary (stable), NeoForge resolves the id reflectively.
 */
public interface Platform {

    /** The loader's config directory (usually {@code .minecraft/config}). */
    Path getConfigDir();

    /** {@code "namespace:path"} id of an item's registry entry. */
    String itemId(Item item);

    /** Item for a {@code "namespace:path"} id, or {@code null} if unknown. */
    Item itemFromId(String id);

    /** {@code "namespace:path"} id carried by a resource key (e.g. dimension, biome, enchantment). */
    String keyId(ResourceKey<?> key);
}
