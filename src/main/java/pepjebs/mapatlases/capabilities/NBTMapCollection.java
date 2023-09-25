package pepjebs.mapatlases.capabilities;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

// for fabric. or use cardinal components. Less optimized as it deserializes the stuff every time but at least doesnt have syncing issues
public class NBTMapCollection implements IMapCollection {
    private final ItemStack atlas;

    public NBTMapCollection(ItemStack atlas){
        this.atlas = atlas;
    }

    @Override
    public boolean add(int mapId, Level level) {
        return false;
    }

    @Override
    public @Nullable Pair<String, MapItemSavedData> remove(String mapName) {
        return null;
    }

    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public byte getScale() {
        return 0;
    }

    @Override
    public int[] getAllIds() {
        return new int[0];
    }

    @Override
    public Collection<ResourceKey<Level>> getAvailableDimensions() {
        return null;
    }

    @Override
    public Collection<Integer> getAvailableSlices(ResourceKey<Level> dimension) {
        return null;
    }

    @Override
    public List<Pair<String, MapItemSavedData>> selectSection(ResourceKey<Level> dimension, @Nullable Integer slice) {
        return null;
    }

    @Override
    public List<Pair<String, MapItemSavedData>> filterSection(ResourceKey<Level> dimension, @Nullable Integer slice, Predicate<MapItemSavedData> predicate) {
        return null;
    }

    @Override
    public Pair<String, MapItemSavedData> select(MapKey key) {
        return null;
    }

    @Override
    public @Nullable Pair<String, MapItemSavedData> getClosest(double x, double z, ResourceKey<Level> dimension, @Nullable Integer slice) {
        return null;
    }

    @Override
    public Collection<Pair<String, MapItemSavedData>> getAll() {
        return null;
    }
}
