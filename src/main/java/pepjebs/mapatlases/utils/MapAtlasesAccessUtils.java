package pepjebs.mapatlases.utils;

import com.mojang.datafixers.util.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.fml.loading.FMLEnvironment;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.item.MapAtlasItem;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MapAtlasesAccessUtils {

    public static int[] getMapIdsFromItemStack(ItemStack atlas) {
        CompoundTag tag = atlas.getTag();
        return tag != null ? tag.getIntArray(MapAtlasItem.MAP_LIST_NBT) : new int[]{};
    }

    // map identifier & map data grouped together
    public static Collection<Pair<String, MapItemSavedData>> getAllMapData(Level level, ItemStack atlas) {
        int[] mapIds = getMapIdsFromItemStack(atlas);
        if (mapIds.length == 0) return List.of();
        List<Pair<String, MapItemSavedData>> mapStates = new ArrayList<>();
        for (int mapId : mapIds) {
            String mapName = MapItem.makeKey(mapId);
            MapItemSavedData state = level.getMapData(mapName);
            if (state == null) {
                ItemStack map = createMapItemStackFromId(mapId);
                state = MapItem.getSavedData(map, level);
            }
            if (state != null) {
                mapStates.add(Pair.of(mapName, state));
            }
        }
        return mapStates;
    }

    public static Map<ResourceKey<Level>, List<Pair<String, MapItemSavedData>>> getAllMapDataByDimension(Level level, ItemStack atlas) {
        return getAllMapData(level, atlas).stream()
                .collect(Collectors.groupingBy(
                        dataObject -> dataObject.getSecond().dimension,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                Function.identity(),
                                Collectors.toList()
                        )
                ));
    }

    public static Collection<Pair<String, MapItemSavedData>> getAllMapDataForDimension(Level level, ItemStack atlas) {
        return getAllMapDataByDimension(level, atlas).getOrDefault(level.dimension(), List.of());
    }

    public static Pair<String, MapItemSavedData> getClosestMapDataFromAtlas(ServerPlayer player, ItemStack atlas) {
        return getClosestMapData(getAllMapDataForDimension(player.level(), atlas), player);
    }


    public static Pair<String, MapItemSavedData> getClosestMapData(
            Collection<Pair<String, MapItemSavedData>> currentDimMapInfos,
            Player player) {
        Pair<String, MapItemSavedData> minDistState = null;
        for (var state : currentDimMapInfos) {
            if (minDistState == null) {
                minDistState = state;
                continue;
            }
            if (distSquare(minDistState.getSecond(), player) > distSquare(state.getSecond(), player)) {
                minDistState = state;
            }
        }
        return minDistState;
    }

    // no square root faster
    public static double distSquare(MapItemSavedData mapState, Player player) {
        return Mth.square(mapState.centerX - player.getX()) + Mth.square(mapState.centerZ - player.getZ());
    }


    public static ItemStack createMapItemStackFromId(int id) {
        ItemStack map = new ItemStack(Items.FILLED_MAP);
        map.getOrCreateTag().putInt("map", id);
        return map;
    }

    public static int getMapIntFromString(String id) {
        if (id == null) {
            MapAtlasesMod.LOGGER.error("Encountered null id when fetching map name. Env: {}", FMLEnvironment.dist);
            return 0;
        }
        return Integer.parseInt(id.substring(4));
    }


    // KEEP NAME
    @Deprecated(forRemoval = true)
    public static Map<String, MapItemSavedData> getAllMapInfoFromAtlas(Level level, ItemStack atlas) {
        if (atlas.getTag() == null) return new HashMap<>();
        int[] mapIds = Arrays.stream(atlas.getTag().getIntArray(MapAtlasItem.MAP_LIST_NBT)).toArray();
        Map<String, MapItemSavedData> mapStates = new HashMap<>();
        for (int mapId : mapIds) {
            String mapName = MapItem.makeKey(mapId);
            MapItemSavedData state = level.getMapData(mapName);
            if (state == null && level instanceof ServerLevel) {
                ItemStack map = createMapItemStackFromId(mapId);
                state = MapItem.getSavedData(map, level);
            }
            if (state != null) {
                mapStates.put(mapName, state);
            }
        }
        return mapStates;
    }

    // KEEP NAME
    @Deprecated(forRemoval = true)
    public static Map.Entry<String, MapItemSavedData> getActiveAtlasMapStateServer(
            Map<String, MapItemSavedData> currentDimMapInfos,
            ServerPlayer player) {
        var p = getClosestMapData(currentDimMapInfos.entrySet().stream()
                .map(entry -> new Pair<>(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList()), player);
        return new AbstractMap.SimpleEntry<>(p.getFirst(), p.getSecond());
    }

}
