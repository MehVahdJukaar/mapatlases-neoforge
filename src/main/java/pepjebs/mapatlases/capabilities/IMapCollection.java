package pepjebs.mapatlases.capabilities;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.utils.Slice;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public interface IMapCollection {

    boolean add(int mapId, Level level );


    @Nullable
    Pair<String, MapItemSavedData> remove(String mapName);

    int getCount();

    boolean isEmpty();

    byte getScale();

    int[] getAllIds();

    Collection<ResourceKey<Level>> getAvailableDimensions();

    Collection<Slice.Type> getAvailableSlices(ResourceKey<Level> dimension);

    Collection<Integer> getHeightTree(ResourceKey<Level> dimension, Slice.Type type);

    List<Pair<String, MapItemSavedData>> selectSection(ResourceKey<Level> dimension, Slice slice);

    List<Pair<String, MapItemSavedData>> filterSection(ResourceKey<Level> dimension, Slice slice,
                                                       Predicate<MapItemSavedData> predicate);

    @Nullable
    default Pair<String, MapItemSavedData> select(int x, int z, ResourceKey<Level> dimension, Slice slice) {
        return select(new MapKey(dimension, x, z, slice));
    }

    Pair<String, MapItemSavedData> select(MapKey key);


    @Nullable
    Pair<String, MapItemSavedData> getClosest(double x, double z, ResourceKey<Level> dimension, Slice slice);

    @Nullable
    default Pair<String, MapItemSavedData> getClosest(Player player, Slice group) {
        return getClosest(player.getX(), player.getZ(), player.level().dimension(), group);
    }

    Collection<Pair<String, MapItemSavedData>> getAll();


}
