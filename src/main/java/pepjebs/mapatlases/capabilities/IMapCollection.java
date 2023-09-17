package pepjebs.mapatlases.capabilities;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;

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

    Collection<ResourceKey<Level>> getAvailableDimensions();

    Collection<Integer> getAvailableSlices(ResourceKey<Level> dimension);

    List<Pair<String, MapItemSavedData>> selectSection(ResourceKey<Level> dimension, @Nullable Integer slice);

    List<Pair<String, MapItemSavedData>> filterSection(ResourceKey<Level> dimension, @Nullable Integer slice,
                                                       Predicate<MapItemSavedData> predicate);

    @Nullable
    default Pair<String, MapItemSavedData> select(int x, int z, ResourceKey<Level> dimension, @Nullable Integer slice) {
        return select(new MapKey(dimension, x, z, slice));
    }

    Pair<String, MapItemSavedData> select(MapKey key);


    @Nullable
    Pair<String, MapItemSavedData> getClosest(double x, double z, ResourceKey<Level> dimension, @Nullable Integer slice);

    @Nullable
    default Pair<String, MapItemSavedData> getClosest(Player player, @Nullable Integer slice) {
        return getClosest(player.getX(), player.getZ(), player.level.dimension(), slice);
    }

    Collection<Pair<String, MapItemSavedData>> getAll();


}
