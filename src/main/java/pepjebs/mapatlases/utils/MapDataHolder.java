package pepjebs.mapatlases.utils;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.capabilities.MapKey;

public class MapDataHolder {
    public final int id;
    public final String stringId;
    public final MapItemSavedData data;

    //redundant info, cache basically as we use this for data structures
    public final Slice slice;
    public final MapType type;
    @Nullable
    public final Integer height;

    public MapDataHolder(String name, MapItemSavedData data) {
        this(findMapIntFromString(name), name, data);
    }

    private MapDataHolder(int id, String stringId, MapItemSavedData data) {
        this.id = id;
        this.stringId = stringId;
        this.data = data;
        this.type = MapType.fromKey(stringId, data);
        this.height = type.getHeight(data);
        this.slice = Slice.of(type, height, data.dimension);
    }

    private static int findMapIntFromString(String id) {
        return Integer.parseInt(id.split("_")[1]);
    }

    @Nullable
    public static MapDataHolder findFromId(Level level, int id) {
        //try all known types
        for (var t : MapType.values()) {
            var d = t.getMapData(level, id);
            if (d != null) {
                return new MapDataHolder(id, d.getFirst(), d.getSecond());
            }
        }
        return null;
    }

    public MapKey makeKey() {
        return MapKey.at(data.scale, data.centerX, data.centerZ, slice);
    }

    public void updateMap(ServerPlayer player) {
        ((MapItem) type.filled).update(player.level(), player, data);
    }

    //utility methods. merge with slice TODO

}
