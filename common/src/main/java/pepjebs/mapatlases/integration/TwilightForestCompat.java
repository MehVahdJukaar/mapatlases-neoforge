package pepjebs.mapatlases.integration;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import twilightforest.TFMagicMapData;
import twilightforest.TFMazeMapData;
import twilightforest.item.MagicMapItem;
import twilightforest.item.MazeMapItem;

public class TwilightForestCompat {

    private static final Supplier<Item> FILLED_MAGIC = Suppliers.memoize(()->
            BuiltInRegistries.ITEM.get(new ResourceLocation("twilightforest", "filled_magic_map")));

    private static final Supplier<Item> FILLED_MAZE = Suppliers.memoize(()->
            BuiltInRegistries.ITEM.get(new ResourceLocation("twilightforest", "filled_maze_map")));

    private static final Supplier<Item> FILLED_ORE = Suppliers.memoize(()->
            BuiltInRegistries.ITEM.get(new ResourceLocation("twilightforest", "filled_ore_map")));

    public static MapItemSavedData getMagic(Level level, String name) {
        return TFMagicMapData.getMagicMapData(level, name);
    }

    public static MapItemSavedData getMaze(Level level, String name) {
        return TFMazeMapData.getMazeMapData(level, name);
    }

    public static ItemStack makeExistingMagic(int id) {
        ItemStack stack = new ItemStack(FILLED_MAGIC.get());
        stack.getOrCreateTag().putInt("map", id);
        return stack;
    }

    public static ItemStack makeExistingMaze(int id) {
        ItemStack stack = new ItemStack(FILLED_MAZE.get());
        stack.getOrCreateTag().putInt("map", id);
        return stack;
    }

    public static ItemStack makeExistingOre(int id) {
        ItemStack stack = new ItemStack(FILLED_ORE.get());
        stack.getOrCreateTag().putInt("map", id);
        return stack;
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
