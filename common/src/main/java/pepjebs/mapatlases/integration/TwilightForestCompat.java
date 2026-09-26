package pepjebs.mapatlases.integration;

import net.minecraft.server.level.ColumnPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import twilightforest.item.MagicMapItem;
import twilightforest.item.MazeMapItem;
import twilightforest.item.mapdata.TFMagicMapData;
import twilightforest.item.mapdata.TFMazeMapData;

public class TwilightForestCompat {

    public static MapItemSavedData getMagic(Level level, MapId name) {
        return TFMagicMapData.getMagicMapData(level, MagicMapItem.getMapName(name.id()));
    }

    public static MapItemSavedData getMaze(Level level, MapId name) {
        return TFMazeMapData.getMazeMapData(level, MazeMapItem.getMapName(name.id()));
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

    public static Integer getSlice(MapItemSavedData data) {
        if (data instanceof TFMazeMapData d) {
            return d.yCenter;
        }
        return null;
    }

    public static boolean isMazeOre(MapItemSavedData data) {
        return data instanceof TFMazeMapData md && md.ore;
    }


}
