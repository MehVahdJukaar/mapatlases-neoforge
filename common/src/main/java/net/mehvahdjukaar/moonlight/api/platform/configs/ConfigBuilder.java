package net.mehvahdjukaar.moonlight.api.platform.configs;

import java.util.function.Supplier;

public final class ConfigBuilder {

    public static ConfigBuilder create(String modId, ConfigType type) {
        return new ConfigBuilder();
    }

    public ConfigBuilder push(String name) {
        return this;
    }

    public ConfigBuilder pop() {
        return this;
    }

    public ConfigBuilder comment(String comment) {
        return this;
    }

    public void setSynced() {
    }

    public ConfigSpec buildAndRegister() {
        return new ConfigSpec();
    }

    public Supplier<Boolean> define(String name, boolean defaultValue) {
        return () -> defaultValue;
    }

    public Supplier<Integer> define(String name, int defaultValue, int min, int max) {
        return () -> defaultValue;
    }

    public Supplier<Double> define(String name, double defaultValue, double min, double max) {
        return () -> defaultValue;
    }

    public Supplier<Double> define(String name, float defaultValue, float min, float max) {
        return () -> (double) defaultValue;
    }

    public Supplier<String> define(String name, String defaultValue) {
        return () -> defaultValue;
    }

    public <T extends Enum<T>> Supplier<T> define(String name, T defaultValue) {
        return () -> defaultValue;
    }
}
