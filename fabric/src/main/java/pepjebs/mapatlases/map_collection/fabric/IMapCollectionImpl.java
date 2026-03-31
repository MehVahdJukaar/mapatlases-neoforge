package pepjebs.mapatlases.map_collection.fabric;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.map_collection.IMapCollection;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.map_collection.MapKey;
import pepjebs.mapatlases.utils.ItemStackData;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.MapType;
import pepjebs.mapatlases.utils.Slice;

import java.util.Collection;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Predicate;

// Proxy class
public class IMapCollectionImpl implements IMapCollection {

    private static final IdentityHashMap<ItemStack, IMapCollectionImpl> INSTANCES = new IdentityHashMap<>();

    @Nullable
    private MapCollection instance = null;

    private final ItemStack stack;

    public IMapCollectionImpl(ItemStack stack) {
        this.stack = stack;
    }

    public static IMapCollection get(ItemStack stack, Level level) {
        return INSTANCES.computeIfAbsent(stack, IMapCollectionImpl::new).getOrCreateInstance(level);
    }

    protected IMapCollection getOrCreateInstance(Level level) {
        if (instance == null || !matchesStackData()) {
            instance = new MapCollection();
            var tag = ItemStackData.getTag(stack);
            if (tag != null) {
                instance.deserializeNBT(tag);
            }
            instance.initialize(level);
        }
        return this;
    }

    private boolean matchesStackData() {
        if (instance == null) {
            return false;
        }
        var tag = ItemStackData.getTag(stack);
        int[] stackIds = tag != null ? tag.getIntArray(MapCollection.MAP_LIST_NBT).orElseGet(() -> new int[0]) : new int[0];
        return Arrays.equals(stackIds, instance.getAllIds());
    }

    private void markDirty() {
        if (instance != null) {
            ItemStackData.update(stack, tag -> tag.putIntArray(MapCollection.MAP_LIST_NBT, instance.getAllIds()));
        }
    }

    @Override
    public boolean add(int mapId, Level level) {
        if (instance != null) {
            boolean ret = instance.add(mapId, level);
            if (ret) markDirty();
            return ret;
        }
        return false;
    }

    @Override
    public boolean remove(MapDataHolder obj) {
        if (instance != null) {
            boolean ret = instance.remove(obj);
            if (ret) markDirty();
            return ret;
        }
        return false;
    }

    @Override
    public int getCount() {
        return instance == null ? 0 : instance.getCount();
    }

    @Override
    public boolean isEmpty() {
        return instance == null || instance.isEmpty();
    }

    @Override
    public byte getScale() {
        return instance == null ? 0 : instance.getScale();
    }

    @Override
    public int[] getAllIds() {
        return instance == null ? new int[0] : instance.getAllIds();
    }

    public boolean hasId(int id) {
        return instance != null && instance.hasId(id);
    }

    @Override
    public Collection<ResourceKey<Level>> getAvailableDimensions() {
        return instance == null ? List.of() : instance.getAvailableDimensions();
    }

    @Override
    public Collection<MapType> getAvailableTypes(ResourceKey<Level> dimension) {
        return instance == null ? List.of() : instance.getAvailableTypes(dimension);

    }

    @Override
    public TreeSet<Integer> getHeightTree(ResourceKey<Level> dimension, MapType type) {
        return instance == null ? new TreeSet<>() : instance.getHeightTree(dimension, type);
    }

    @Override
    public List<MapDataHolder> selectSection(Slice slice) {
        return instance == null ? List.of() : instance.selectSection(slice);

    }

    @Override
    public List<MapDataHolder> filterSection(Slice slice, Predicate<MapItemSavedData> predicate) {
        return instance == null ? List.of() : instance.filterSection(slice, predicate);
    }

    @Override
    public MapDataHolder select(MapKey key) {
        return instance == null ? null : instance.select(key);
    }

    @Override
    public @Nullable MapDataHolder getClosest(double x, double z, Slice slice) {
        return instance == null ? null : instance.getClosest(x, z, slice);
    }

    @Override
    public List<MapDataHolder> getAll() {
        return instance == null ? List.of() : instance.getAll();
    }

    @Override
    public void addNotSynced(Level level) {
        if (instance != null) {
            instance.addNotSynced(level);
        }
    }

    @Override
    public boolean hasOneSlice() {
        return instance != null && instance.hasOneSlice();
    }


}
