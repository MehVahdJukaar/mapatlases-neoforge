package net.mehvahdjukaar.moonlight.api.platform.configs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import net.fabricmc.loader.api.FabricLoader;
import pepjebs.mapatlases.MapAtlasesMod;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ConfigBuilder {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final String modId;
    private final ConfigType type;
    private final Deque<String> sectionStack = new ArrayDeque<>();
    private final List<Entry<?>> entries = new ArrayList<>();
    private boolean synced;

    private ConfigBuilder(String modId, ConfigType type) {
        this.modId = modId;
        this.type = type;
    }

    public static ConfigBuilder create(String modId, ConfigType type) {
        return new ConfigBuilder(modId, type);
    }

    public ConfigBuilder comment(String... comments) {
        return this;
    }

    public ConfigBuilder push(String section) {
        sectionStack.addLast(section);
        return this;
    }

    public ConfigBuilder pop() {
        if (!sectionStack.isEmpty()) {
            sectionStack.removeLast();
        }
        return this;
    }

    public void setSynced() {
        this.synced = true;
    }

    public Supplier<Boolean> define(String name, boolean defaultValue) {
        return addEntry(name, defaultValue, this::readBoolean, JsonPrimitive::new);
    }

    public Supplier<String> define(String name, String defaultValue) {
        return addEntry(name, defaultValue, this::readString, JsonPrimitive::new);
    }

    public <T extends Enum<T>> Supplier<T> define(String name, T defaultValue) {
        Class<T> enumClass = defaultValue.getDeclaringClass();
        return addEntry(name, defaultValue, element -> readEnum(element, enumClass, defaultValue),
                value -> new JsonPrimitive(value.name()));
    }

    public Supplier<Integer> define(String name, int defaultValue, int minValue, int maxValue) {
        return addEntry(name, defaultValue, element -> clampInt(element, minValue, maxValue), JsonPrimitive::new);
    }

    public Supplier<Double> define(String name, int defaultValue, double minValue, double maxValue) {
        return define(name, (double) defaultValue, minValue, maxValue);
    }

    public Supplier<Double> define(String name, float defaultValue, double minValue, double maxValue) {
        return define(name, (double) defaultValue, minValue, maxValue);
    }

    public Supplier<Double> define(String name, double defaultValue, double minValue, double maxValue) {
        return addEntry(name, defaultValue, element -> clampDouble(element, minValue, maxValue), JsonPrimitive::new);
    }

    public ConfigSpec buildAndRegister() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path configPath = configDir.resolve(modId + "-" + type.name().toLowerCase(Locale.ROOT) + ".json");
        JsonObject root = new JsonObject();
        boolean createdFile = false;

        try {
            Files.createDirectories(configDir);
            if (Files.exists(configPath)) {
                root = readConfig(configPath);
            } else {
                root = createDefaultRoot();
                writeConfig(configPath, root);
                createdFile = true;
            }
        } catch (IOException e) {
            MapAtlasesMod.LOGGER.error("Failed to prepare config file {}", configPath, e);
        }

        for (Entry<?> entry : entries) {
            entry.load(root);
        }

        if (createdFile) {
            MapAtlasesMod.LOGGER.info("Created {} config at {}", type.name().toLowerCase(Locale.ROOT), configPath.toAbsolutePath());
        } else {
            MapAtlasesMod.LOGGER.info("Loaded {} config from {}", type.name().toLowerCase(Locale.ROOT), configPath.toAbsolutePath());
        }
        return new ConfigSpec(modId, type, configPath.toAbsolutePath(), synced);
    }

    private JsonObject createDefaultRoot() {
        JsonObject root = new JsonObject();
        for (Entry<?> entry : entries) {
            entry.writeDefault(root);
        }
        return root;
    }

    private JsonObject readConfig(Path configPath) {
        try (BufferedReader reader = Files.newBufferedReader(configPath)) {
            JsonReader jsonReader = new JsonReader(reader);
            jsonReader.setLenient(true);
            JsonElement parsed = JsonParser.parseReader(jsonReader);
            if (parsed != null && parsed.isJsonObject()) {
                return parsed.getAsJsonObject();
            }
            MapAtlasesMod.LOGGER.warn("Config file {} is not a JSON object; using defaults for missing values", configPath.toAbsolutePath());
        } catch (Exception e) {
            MapAtlasesMod.LOGGER.error("Failed to read config file {}; using defaults for missing values",
                    configPath.toAbsolutePath(), e);
        }
        return new JsonObject();
    }

    private void writeConfig(Path configPath, JsonObject root) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(root, writer);
        }
    }

    private <T> Supplier<T> addEntry(
            String name,
            T defaultValue,
            Function<JsonElement, T> parser,
            Function<T, JsonElement> serializer
    ) {
        MutableValue<T> value = new MutableValue<>(defaultValue);
        entries.add(new Entry<>(new ArrayList<>(sectionStack), name, defaultValue, value, parser, serializer));
        return value;
    }

    private Boolean readBoolean(JsonElement element) {
        if (!element.isJsonPrimitive()) {
            throw new IllegalArgumentException("Expected boolean primitive");
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        if (primitive.isString()) {
            return Boolean.parseBoolean(primitive.getAsString());
        }
        throw new IllegalArgumentException("Expected boolean value");
    }

    private String readString(JsonElement element) {
        if (!element.isJsonPrimitive()) {
            throw new IllegalArgumentException("Expected string primitive");
        }
        return element.getAsString();
    }

    private <T extends Enum<T>> T readEnum(JsonElement element, Class<T> enumClass, T defaultValue) {
        if (!element.isJsonPrimitive()) {
            throw new IllegalArgumentException("Expected enum string");
        }
        String value = element.getAsString();
        for (T constant : enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(value)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("Invalid enum value " + value + " for " + enumClass.getSimpleName());
    }

    private Integer clampInt(JsonElement element, int minValue, int maxValue) {
        if (!element.isJsonPrimitive()) {
            throw new IllegalArgumentException("Expected numeric primitive");
        }
        int value = element.getAsInt();
        return Math.max(minValue, Math.min(maxValue, value));
    }

    private Double clampDouble(JsonElement element, double minValue, double maxValue) {
        if (!element.isJsonPrimitive()) {
            throw new IllegalArgumentException("Expected numeric primitive");
        }
        double value = element.getAsDouble();
        return Math.max(minValue, Math.min(maxValue, value));
    }

    private static final class MutableValue<T> implements Supplier<T> {

        private T value;

        private MutableValue(T value) {
            this.value = value;
        }

        @Override
        public T get() {
            return value;
        }
    }

    private static final class Entry<T> {

        private final List<String> sections;
        private final String name;
        private final T defaultValue;
        private final MutableValue<T> holder;
        private final Function<JsonElement, T> parser;
        private final Function<T, JsonElement> serializer;

        private Entry(
                List<String> sections,
                String name,
                T defaultValue,
                MutableValue<T> holder,
                Function<JsonElement, T> parser,
                Function<T, JsonElement> serializer
        ) {
            this.sections = sections;
            this.name = name;
            this.defaultValue = defaultValue;
            this.holder = holder;
            this.parser = parser;
            this.serializer = serializer;
        }

        private void load(JsonObject root) {
            JsonObject sectionObject = findSection(root);
            if (sectionObject == null || !sectionObject.has(name)) {
                holder.value = defaultValue;
                return;
            }
            try {
                holder.value = parser.apply(sectionObject.get(name));
            } catch (Exception e) {
                holder.value = defaultValue;
                MapAtlasesMod.LOGGER.warn("Invalid config value for {}.{}; using default {}",
                        String.join(".", sections), name, defaultValue);
            }
        }

        private void writeDefault(JsonObject root) {
            JsonObject sectionObject = ensureSection(root);
            sectionObject.add(name, serializer.apply(defaultValue));
        }

        private JsonObject findSection(JsonObject root) {
            JsonObject current = root;
            for (String section : sections) {
                if (!current.has(section) || !current.get(section).isJsonObject()) {
                    return null;
                }
                current = current.getAsJsonObject(section);
            }
            return current;
        }

        private JsonObject ensureSection(JsonObject root) {
            JsonObject current = root;
            for (String section : sections) {
                if (!current.has(section) || !current.get(section).isJsonObject()) {
                    JsonObject next = new JsonObject();
                    current.add(section, next);
                    current = next;
                } else {
                    current = current.getAsJsonObject(section);
                }
            }
            return current;
        }
    }
}
