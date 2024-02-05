package pepjebs.mapatlases.map_collection.fabric;

import dev.onyxstudios.cca.api.v3.item.ItemComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.capabilities.MapKey;
import pepjebs.mapatlases.map_collection.IMapCollection;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.MapType;
import pepjebs.mapatlases.utils.Slice;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Predicate;

// Proxy class
public class IMapCollectionImpl extends ItemComponent implements IMapCollection {
    private static final String COMPONENT_KEY = "map_collection";

    @Nullable
    private MapCollection instance = null;

    public IMapCollectionImpl(ItemStack stack) {
        super(stack);
    }

    public static IMapCollection get(ItemStack stack, Level level) {
        try {
            Optional<IMapCollectionImpl> resolve = CCStuff.MAP_COLLECTION_COMPONENT.maybeGet(stack);

            if (resolve.isEmpty()) {
                throw new AssertionError("Map Atlas cca was empty. How is this possible? Culprit itemstack " + stack);
            }
            IMapCollectionImpl cap = resolve.get();
            return cap.getOrCreateInstance(level);
        } catch (Exception e) {
            throw new AssertionError("Cardinal Component for Map Atlases could not be gathered." +
                    "This is a Cardinal Component bug! ", e);
        }

    }

    protected IMapCollection getOrCreateInstance(Level level) {
        if (instance == null) {
            instance = new MapCollection();
            instance.deserializeNBT(this.getCompound(COMPONENT_KEY));
            instance.initialize(level);
        }
        return this;
    }


    @Override
    public void onTagInvalidated() {
        super.onTagInvalidated();
        instance = null;
    }

    private void markDirty() {
        if (instance != null) {
            this.putCompound(COMPONENT_KEY, instance.serializeNBT());
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
    public boolean hasOneSlice() {
        return instance != null && instance.hasOneSlice();
    }


}
