package pepjebs.mapatlases.capabilities;

import com.mojang.datafixers.util.Pair;
import net.minecraft.Util;
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
import java.util.function.Predicate;

import static pepjebs.mapatlases.utils.MapAtlasesAccessUtils.createMapItemStackFromId;

// The porpoise of this object is to save a datastructures with all available maps so we dont have to keep deserializing nbt
public class MapCollectionCap implements IMapCollection, INBTSerializable<CompoundTag> {

    public static final Capability<MapCollectionCap> ATLAS_CAP_TOKEN = CapabilityManager.get(new CapabilityToken<>() {
    });

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(MapCollectionCap.class);
    }

    public static final String MAP_LIST_NBT = "maps";
    public static final String ACTIVE_MAP_NBT = "active_map";

    private final Map<MapKey, Pair<String, MapItemSavedData>> maps = new HashMap<>();
    private final Map<String, MapKey> keysMap = new HashMap<>();
    private final Map<String, Integer> idMap = new HashMap<>();
    //available dimensions and slices
    private final Map<ResourceKey<Level>, TreeSet<Integer>> dimensionSlices = new HashMap<>();
    @Nullable
    private Pair<String, MapItemSavedData> centerMap = null;
    private byte scale = 0;
    private CompoundTag lazyNbt = null;

    public MapCollectionCap() {
        int aa = 1;
    }

    public boolean isInitialized() {
        return lazyNbt == null;
    }

    private void assertInitialized() {
        assert this.lazyNbt == null;
    }


    // we need leven context
    public void initialize(Level level) {
        if (level.isClientSide) {
            int aa = 1;
        }
        if (lazyNbt != null) {
            int[] array = lazyNbt.getIntArray(MAP_LIST_NBT);
            for (int i : array) {
                add(i, level, Integer.MIN_VALUE);
            }
            String center = lazyNbt.getString(ACTIVE_MAP_NBT);
            if (!center.isEmpty()) {
                var data = level.getMapData(center);
                if (data != null) centerMap = Pair.of(center, data);
            }
            lazyNbt = null;
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        if (!isInitialized()) return lazyNbt;
        CompoundTag c = new CompoundTag();
        c.putIntArray(MAP_LIST_NBT, idMap.values().stream().toList());
        if (centerMap != null) {
            c.putString(ACTIVE_MAP_NBT, centerMap.getFirst());
        }
        return c;
    }

    @Override
    public void deserializeNBT(CompoundTag c) {
        lazyNbt = c.copy();
    }

    @Override
    public int getCount() {
        assertInitialized();
        return maps.size();
    }

    public boolean isEmpty() {
        assertInitialized();
        return maps.isEmpty();
    }

    @Override
    public boolean add(int mapId, Level level, @Nullable Integer debug) {
        assertInitialized();
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
            Integer slice = getSlice(d);
            if(!Objects.equals(Integer.MIN_VALUE, debug) && !Objects.equals(slice, debug)){
                int aa = 1;
            }
            MapKey key = new MapKey(d.dimension, d.centerX, d.centerZ, slice);
            //remove duplicates
            if (maps.containsKey(key)) {
                String first = maps.get(key).getFirst();
                keysMap.remove(first);
                idMap.remove(first);
            }
            keysMap.put(mapKey, key);
            idMap.put(mapKey, mapId);
            maps.put(key, Pair.of(mapKey, d));
            dimensionSlices.computeIfAbsent(key.dimension, a -> new TreeSet<>())
                    .add(key.slice == null ? Integer.MAX_VALUE : key.slice);
            if (maps.size() != keysMap.size()) {
                int aa = 1;
            }
            return true;
        }
        return false;
    }

    @Nullable
    public static Integer getSlice(MapItemSavedData data) {
        return MapAtlasesMod.SUPPLEMENTARIES ? SupplementariesCompat.getSlice(data) : null;
    }

    @Nullable
    @Override
    public Pair<String, MapItemSavedData> remove(String mapKey) {
        assertInitialized();
        var k = keysMap.remove(mapKey);
        idMap.remove(mapKey);
        if (k != null) {
            dimensionSlices.clear();
            for (var j : keysMap.values()) {
                dimensionSlices.computeIfAbsent(j.dimension, a -> new TreeSet<>())
                        .add(j.slice == null ? Integer.MAX_VALUE : j.slice);
            }
            return maps.remove(k);
        }
        return null;
    }

    @Override
    public Pair<String, MapItemSavedData> getActive() {
        assertInitialized();
        return centerMap;
    }

    @Override
    public byte getScale() {
        assertInitialized();
        return scale;
    }

    @Override
    public Collection<ResourceKey<Level>> getAvailableDimensions() {
        assertInitialized();
        return dimensionSlices.keySet();
    }

    private static final TreeSet<Integer> TOP = Util.make(() -> {
        var t = new TreeSet<Integer>();
        t.add(Integer.MAX_VALUE);
        return t;
    });

    @Override
    public TreeSet<Integer> getAvailableSlices(ResourceKey<Level> dimension) {
        assertInitialized();
        return dimensionSlices.getOrDefault(dimension, TOP);
    }

    @Override
    public Collection<Pair<String, MapItemSavedData>> getAll() {
        assertInitialized();
        return maps.values();
    }

    @Override
    public List<Pair<String, MapItemSavedData>> selectSection(ResourceKey<Level> dimension, @Nullable Integer slice) {
        assertInitialized();
        return maps.entrySet().stream().filter(e -> e.getKey().dimension.equals(dimension) && Objects.equals(e.getKey().slice, slice))
                .map(Map.Entry::getValue).toList();
    }

    @Override
    public List<Pair<String, MapItemSavedData>> filterSection(ResourceKey<Level> dimension, @Nullable Integer slice,
                                                              Predicate<MapItemSavedData> predicate) {
        assertInitialized();
        return maps.entrySet().stream().filter(e -> e.getKey().dimension.equals(dimension) && Objects.equals(e.getKey().slice, slice)
                        && predicate.test(e.getValue().getSecond()))
                .map(Map.Entry::getValue).toList();
    }

    @Override
    public Pair<String, MapItemSavedData> select(int x, int z, ResourceKey<Level> dimension, @Nullable Integer slice) {
        assertInitialized();
        return maps.get(new MapKey(dimension, x, z, slice));
    }

    @Override
    public Pair<String, MapItemSavedData> getClosest(double x, double z, ResourceKey<Level> dimension, @Nullable Integer slice) {
        assertInitialized();
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

    @Override
    public void setActive(@Nullable Pair<String, MapItemSavedData> activeMap) {
        assertInitialized();
        this.centerMap = activeMap;
    }

    private record MapKey(ResourceKey<Level> dimension, int mapX, int mapZ, @Nullable Integer slice) {
    }

}
