package pepjebs.mapatlases.integration;

import net.mehvahdjukaar.supplementaries.common.items.SliceMap;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class SupplementariesCompat {

    public static Integer getSlice(MapItemSavedData data) {
        int i = SliceMap.getMapHeight(data);
        return i == Integer.MAX_VALUE ? null : i;
    }
}
