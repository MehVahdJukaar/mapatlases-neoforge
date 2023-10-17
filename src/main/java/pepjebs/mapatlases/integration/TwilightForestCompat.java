package pepjebs.mapatlases.integration;

import net.minecraft.server.level.ColumnPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class TwilightForestCompat {

    public static MapItemSavedData getMagic(Level level, String name) {
        return null;
    }

    public static MapItemSavedData getMaze(Level level, String name) {
        return null;
    }

    public static ItemStack makeMagic(int destX, int destZ, byte scale, Level level) {
        return ItemStack.EMPTY;
    }

    public static ItemStack makeMaze(int destX, int destZ, byte scale, Level level, int height) {
        return ItemStack.EMPTY;
    }

    public static ItemStack makeOre(int destX, int destZ, byte scale, Level level, int height) {
        return ItemStack.EMPTY;
    }

    public static ColumnPos getMagicMapCenter(int px, int pz) {
        return null;
    }

    public static Integer getSlice(MapItemSavedData data) {

        return null;
    }

    public static boolean isMazeOre(MapItemSavedData data) {
        return data instanceof TFMazeMapData md && md.ore;
    }
}
