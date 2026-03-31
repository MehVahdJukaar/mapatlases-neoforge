package pepjebs.mapatlases.integration.moonlight;

import net.minecraft.client.quickplay.QuickPlayLog;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.ApiStatus;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.util.Set;

public class ClientMarkers {

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
    }

    public static Set<?> send(Integer integer, MapItemSavedData data) {
        return Set.of();
    }

    public static synchronized void addPin(MapDataHolder holder, ColumnPos pos, String text, int index) {
    }

    public static synchronized boolean removeClientDeco(int mapId, String key) {
        return false;
    }

    public static void focusClientDeco(MapDataHolder map, Object deco, boolean focused) {
    }

    public static boolean isClientDecoFocused(MapDataHolder map, Object deco) {
        return false;
    }
}
