package net.mehvahdjukaar.moonlight.api.platform;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class PlatHelper {

    private PlatHelper() {
    }

    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public static PhysicalSide getPhysicalSide() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ? PhysicalSide.CLIENT : PhysicalSide.SERVER;
    }

    public static Platform getPlatform() {
        return Platform.FABRIC;
    }

    public static boolean isDev() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    public static Path getGamePath() {
        return FabricLoader.getInstance().getGameDir();
    }

    public enum PhysicalSide {
        CLIENT,
        SERVER;

        public boolean isClient() {
            return this == CLIENT;
        }
    }

    public enum Platform {
        FABRIC;

        public boolean isFabric() {
            return true;
        }
    }
}
