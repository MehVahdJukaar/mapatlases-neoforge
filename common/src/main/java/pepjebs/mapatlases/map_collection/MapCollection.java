package pepjebs.mapatlases.map_collection;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.Util;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.MapType;
import pepjebs.mapatlases.utils.Slice;

import java.util.*;
import java.util.function.Predicate;

public class MapCollection {

    public static final Codec<MapCollection> CODEC = Codec.simpleMap(
            MapType.CODEC, MapId.CODEC.listOf(), StringRepresentable.keys(MapType.values())
    ).xmap(MapCollection::new, m -> m.ids).codec();

    public static final StreamCodec<ByteBuf, MapCollection> STREAM_CODEC = ByteBufCodecs.map(
                    i -> new EnumMap<>(MapType.class), MapType.STREAM_CODEC, MapId.STREAM_CODEC.apply(ByteBufCodecs.list()))
            .map(MapCollection::new, m -> m.ids);

    protected final Map<MapSearchKey, MapDataHolder> maps = new HashMap<>();
    protected final EnumMap<MapType, List<MapId>> ids = new EnumMap<>(MapType.class);
    //available dimensions and slices
    protected final Map<ResourceKey<Level>, Map<MapType, TreeSet<MapId>>> dimensionSlices = new HashMap<>();
    protected final int size;
    protected byte scale = 0;
    // list of ids that have not been received yet
    protected final Map<MapType, Set<MapId>> notSyncedIds = new EnumMap<>(MapType.class);

    private boolean initialized = false;


    protected MapCollection(Map<MapType, List<MapId>> integers) {
        int s = 0;
        for (var e : integers.entrySet()) {
            this.ids.computeIfAbsent(e.getKey(), k -> new ArrayList<>())
                    .addAll(e.getValue());
            s += e.getValue().size();
        }
        this.size = s;
    }

    protected MapCollection(Map<MapType, List<MapId>> integers, Level level) {
        this(integers);
        this.initialize(level);
    }

    protected void assertInitialized() {
        Preconditions.checkState(this.isInitialized(), "map collection capability was not initialized");
    }

    public Map<MapType, List<MapId>> getIdsCopy() {
        Map<MapType, List<MapId>> map = new HashMap<>();
        for (var e : ids.entrySet()) {
            map.computeIfAbsent(e.getKey(), k -> new ArrayList<>())
                    .addAll(e.getValue());
        }
        return map;
    }

    public boolean hasMap(MapId id, MapType type) {
        assertInitialized();
        List<MapId> mapIds = ids.get(type);
        return mapIds != null && mapIds.contains(id);
    }

    public int getCount() {
        assertInitialized();
        return ids.size();
    }

    public boolean isReallyEmpty() {
        return ids.isEmpty();
    }

    public boolean isEmpty() {
        assertInitialized();
        return maps.isEmpty();
    }

    public byte getScale() {
        assertInitialized();
        return scale;
    }

    public Collection<MapType> getAvailableTypes(ResourceKey<Level> dimension) {
        assertInitialized();
        var mapTypeTreeSetMap = dimensionSlices.get(dimension);
        if (mapTypeTreeSetMap != null) return mapTypeTreeSetMap.keySet();
        else return List.of();
    }

    public Collection<ResourceKey<Level>> getAvailableDimensions() {
        assertInitialized();
        return dimensionSlices.keySet();
    }

    private static final TreeSet<Integer> TOP = Util.make(() -> {
        var t = new TreeSet<Integer>();
        t.add(Integer.MAX_VALUE);
        return t;
    });

    public TreeSet<Integer> getHeightTree(ResourceKey<Level> dimension, MapType kind) {
        assertInitialized();
        var d = dimensionSlices.get(dimension);
        if (d != null) {
            return d.getOrDefault(kind, TOP);
        }
        return TOP;
    }

    public List<MapDataHolder> getAll() {
        assertInitialized();
        return new ArrayList<>(maps.values());
    }

    public List<MapDataHolder> selectSection(Slice slice) {
        assertInitialized();
        return maps.entrySet().stream().filter(e -> e.getKey().isSameSlice(slice))
                .map(Map.Entry::getValue).toList();
    }

    public List<MapDataHolder> filterSection(Slice slice, Predicate<MapItemSavedData> predicate) {
        assertInitialized();
        return new ArrayList<>(maps.entrySet().stream().filter(e -> e.getKey().isSameSlice(slice)
                        && predicate.test(e.getValue().data))
                .map(Map.Entry::getValue).toList());
    }

    @Nullable
    public MapDataHolder select(MapSearchKey key) {
        assertInitialized();
        return maps.get(key);
    }

    @Nullable
    public MapDataHolder select(int x, int z, Slice slice) {
        return select(new MapSearchKey(x, z, slice));
    }

    @Nullable
    public MapDataHolder getClosest(Player player, Slice slice) {
        return getClosest(player.getX(), player.getZ(), slice);
    }

    @Nullable
    public MapDataHolder getClosest(double x, double z, Slice slice) {
        assertInitialized();
        MapDataHolder minDistState = null;
        for (var e : maps.entrySet()) {
            var key = e.getKey();
            if (key.isSameSlice(slice)) {
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
        return Mth.square(mapState.centerX - x) + Mth.square(mapState.centerZ - z);
    }


    public boolean hasOneSlice() {
        return maps.keySet().stream().anyMatch(k -> k.slice() != null);
    }

    protected boolean addInternal(MapId intId, MapType type, Level level) {
        assertInitialized();

        MapDataHolder found = MapDataHolder.get(intId, type, level);
        if (this.isEmpty() && found != null) {
            scale = found.data.scale;
        }

        if (found == null) {
            if (level instanceof ServerLevel) {
                MapAtlasesMod.LOGGER.error("Map with id {} not found in level {}", intId, level.dimension().location());
            } else {
                //wait till we receive data from server
                ids.add(intId);
                notSyncedIds.add(intId);
            }
            return false;
        }

        MapItemSavedData d = found.data;

        if (d != null && d.scale == scale) {
            MapSearchKey key = found.makeKey();

            //from now on we assume that all client maps cant have their center and data unfilled
            if (maps.containsKey(key)) {
                MapAtlasesMod.LOGGER.error("Duplicate map key {} found in level {}", key, level.dimension().location());
                return false;

            }
            ids.add(intId);
            maps.put(key, found);
            addToDimensionMap(key);
            return true;
        }
        return false;
    }

    protected void addToDimensionMap(MapSearchKey j) {
        dimensionSlices.computeIfAbsent(j.slice().dimension(), d -> new EnumMap<>(MapType.class))
                .computeIfAbsent(j.slice().type(), a -> new TreeSet<>())
                .add(j.slice().height() == null ? Integer.MAX_VALUE : j.slice().height());
    }


    public boolean isInitialized() {
        return initialized;
    }

    public MapCollection removeAndAssigns(ItemStack atlas, Level level, int toRemove) {
        //make id copy
        if (!ids.contains(toRemove)) return this;
        Collection<Integer> newIds = new HashSet<>(ids);
        newIds.remove(toRemove);
        var newColl = new MapCollection(newIds, level);
        atlas.set(MapAtlasesMod.MAP_COLLECTION.get(), newColl);
        return newColl;
    }

    public MapCollection addAndAssigns(ItemStack atlas, Level level, MapType type, Collection<MapId> map) {
        if (maps.isEmpty()) return this;

        Collection<Integer> newIds = new HashSet<>(ids);
        var newColl = new MapCollection(newIds, level);
        atlas.set(MapAtlasesMod.MAP_COLLECTION.get(), newColl);
        return newColl;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MapCollection that)) return false;
        return Objects.equals(ids, that.ids);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ids);
    }

    // we need leven context
    public void initialize(Level level) {
        if (!isInitialized()) {

            for (int i : ids) {
                addInternal(i, level);
            }
            initialized = true;
        }
    }

    // if a duplicate exists its likely that its data was not synced yet
    public void updateNotSynced(Level level) {
        notSyncedIds.removeIf(i -> addInternal(i, level));
    }


}
