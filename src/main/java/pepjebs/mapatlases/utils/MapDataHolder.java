package pepjebs.mapatlases.utils;

import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.capabilities.MapKey;

public class MapDataHolder {
    //TODO: replace pair<String,MapData> with this
    private final int id;
    private final String stringId;
    private final MapItemSavedData data;

    //lazy
    private MapKey key = null;


    public MapDataHolder(String name, MapItemSavedData data) {
        this.id = findMapIntFromString(name);
        this.data = data;
        this.stringId = key.slice().getMapString(id);
    }

    private MapDataHolder(int id, String stringId, MapItemSavedData data) {
        this.id = id;
        this.stringId = stringId;
        this.data = data;
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

    public int intId() {
        return id;
    }

    public String stringId() {
        return stringId;
    }

    public MapKey key() {
        if (key == null) {
            this.key = MapKey.of(Pair.of(stringId, data));
        }
        return key;
    }

    public MapItemSavedData data() {
        return data;
    }

    public void updateMap(ServerPlayer player) {
        ((MapItem) key.slice().type().filled).update(player.level(), player, data);
    }

    //utility methods. merge with slice

}
