package net.mehvahdjukaar.moonlight.api.platform.configs;

import java.nio.file.Path;

public final class ConfigSpec {

    private final String modId;
    private final ConfigType type;
    private final Path path;
    private final boolean synced;

    public ConfigSpec(String modId, ConfigType type, Path path, boolean synced) {
        this.modId = modId;
        this.type = type;
        this.path = path;
        this.synced = synced;
    }

    public String modId() {
        return modId;
    }

    public ConfigType type() {
        return type;
    }

    public Path path() {
        return path;
    }

    public boolean synced() {
        return synced;
    }
}
