package pepjebs.mapatlases.integration;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.NotNull;

public class SupplementariesCompat {

    public static void init() {
    }

    public static Integer getSlice(@NotNull MapItemSavedData data) {
        return null;
    }

    public static ItemStack createSliced(Level level, int destX, int destZ, byte scale, boolean b, boolean b1, Integer slice) {
        return ItemStack.EMPTY;
    }

    public static ItemStack createExistingSliced(int id) {
        return ItemStack.EMPTY;
    }

    public static int getSliceReach() {
        return 128;
    }

    public static boolean canPlayerSeeDeathMarker(Player p) {
        return false;
    }

    public static boolean hasAntiqueInk(ItemStack itemstack) {
        return false;
    }

    public static void setAntiqueInk(ItemStack stacks) {
    }

    public static void setMapAntique(ItemStack newMap, Level level) {
    }

    public static boolean isAntiqueInk(ItemStack itemstack) {
        return false;
    }

    public static Integer createAntiqueMapData(MapItemSavedData data, Level level, boolean on, boolean replaceOld) {
        return null;
    }
}
