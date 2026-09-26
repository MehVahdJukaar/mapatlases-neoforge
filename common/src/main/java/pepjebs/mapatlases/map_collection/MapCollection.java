package pepjebs.mapatlases.map_collection;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.MapType;
import pepjebs.mapatlases.utils.Slice;

import java.util.*;
import java.util.function.Predicate;

public class MapCollection {

    private static final NavigableSet<Integer> TOP = Collections.unmodifiableNavigableSet(new TreeSet<>(List.of(Integer.MAX_VALUE)));

    private final MapIds ids;
    private final Map<MapGridKey, MapDataHolder> maps = new HashMap<>();
    //available dimensions and slices
    private final Map<ResourceKey<Level>, Map<MapType, TreeSet<Integer>>> mapHeights = new HashMap<>();
    private byte scale = 0;
    // list of ids that have not been received yet
    private final Set<Pair<MapType, MapId>> notSyncedIds = new HashSet<>();

    MapCollection(MapIds ids, Level level) {
        this.ids = ids;
        for (var e : ids.getAll().entrySet()) {
            for (MapId id : e.getValue()) {
                populateInDataStructure(id, e.getKey(), level);
            }
        }
    }

    public MapIds getIds() {
        return ids;
    }

    public boolean hasMap(MapId id, MapType type) {
        return ids.contains(type, id);
    }

    public int getCount() {
        return ids.getCount();
    }

    public boolean isEmpty() {
        return maps.isEmpty();
    }

    public byte getScale() {
        return scale;
    }

    public Collection<MapType> getAvailableTypes(ResourceKey<Level> dimension) {
        return mapHeights.getOrDefault(dimension, Map.of()).keySet();
    }

    public Collection<ResourceKey<Level>> getAvailableDimensions() {
        return mapHeights.keySet();
    }

    public boolean hasDimension(ResourceKey<Level> levelResourceKey) {
        return mapHeights.containsKey(levelResourceKey);
    }

    //DONT MODIFY THIS SET
    public NavigableSet<Integer> getHeightTree(ResourceKey<Level> dimension, MapType kind) {
        var tree = mapHeights.getOrDefault(dimension, Map.of()).get(kind);
        return tree == null ? TOP : Collections.unmodifiableNavigableSet(tree);
    }

    public Slice closestAvailableSlice(ResourceKey<Level> dimension, Slice preferred) {
        if (!getMapsInSlice(preferred).isEmpty()) return preferred;
        var types = getAvailableTypes(dimension);
        if (types.isEmpty()) return preferred;
        MapType type = types.contains(preferred.type()) ? preferred.type() : types.iterator().next();
        return sliceNearHeight(dimension, type, preferred.heightOrTop());
    }

    public Slice sliceNearHeight(ResourceKey<Level> dimension, MapType type, int height) {
        NavigableSet<Integer> heights = getHeightTree(dimension, type);
        Integer below = heights.floor(height);
        return Slice.of(type, below == null ? heights.first() : below, dimension);
    }

    @Nullable
    public Slice adjacentSlice(Slice slice, boolean up) {
        NavigableSet<Integer> heights = getHeightTree(slice.dimension(), slice.type());
        int current = slice.heightOrTop();
        Integer next = up ? heights.ceiling(current + 1) : heights.floor(current - 1);
        return next == null ? null : Slice.of(slice.type(), next, slice.dimension());
    }

    @Nullable
    public Slice nextTypeSlice(Slice slice) {
        var types = new ArrayList<>(getAvailableTypes(slice.dimension()));
        if (types.isEmpty()) return null;
        MapType next = types.get((types.indexOf(slice.type()) + 1) % types.size());
        return sliceNearHeight(slice.dimension(), next, slice.heightOrTop());
    }

    public List<MapDataHolder> getAllFound() {
        return new ArrayList<>(maps.values());
    }

    public List<MapDataHolder> getMapsInSlice(Slice slice) {
        return filter(m -> Objects.equals(m.slice, slice));
    }

    public List<MapDataHolder> filter(Predicate<MapDataHolder> predicate) {
        List<MapDataHolder> matching = new ArrayList<>();
        for (MapDataHolder holder : maps.values()) {
            if (predicate.test(holder)) matching.add(holder);
        }
        return matching;
    }

    @Nullable
    public MapDataHolder getMapAt(MapGridKey key) {
        return maps.get(key);
    }

    @Nullable
    public MapDataHolder getMapAt(int x, int z, Slice slice) {
        return getMapAt(MapGridKey.at(scale, slice, x, z));
    }

    @Nullable
    public MapDataHolder getClosest(Player player, Slice slice) {
        return getClosest(player.getX(), player.getZ(), slice);
    }

    @Nullable
    public MapDataHolder getClosest(double x, double z, Slice slice) {
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

    private static double distSquare(MapItemSavedData mapState, double x, double z) {
        return Mth.square(mapState.centerX - x) + Mth.square(mapState.centerZ - z);
    }


    public boolean hasAnySlicedMap() {
        return maps.keySet().stream().anyMatch(k -> k.slice.height().isPresent());
    }

    private boolean populateInDataStructure(MapId id, MapType type, Level level) {
        MapDataHolder found = MapDataHolder.find(id, type, level);
        if (found == null) {
            if (level instanceof ServerLevel) {
                MapAtlasesMod.LOGGER.error("Map with id {} not found in level {}", id, level.dimension().location());
            } else {
                //wait till we receive data from server
                notSyncedIds.add(Pair.of(type, id));
            }
            return false;
        }

        // scale comes from the first map that resolves. Cant use initialized here as on the client
        // the collection can be initialized before any map data has arrived
        if (maps.isEmpty()) {
            scale = found.data.scale;
        }
        if (found.data.scale != scale) return false;

        MapGridKey key = found.makeKey();
        //from now on we assume that all client maps cant have their center and data unfilled
        if (maps.containsKey(key)) {
            // Existing atlases can carry a lot of these, so log at debug rather than error.
            MapAtlasesMod.LOGGER.debug("Duplicate map key {} found in level {}", key, level.dimension().location());
            return false;
        }
        maps.put(key, found);
        registerSliceHeight(key.slice);
        return true;
    }

    private void registerSliceHeight(Slice slice) {
        mapHeights.computeIfAbsent(slice.dimension(), d -> new EnumMap<>(MapType.class))
                .computeIfAbsent(slice.type(), a -> new TreeSet<>())
                .add(slice.heightOrTop());
    }

    // if a duplicate exists its likely that its data was not synced yet
    public void resolvePendingMaps(Level level) {
        notSyncedIds.removeIf(i -> populateInDataStructure(i.getValue(), i.getKey(), level));
    }

    //takenCells is shared across the whole batch so two maps cant land on the same cell
    private List<MapId> filterMapsThatCanBeAdded(Level level, MapType type, Collection<MapId> candidates, Set<MapGridKey> takenCells) {
        List<MapId> accepted = new ArrayList<>();
        for (MapId id : candidates) {
            if (ids.contains(type, id)) continue;
            if (accepted.contains(id)) continue;
            MapDataHolder found = MapDataHolder.find(id, type, level);
            if (found == null) {
                // not resolvable yet on the client, so let indexMap judge it later
                accepted.add(id);
                continue;
            }
            //an empty collection has no scale yet, the first map added decides it
            if (!maps.isEmpty() && found.data.scale != scale) continue;
            MapGridKey cell = found.makeKey();
            if (maps.containsKey(cell)) continue;
            if (!takenCells.add(cell)) continue;
            accepted.add(id);
        }
        return accepted;
    }

    public boolean addAndAssign(ItemStack atlas, Level level, MapType type, MapId map) {
        return addAndAssign(atlas, level, Map.of(type, List.of(map)));
    }

    //true if at least one map made it in, dupes and scale mismatches just get dropped
    public boolean addAndAssign(ItemStack atlas, Level level, Map<MapType, ? extends Collection<MapId>> candidates) {
        Map<MapType, List<MapId>> toAdd = new EnumMap<>(MapType.class);
        Set<MapGridKey> takenCells = new HashSet<>();
        for (var e : candidates.entrySet()) {
            List<MapId> accepted = filterMapsThatCanBeAdded(level, e.getKey(), e.getValue(), takenCells);
            if (!accepted.isEmpty()){
                toAdd.put(e.getKey(), accepted);
            }
        }
        if (toAdd.isEmpty()) return false;
        atlas.set(MapAtlasesMod.MAP_COLLECTION.get(), ids.plus(toAdd));
        return true;
    }

    public boolean removeAndAssign(ItemStack atlas, Collection<MapDataHolder> holders) {
        MapIds newIds = ids.minus(holders);
        if (newIds.getCount() == ids.getCount()) return false;
        atlas.set(MapAtlasesMod.MAP_COLLECTION.get(), newIds);
        return true;
    }

    //height to auto switch to for where the player stands, null to stay put
    @Nullable
    public Integer nearestReachableSliceHeight(Player player, ResourceKey<Level> dimension, MapType type) {
        Level level = player.level();
        NavigableSet<Integer> heightTree = getHeightTree(dimension, type);
        if (heightTree.size() == 1) return null;
        int y = player.getBlockY();

        int worldSurface = level.getHeight(Heightmap.Types.OCEAN_FLOOR, player.getBlockX(), player.getBlockZ());
        boolean isAboveHeightMap = y >= worldSurface;
        Integer ceiling = heightTree.ceiling(y);
        if (isAboveHeightMap) {
            return ceiling;
        }
        //if not aove check one below and above where we are
        else {
            Integer floor = heightTree.floor(y);

            int aboveDist = ceiling == null ? 0 : ceiling - y;
            int belowDist = floor == null ? 0 : y - floor;
            int max = Math.max(belowDist, aboveDist);
            boolean canGoUp = true;
            boolean canGoDown = true;
            BlockPos.MutableBlockPos pos = player.blockPosition().mutable();
            int startY = pos.getY();
            for (int j = 1; j <= max; j++) {
                //nothing found. we dont change
                if (!canGoUp && !canGoDown) {
                    return null;
                }
                if (j == aboveDist) {
                    return ceiling;
                }
                if (j == belowDist) {
                    return floor;
                }
                if (canGoUp) {
                    pos.setY(startY + j);
                    if (level.getBlockState(pos).getMapColor(level, pos) != MapColor.NONE) {
                        canGoUp = false;
                    }
                }
                if (canGoDown) {
                    pos.setY(startY - j);
                    if (level.getBlockState(pos).getMapColor(level, pos) != MapColor.NONE) {
                        canGoDown = false;
                    }
                }
            }
            return null;
        }
    }

}
