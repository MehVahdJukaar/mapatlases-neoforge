package pepjebs.mapatlases.integration;

import net.minecraft.server.level.ColumnPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import twilightforest.TFMagicMapData;
import twilightforest.TFMazeMapData;
import twilightforest.item.MagicMapItem;
import twilightforest.item.MazeMapItem;

public class TwilightForestCompat {

    public static MapItemSavedData getMagic(Level level, String name) {
        return TFMagicMapData.getMagicMapData(level, name);
    }

    public static MapItemSavedData getMaze(Level level, String name) {
        return TFMazeMapData.getMazeMapData(level, name);
    }

    public static ItemStack makeMagic(int destX, int destZ, byte scale, Level level) {
        return MagicMapItem.setupNewMap(level, destX, destZ,
                scale, true, false);
    }

    public static ItemStack makeMaze(int destX, int destZ, byte scale, Level level, int height) {
        return MazeMapItem.setupNewMap(level, destX, destZ,
                scale, true, false, height, false);
    }

    public static ItemStack makeOre(int destX, int destZ, byte scale, Level level, int height) {
        return MazeMapItem.setupNewMap(level, destX, destZ,
                scale, true, false, height, true);
    }

    public static ColumnPos getMagicMapCenter(int px, int pz) {
        return MagicMapItem.getMagicMapCenter(px, pz);
    }
}
