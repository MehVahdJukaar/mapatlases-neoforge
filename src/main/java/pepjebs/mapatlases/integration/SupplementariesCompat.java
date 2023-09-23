package pepjebs.mapatlases.integration;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class SupplementariesCompat {

    public static Integer getSlice(MapItemSavedData data) {
        return null;
        //int i = SliceMapItem.getMapHeight(data);
        //return i == Integer.MAX_VALUE ? null : i;
    }

    public static ItemStack createSliced(Level level, int destX, int destZ, byte scale, boolean b, boolean b1, Integer slice) {
       // return SliceMapItem.createSliced(level, destX, destZ, scale, b, b1, slice);
return null;
    }

    public static int getSliceReach() {
        return (int) (SliceMapItem.getRangeMultiplier() * 128);
    }
}
