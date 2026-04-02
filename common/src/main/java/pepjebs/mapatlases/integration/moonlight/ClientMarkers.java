package pepjebs.mapatlases.integration.moonlight;

import net.minecraft.client.quickplay.QuickPlayLog;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.ApiStatus;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClientMarkers {
    private static final List<String> PIN_TEXTURES = List.of(
            "pin",
            "pin_apple",
            "pin_aqua",
            "pin_blue",
            "pin_green",
            "pin_orange",
            "pin_purple",
            "pin_red",
            "pin_yellow"
    );
    private static final Set<String> FOCUSED_PINS = new HashSet<>();

    @ApiStatus.Internal
    public static void setWorldFolder(String pId, QuickPlayLog.Type type) {
    }

    @ApiStatus.Internal
    public static void deleteAllMarkersData(String folderName) {
    }

    @ApiStatus.Internal
    public static synchronized void loadClientMarkers(long seed, String levelName) {
    }

    public static void saveClientMarkers() {
    }

    public static synchronized void unloadWorld() {
        FOCUSED_PINS.clear();
    }

    public static Set<?> send(Integer integer, MapItemSavedData data) {
        return Set.of();
    }

    public static synchronized void addPin(MapDataHolder holder, ColumnPos pos, String text, int index) {
    }

    public static synchronized boolean removeClientDeco(int mapId, String key) {
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
        String texture = PIN_TEXTURES.get(normalizePinIndex(index));
        if (small) {
            texture += "_small";
        }
        return Identifier.fromNamespaceAndPath("map_atlases", "textures/map_marker/" + texture + ".png");
    }

    public static int normalizePinIndex(int index) {
        return Math.floorMod(index, PIN_TEXTURES.size());
    }

    private static String toFocusKey(int mapId, String pinId) {
        return mapId + ":" + pinId;
    }
}
