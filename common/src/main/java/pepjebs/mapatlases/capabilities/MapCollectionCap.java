package pepjebs.mapatlases.capabilities;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.map_collection.IMapCollection;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.MapType;
import pepjebs.mapatlases.utils.Slice;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Predicate;

//backwards compat
public record MapCollectionCap(IMapCollection instance) implements IMapCollection {

    @Override
    public boolean add(int mapId, Level level) {
        return instance.add(mapId, level);
    }

    @Override
    public boolean remove(MapDataHolder obj) {
        return false;
    }

    @Override
    public int getCount() {
        return instance.getCount();
    }

    @Override
    public boolean isEmpty() {
        return instance.isEmpty();
    }

    @Override
    public byte getScale() {
        return instance.getScale();
    }

    @Override
    public int[] getAllIds() {
        return instance.getAllIds();
    }

    @Override
    public Collection<ResourceKey<Level>> getAvailableDimensions() {
        return instance.getAvailableDimensions();
    }

    @Override
    public Collection<MapType> getAvailableTypes(ResourceKey<Level> dimension) {
        return instance.getAvailableTypes(dimension);
    }

    @Override
    public TreeSet<Integer> getHeightTree(ResourceKey<Level> dimension, MapType type) {
        return instance.getHeightTree(dimension,type);
    }

    @Override
    public List<MapDataHolder> selectSection(Slice slice) {
        return instance.selectSection(slice);
    }

    @Override
    public List<MapDataHolder> filterSection(Slice slice, Predicate<MapItemSavedData> predicate) {
        return instance.filterSection(slice, predicate);
    }

    @Override
    public @Nullable MapDataHolder select(MapKey key) {
        return instance.select(key);
    }

    @Override
    public @Nullable MapDataHolder getClosest(double x, double z, Slice slice) {
        return instance.getClosest(x,z,slice);
    }

    @Override
    public List<MapDataHolder> getAll() {
        return instance.getAll();
    }

    @Override
    public boolean hasOneSlice() {
        return instance.hasOneSlice();
    }
}
