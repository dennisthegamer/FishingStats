package de.dennisthegamer.fishingstats.fabric;

import de.dennisthegamer.fishingstats.platform.Platform;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class FabricPlatform implements Platform {

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public String getMinecraftVersion() {
        return FabricLoader.getInstance()
                .getModContainer("minecraft")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }
}
