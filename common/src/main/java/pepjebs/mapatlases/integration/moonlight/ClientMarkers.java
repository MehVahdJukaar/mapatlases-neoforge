package pepjebs.mapatlases.integration.moonlight;

import net.minecraft.client.quickplay.QuickPlayLog;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import org.jetbrains.annotations.ApiStatus;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClientMarkers {
    private static final List<String> PIN_TEXTURES = List.of(
            "pin_red",
            "pin_blue",
            "pin_green",
            "pin_yellow",
            "pin_orange",
            "pin_apple",
            "pin_aqua",
            "pin_purple"
    );
    private static final Set<String> FOCUSED_PINS = new HashSet<>();
    private static final Map<Integer, Map<String, PinData>> PINS_BY_MAP = new HashMap<>();
    private static String lastFolderNameOrIP = null;
    private static QuickPlayLog.Type lastType = QuickPlayLog.Type.SINGLEPLAYER;
    private static Path currentPath = null;

    @ApiStatus.Internal
    public static void setWorldFolder(String pId, QuickPlayLog.Type type) {
        lastFolderNameOrIP = pId;
        lastType = type;
    }

    @ApiStatus.Internal
    public static void deleteAllMarkersData(String folderName) {
        try {
            Files.deleteIfExists(getFilePath(folderName, QuickPlayLog.Type.SINGLEPLAYER));
        } catch (Exception ignored) {
        }
    }

    @ApiStatus.Internal
    public static synchronized void loadClientMarkers(long seed, String levelName) {
        PINS_BY_MAP.clear();
        currentPath = getFilePath(lastFolderNameOrIP == null ? levelName : lastFolderNameOrIP, lastType);
        if (Files.exists(currentPath)) {
            try {
                for (String line : Files.readAllLines(currentPath, StandardCharsets.UTF_8)) {
                    PinData pin = PinData.deserialize(line);
                    if (pin != null) {
                        PINS_BY_MAP.computeIfAbsent(pin.mapId(), ignored -> new HashMap<>()).put(pin.id(), pin);
                    }
                }
            } catch (Exception ignored) {
                PINS_BY_MAP.clear();
            }
        }
        lastFolderNameOrIP = null;
        lastType = QuickPlayLog.Type.SINGLEPLAYER;
    }

    public static void saveClientMarkers() {
        Path path = currentPath;
        if (path == null) {
            return;
        }
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, PINS_BY_MAP.values().stream()
                    .flatMap(pins -> pins.values().stream())
                    .map(PinData::serialize)
                    .toList(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    public static synchronized void unloadWorld() {
        saveClientMarkers();
        FOCUSED_PINS.clear();
        PINS_BY_MAP.clear();
        currentPath = null;
    }

    public static Set<?> send(Integer integer, MapItemSavedData data) {
        return Set.of();
    }

    public static synchronized void addPin(MapDataHolder holder, ColumnPos pos, String text, int index) {
        String markerId = MapAtlasesConfig.pinMarkerId.get();
        if (markerId.isEmpty()) {
            return;
        }
        BlockPos blockPos = new BlockPos(pos.x(), 0, pos.z());
        Component name = text.isEmpty() ? null : Component.literal(text);
        String pinId = MoonlightCompat.getPinId(blockPos, name, index);
        PINS_BY_MAP.computeIfAbsent(holder.id, ignored -> new HashMap<>())
                .put(pinId, new PinData(holder.stringId, holder.id, blockPos, markerId, text, index, pinId));
        MoonlightCompat.addDecoration(
                holder.data,
                blockPos,
                Identifier.parse(markerId),
                name,
                index,
                pinId
        );
        MapAtlasesClient.markDecorationsDirty(holder.data);
        saveClientMarkers();
    }

    public static synchronized void addMissingPinsToMap(MapDataHolder holder, Set<String> existingIds) {
        Map<String, PinData> pins = PINS_BY_MAP.get(holder.id);
        if (pins == null || pins.isEmpty()) {
            return;
        }
        for (PinData pin : pins.values()) {
            if (existingIds.contains(pin.id())) {
                continue;
            }
            MoonlightCompat.addDecoration(
                    holder.data,
                    pin.pos(),
                    Identifier.parse(pin.markerId()),
                    pin.text().isEmpty() ? null : Component.literal(pin.text()),
                    pin.index(),
                    pin.id()
            );
        }
    }

    public static synchronized boolean removeClientDeco(int mapId, String key) {
        Map<String, PinData> pins = PINS_BY_MAP.get(mapId);
        if (pins != null) {
            pins.remove(key);
            if (pins.isEmpty()) {
                PINS_BY_MAP.remove(mapId);
            }
            saveClientMarkers();
        }
        return FOCUSED_PINS.remove(toFocusKey(mapId, key));
    }

    public static void focusClientDeco(MapDataHolder map, Object deco, boolean focused) {
        if (deco instanceof InternalPinDecoration pin) {
            String key = toFocusKey(map.id, pin.id());
            if (focused) {
                FOCUSED_PINS.add(key);
            } else {
                FOCUSED_PINS.remove(key);
            }
        }
    }

    public static boolean isClientDecoFocused(MapDataHolder map, Object deco) {
        return deco instanceof InternalPinDecoration pin && FOCUSED_PINS.contains(toFocusKey(map.id, pin.id()));
    }

    public static Identifier getPinTexture(int index, boolean small) {
        String texture = getPinTextureName(index);
        if (small) {
            texture += "_small";
        }
        return Identifier.fromNamespaceAndPath("map_atlases", "textures/map_marker/" + texture + ".png");
    }

    public static String getPinTextureName(int index) {
        return PIN_TEXTURES.get(normalizePinIndex(index));
    }

    public static int normalizePinIndex(int index) {
        return Math.floorMod(index, PIN_TEXTURES.size());
    }

    private static String toFocusKey(int mapId, String pinId) {
        return mapId + ":" + pinId;
    }

    private static Path getFilePath(String id, QuickPlayLog.Type type) {
        String fileName = (type == QuickPlayLog.Type.SINGLEPLAYER ? id : sanitiseServerName(id));
        return PlatHelper.getGamePath()
                .resolve("map_atlases/" + type.getSerializedName() + "/" + fileName + ".pins");
    }

    private static String sanitiseServerName(String input) {
        return input.toLowerCase()
                .replaceAll("\\]:\\d+$", "")
                .replaceAll("[\\[\\]]", "")
                .replaceAll("[^a-z0-9 ]", "_");
    }

    private record PinData(String mapStringId, int mapId, BlockPos pos, String markerId, String text, int index, String id) {
        private String serialize() {
            return mapStringId + "\t" + pos.getX() + "\t" + pos.getZ() + "\t" + index + "\t" + markerId + "\t" +
                    Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)) + "\t" + id;
        }

        private static PinData deserialize(String line) {
            String[] parts = line.split("\t", -1);
            if (parts.length != 7) {
                return null;
            }
            try {
                String mapStringId = parts[0];
                int mapId = mapStringId.contains("_")
                        ? MapAtlasesAccessUtils.findMapIntFromString(mapStringId)
                        : Integer.parseInt(mapStringId);
                if (!mapStringId.contains("_")) {
                    mapStringId = "map_" + mapStringId;
                }
                int x = Integer.parseInt(parts[1]);
                int z = Integer.parseInt(parts[2]);
                int index = Integer.parseInt(parts[3]);
                String text = new String(Base64.getDecoder().decode(parts[5]), StandardCharsets.UTF_8);
                return new PinData(mapStringId, mapId, new BlockPos(x, 0, z), parts[4], text, index, parts[6]);
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
