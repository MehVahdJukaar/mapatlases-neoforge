package pepjebs.mapatlases.client;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.SupplementariesCompat;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.*;

public class MapDataCache {

    //Hmm we could make this an itemstack capability. later after fabric version i guess
    private static final Map<MapKey, Pair<String, MapItemSavedData>> CACHED_MAP = new HashMap<>();
    private static final Set<ResourceKey<Level>> DIMENSIONS_CACHE = new HashSet<>();
    private static ItemStack activeAtlas = ItemStack.EMPTY;

    public static Pair<String, MapItemSavedData> getMapAt(ResourceKey<Level> dimension, int size, int mapX, int mapZ) {
        return getMapAt(dimension, size, mapX, mapZ, null);
    }

    @Nullable
    public static Pair<String, MapItemSavedData> getMapAt(ResourceKey<Level> dimension, int size, int mapX, int mapZ, @Nullable Integer slice) {
        return CACHED_MAP.get(new MapKey(dimension, size, mapX, mapZ, slice));
    }

    public static Set<ResourceKey<Level>> getAtlasDimensions() {
        return DIMENSIONS_CACHE;
    }

    private record MapKey(ResourceKey<Level> dimension, int scale, int mapX, int mapZ, @Nullable Integer slice) {
    }

    public static void populateMapCache(Collection<Pair<String, MapItemSavedData>> currentAtlasData) {
        CACHED_MAP.clear();
        boolean supplementaries = MapAtlasesMod.SUPPLEMENTARIES;
        for (var p : currentAtlasData) {
            MapItemSavedData d = p.getSecond();
            Integer slice = supplementaries ? SupplementariesCompat.getSlice(d) : null;
            MapKey key = new MapKey(d.dimension, d.scale, d.centerX, d.centerX, slice);
            CACHED_MAP.put(key, p);
            DIMENSIONS_CACHE.add(d.dimension);
        }
    }

    //called when item changes. New maps will be added by packet sent when they are created
    public static void acceptAtlasItem(Level level, ItemStack atlas) {
        populateMapCache(MapAtlasesAccessUtils.getAllMapData(level, atlas));
        activeAtlas = atlas;
    }


    @Nullable
    public static Pair<String, MapItemSavedData> getClosestMapData(
            Player player, int scale, @Nullable Integer slice) {
        Pair<String, MapItemSavedData> minDistState = null;
        ResourceKey<Level> dim = player.level().dimension();
        for (var e : CACHED_MAP.entrySet()) {
            var key = e.getKey();
            if (key.dimension.equals(dim) && scale == key.scale() && Objects.equals(key.slice, slice)) {
                if (minDistState == null) {
                    minDistState = e.getValue();
                    continue;
                }
                if (distSquare(minDistState.getSecond(), player) > distSquare(e.getValue().getSecond(), player)) {
                    minDistState = e.getValue();
                }
            }
        }
        return minDistState;
    }

    public static double distSquare(MapItemSavedData mapState, Player player) {
        return Mth.square(mapState.centerX - player.getX()) +
                Mth.square(mapState.centerZ - player.getZ());
    }

}
