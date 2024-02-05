package pepjebs.mapatlases.capabilities;

import com.mojang.datafixers.util.Pair;
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
public record MapCollectionCap(IMapCollection instance) {

    public boolean add(int mapId, Level level) {
        return instance.add(mapId, level);
    }

    public boolean remove(MapDataHolder obj) {
        return false;
    }

    public int getCount() {
        return instance.getCount();
    }

    public boolean isEmpty() {
        return instance.isEmpty();
    }

    public byte getScale() {
        return instance.getScale();
    }

    public int[] getAllIds() {
        return instance.getAllIds();
    }

    public Collection<ResourceKey<Level>> getAvailableDimensions() {
        return instance.getAvailableDimensions();
    }

    public Collection<MapType> getAvailableTypes(ResourceKey<Level> dimension) {
        return instance.getAvailableTypes(dimension);
    }

    public TreeSet<Integer> getHeightTree(ResourceKey<Level> dimension, MapType type) {
        return instance.getHeightTree(dimension, type);
    }

    public List<MapDataHolder> selectSection(Slice slice) {
        return instance.selectSection(slice);
    }

    public List<MapDataHolder> filterSection(Slice slice, Predicate<MapItemSavedData> predicate) {
        return instance.filterSection(slice, predicate);
    }

    public Pair<String, MapItemSavedData> select(MapKey key) {
        MapDataHolder select = instance.select(key);
        if (select == null) return null;
        return Pair.of(select.stringId, select.data);
    }

    public @Nullable MapDataHolder select2(MapKey key) {
        return instance.select(key);
    }

    public @Nullable MapDataHolder getClosest(double x, double z, Slice slice) {
        return instance.getClosest(x, z, slice);
    }

    public List<MapDataHolder> getAll() {
        return instance.getAll();
    }

    public boolean hasOneSlice() {
        return instance.hasOneSlice();
    }
}
