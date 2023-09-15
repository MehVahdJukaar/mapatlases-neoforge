package pepjebs.mapatlases.integration;

import net.mehvahdjukaar.supplementaries.common.items.SliceMapItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class SupplementariesCompat {

    public static Integer getSlice(MapItemSavedData data) {
        int i = SliceMapItem.getMapHeight(data);
        return i == Integer.MAX_VALUE ? null : i;
    }

    public static ItemStack createSliced(Level level, int destX, int destZ, byte scale, boolean b, boolean b1, Integer slice) {
        return  SliceMapItem.createSliced(level,destX,destZ,scale,b, b1, slice);

    }
}