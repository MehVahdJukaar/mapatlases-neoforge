package pepjebs.mapatlases.capabilities;

import com.mojang.datafixers.util.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.SupplementariesCompat;

import java.util.*;

import static pepjebs.mapatlases.utils.MapAtlasesAccessUtils.createMapItemStackFromId;

// The porpoise of this object is to save a datastructures with all available maps so we dont have to keep deserializing nbt
public class MapCollectionCap implements IMapCollection, INBTSerializable<CompoundTag> {

    public static final Capability<MapCollectionCap> ATLAS_CAP_TOKEN = CapabilityManager.get(new CapabilityToken<>() {
    });

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(MapCollectionCap.class);
    }

    public static final String MAP_LIST_NBT = "maps";

    private final Map<MapKey, Pair<String, MapItemSavedData>> maps = new HashMap<>();
    private final Map<String, Pair<MapKey, Integer>> keysMap = new HashMap<>();
    private final Set<ResourceKey<Level>> availableDimensions = new HashSet<>();
    @Nullable
    private Pair<String, MapItemSavedData> centerMap = null;
    private byte scale = 0;

    public MapCollectionCap() {
        int aa = 1;
    }


    @Override
    public CompoundTag serializeNBT() {
        CompoundTag c = new CompoundTag();
        c.putIntArray(MAP_LIST_NBT, getIds());

        return c;
    }

    @Override
    public void deserializeNBT(CompoundTag c) {
        int[] array = c.getIntArray(MAP_LIST_NBT);
        Level level = MapAtlasesMod.giveMeALevelPls();
        for (int i : array) {
            add(i, level);
        }
    }

    //TODO: improve
    @Override
    @Deprecated(forRemoval = true)
    public List<Integer> getIds() {
        return keysMap.values().stream().map(Pair::getSecond).toList();
    }

    @Override
    public int getCount() {
        return maps.size();
    }

    public boolean isEmpty() {
        return maps.isEmpty();
    }

    @Override
    public boolean add(int mapId, Level level) {
        String mapKey = MapItem.makeKey(mapId);
        MapItemSavedData d = level.getMapData(mapKey);
        if (this.isEmpty() && d != null) {
            scale = d.scale;
            centerMap = Pair.of(mapKey, d);
        }

        if (d == null && level instanceof ServerLevel) {
            ItemStack map = createMapItemStackFromId(mapId);
            d = MapItem.getSavedData(map, level);
        }
        if (d != null && d.scale == scale) {
            Integer slice = MapAtlasesMod.SUPPLEMENTARIES ? SupplementariesCompat.getSlice(d) : null;
            MapKey key = new MapKey(d.dimension, d.centerX, d.centerZ, slice);
            //remove duplicates
            if(maps.containsKey(key)){
                keysMap.remove(maps.get(key).getFirst());
            }
            keysMap.put(mapKey, Pair.of(key, mapId));
            maps.put(key, Pair.of(mapKey, d));
            availableDimensions.add(d.dimension);
            if(maps.size() != keysMap.size()){
                int aa = 1;
            }
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public Pair<String, MapItemSavedData> remove(String mapKey) {
        var k = keysMap.remove(mapKey);
        if (k != null) {
            availableDimensions.clear();
            for (var j : keysMap.values()) {
                availableDimensions.add(j.getFirst().dimension);
            }
            return maps.remove(k.getFirst());
        }
        return null;
    }

    @Override
    public Pair<String, MapItemSavedData> getActive() {
        return centerMap;
    }

    @Override
    public byte getScale() {
        return scale;
    }

    @Override
    public Collection<ResourceKey<Level>> getAvailableDimensions() {
        return availableDimensions;
    }

    @Override
    public Collection<Pair<String, MapItemSavedData>> getAll() {
        return maps.values();
    }

    @Override
    public Collection<Pair<String, MapItemSavedData>> selectSection(ResourceKey<Level> dimension, @Nullable Integer slice) {
        return maps.entrySet().stream().filter(e -> e.getKey().dimension.equals(dimension) && Objects.equals(e.getKey().slice, slice))
                .map(Map.Entry::getValue).toList();
    }

    @Override
    public Pair<String, MapItemSavedData> select(int x, int z, ResourceKey<Level> dimension, @Nullable Integer slice) {
        return maps.get(new MapKey(dimension, x, z, slice));
    }

    @Override
    public Pair<String, MapItemSavedData> getClosest(double x, double z, ResourceKey<Level> dimension, @Nullable Integer slice) {
        Pair<String, MapItemSavedData> minDistState = null;
        for (var e : maps.entrySet()) {
            var key = e.getKey();
            if (key.dimension.equals(dimension) && Objects.equals(key.slice, slice)) {
                if (minDistState == null) {
                    minDistState = e.getValue();
                    continue;
                }
                if (distSquare(minDistState.getSecond(), x, z) > distSquare(e.getValue().getSecond(), x, z)) {
                    minDistState = e.getValue();
                }
            }
        }
        return minDistState;
    }

    public static double distSquare(MapItemSavedData mapState, double x, double z) {
        return Mth.square(mapState.centerX - x) + Mth.square(mapState.centerZ - z);
    }

    private record MapKey(ResourceKey<Level> dimension, int mapX, int mapZ, @Nullable Integer slice) {
    }

}
