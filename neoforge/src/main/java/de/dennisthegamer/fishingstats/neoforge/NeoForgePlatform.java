package de.dennisthegamer.fishingstats.neoforge;

import de.dennisthegamer.fishingstats.platform.Platform;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public final class NeoForgePlatform implements Platform {

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public String getMinecraftVersion() {
        return ModList.get().getModContainerById("minecraft")
                .map(mod -> mod.getModInfo().getVersion().toString())
                .orElse("unknown");
    }
}
