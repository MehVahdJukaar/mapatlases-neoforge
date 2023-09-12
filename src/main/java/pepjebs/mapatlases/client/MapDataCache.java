package pepjebs.mapatlases.client;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MapDataCache {

    private static final Map<MapKey, Pair<String, MapItemSavedData>> CACHED_MAP = new HashMap<>();

    public static Pair<String, MapItemSavedData> getMapAt(ResourceKey<Level> dimension, int size, int mapX, int mapZ) {
        return getMapAt(dimension, size, mapX, mapZ, null);
    }

    @Nullable
    public static Pair<String, MapItemSavedData> getMapAt(ResourceKey<Level> dimension, int size, int mapX, int mapZ, @Nullable Integer slice) {
        return CACHED_MAP.get(new MapKey(dimension, size, mapX, mapZ, slice));
    }

    private record MapKey(ResourceKey<Level> dimension, int scale, int mapX, int mapZ, @Nullable Integer slice) {
    }

    public static void populateMapCache(Collection<Pair<String, MapItemSavedData>> currentAtlasData) {
        CACHED_MAP.clear();
        for (var p : currentAtlasData) {
            MapItemSavedData d = p.getSecond();
            Integer slice = null;
            MapKey key = new MapKey(d.dimension, d.scale, d.centerX, d.centerX, slice);
            CACHED_MAP.put(key, p);
        }
    }
}
