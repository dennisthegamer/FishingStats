package de.dennisthegamer.fishingstats.platform;

import java.nio.file.Path;

/**
 * Loader abstraction: everything the shared code needs from Fabric Loader / NeoForge.
 * One implementation per loader module, registered via {@code META-INF/services}.
 */
public interface Platform {

    /** The loader's config directory (usually {@code .minecraft/config}). */
    Path getConfigDir();

    /** Friendly version string of the running Minecraft instance (e.g. "26.1.2"). */
    String getMinecraftVersion();
}
