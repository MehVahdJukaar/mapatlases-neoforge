package pepjebs.mapatlases.capabilities;

import com.google.common.base.Preconditions;
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
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.MapType;
import pepjebs.mapatlases.utils.Slice;

import java.util.*;
import java.util.function.Predicate;

// The porpoise of this object is to save a datastructures with all available maps so we dont have to keep deserializing nbt
public class MapCollectionCap implements IMapCollection, INBTSerializable<CompoundTag> {

    public static final Capability<MapCollectionCap> ATLAS_CAP_TOKEN = CapabilityManager.get(new CapabilityToken<>() {
    });

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(MapCollectionCap.class);
    }

    public static final String MAP_LIST_NBT = "maps";

    private final Map<MapKey, MapDataHolder> maps = new HashMap<>();
    private final Set<Integer> ids = new HashSet<>();
    //available dimensions and slices
    private final Map<ResourceKey<Level>, Map<MapType, TreeSet<Integer>>> dimensionSlices = new HashMap<>();
    private byte scale = 0;
    private CompoundTag lazyNbt = null;
    private final Set<Integer> duplicates = new HashSet<>();

    public MapCollectionCap() {
    }

    public boolean isInitialized() {
        return lazyNbt == null;
    }

    private void assertInitialized() {
        Preconditions.checkState(this.lazyNbt == null, "map collection capability was not initialized");
    }

    // if a duplicate exists its likely that its data was somehow not synced yet
    public void fixDuplicates(Level level) {
        duplicates.removeIf(i -> add(i, level));
    }

    // we need leven context
    public void initialize(Level level) {
        if (level.isClientSide) {
            int aa = 1;
        }
        if (lazyNbt != null) {
            int[] array = lazyNbt.getIntArray(MAP_LIST_NBT);
            lazyNbt = null;
            for (int i : array) {
                add(i, level);
            }
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        if (!isInitialized()) return lazyNbt;
        CompoundTag c = new CompoundTag();
        c.putIntArray(MAP_LIST_NBT, ids.stream().toList());
        return c;
    }

    @Override
    public int[] getAllIds() {
        if (!isInitialized()) return lazyNbt.getIntArray(MAP_LIST_NBT);
        return ids.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public void deserializeNBT(CompoundTag c) {
        lazyNbt = c.copy();
    }

    @Override
    public int getCount() {
        assertInitialized();
        return ids.size();
    }

    public boolean isEmpty() {
        assertInitialized();
        return maps.isEmpty();
    }

    @Override
    public boolean add(int intId, Level level) {
        assertInitialized();

        MapDataHolder found = MapDataHolder.findFromId(level, intId);
        if (this.isEmpty() && found != null) {
            scale = found.data.scale;
        }

        if (found == null) {
            if (level instanceof ServerLevel) {
                // Create a default map if server doesnt have it. Should never happen
                ItemStack map = MapAtlasesAccessUtils.createMapItemStackFromId(intId);
                MapItemSavedData d = MapItem.getSavedData(map, level);
                String mapString = MapItem.makeKey(MapItem.getMapId(map));
                found = new MapDataHolder(mapString, d);
            } else {
                //wait till we reiceie data from server
                ids.add(intId);
                if (!duplicates.contains(intId)) duplicates.add(intId);
                return false;
            }
        }
        MapItemSavedData d = found.data;

        if (d != null && d.scale == scale) {
            MapKey key = found.makeKey();

            //from now on we assume that all client maps cant have their center and data unfilled
            if (maps.containsKey(key)) {
                if (true) return false;
                //remove duplicates

                //this should not happen anymire actually
                var old = maps.get(key);
                ids.add(intId);
                if (!duplicates.contains(intId)) duplicates.add(intId);
                return false;
                //if we reach here something went wrong. likely extra map data not being received yet. TODO: fix
                //we just store the map id without actually adding it as its map key is incorrect
                //error

            }
            ids.add(intId);
            maps.put(key, found);
            addToDimensionMap(key);
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(MapDataHolder map) {
        assertInitialized();
        boolean success = ids.remove(map.id);
        if (maps.remove(map.makeKey()) != null) {
            dimensionSlices.clear();
            for (var j : maps.keySet()) {
                addToDimensionMap(j);
            }
        }
        return success;
    }

    private void addToDimensionMap(MapKey j) {
        dimensionSlices.computeIfAbsent(j.dimension(), d -> new EnumMap<>(MapType.class))
                .computeIfAbsent(j.slice().type(), a -> new TreeSet<>())
                .add(j.slice().height() == null ? Integer.MAX_VALUE : j.slice().height());
    }

    @Override
    public byte getScale() {
        assertInitialized();
        return scale;
    }

    @Override
    public Collection<MapType> getAvailableTypes(ResourceKey<Level> dimension) {
        assertInitialized();
        return dimensionSlices.get(dimension).keySet();
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
    public TreeSet<Integer> getHeightTree(ResourceKey<Level> dimension, MapType kind) {
        assertInitialized();
        var d = dimensionSlices.get(dimension);
        if (d != null) {
            return d.getOrDefault(kind, TOP);
        }
        return TOP;
    }

    @Override
    public List<MapDataHolder> getAll() {
        assertInitialized();
        return new ArrayList<>(maps.values());
    }

    @Override
    public List<MapDataHolder> selectSection(ResourceKey<Level> dimension, Slice type) {
        assertInitialized();
        return maps.entrySet().stream().filter(e -> e.getKey().isSameDimSameSlice(dimension, type))
                .map(Map.Entry::getValue).toList();
    }

    @Override
    public List<MapDataHolder> filterSection(ResourceKey<Level> dimension, Slice slice,
                                             Predicate<MapItemSavedData> predicate) {
        assertInitialized();
        return new ArrayList<>(maps.entrySet().stream().filter(e -> e.getKey().isSameDimSameSlice(dimension, slice)
                        && predicate.test(e.getValue().data))
                .map(Map.Entry::getValue).toList());
    }

    @Nullable
    @Override
    public MapDataHolder select(MapKey key) {
        assertInitialized();
        return maps.get(key);
    }

    @Nullable
    @Override
    public MapDataHolder getClosest(double x, double z, ResourceKey<Level> dimension, Slice slice) {
        assertInitialized();
        MapDataHolder minDistState = null;
        for (var e : maps.entrySet()) {
            var key = e.getKey();
            if (key.isSameDimSameSlice(dimension, slice)) {
                if (minDistState == null) {
                    minDistState = e.getValue();
                    continue;
                }
                if (distSquare(minDistState.data, x, z) > distSquare(e.getValue().data, x, z)) {
                    minDistState = e.getValue();
                }
            }
        }
        return minDistState;
    }

    public static double distSquare(MapItemSavedData mapState, double x, double z) {
        return Mth.square(mapState.x - x) + Mth.square(mapState.z - z);
    }


    public boolean hasOneSlice() {
        return maps.keySet().stream().anyMatch(k -> k.slice() != null);
    }
}