package pepjebs.mapatlases.utils;

import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

//old name of AtlasMap. moonlight and supplementaries still link against it
@Deprecated(forRemoval = true)
public class MapDataHolder extends AtlasMap {

    public MapDataHolder(MapId id, MapType type, MapItemSavedData data) {
        super(id, type, data);
    }
}
