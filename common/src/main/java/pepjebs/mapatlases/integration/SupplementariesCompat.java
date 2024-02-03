package pepjebs.mapatlases.integration;

import net.mehvahdjukaar.moonlight.api.platform.PlatformHelper;
import net.mehvahdjukaar.supplementaries.common.items.SliceMapItem;
import net.mehvahdjukaar.supplementaries.common.misc.AntiqueInkHelper;
import net.mehvahdjukaar.supplementaries.common.misc.map_markers.WeatheredMap;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.utils.MapDataHolder;

public class SupplementariesCompat {

    public static void init() {
        if (PlatformHelper.getEnv().isClient()) {
            SupplementariesClientCompat.init();
        }
        // turn on map light
    }

    public static Integer getSlice(MapItemSavedData data) {
        int i = SliceMapItem.getMapHeight(data);
        return i == Integer.MAX_VALUE ? null : i;
    }

    public static ItemStack createSliced(Level level, int destX, int destZ, byte scale, boolean b, boolean b1, Integer slice) {
        return SliceMapItem.createSliced(level, destX, destZ, scale, b, b1, slice);

    }

    public static int getSliceReach() {
        return (int) (SliceMapItem.getRangeMultiplier() * 128);
    }

    public static boolean canPlayerSeeDeathMarker(Player p) {
        return false;// TODO  !MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(p).isEmpty();
    }

    public static boolean hasAntiqueInk(ItemStack itemstack) {
        return AntiqueInkHelper.hasAntiqueInk(itemstack);
    }

    public static void setAntiqueInk(ItemStack stacks) {
        AntiqueInkHelper.setAntiqueInk(stacks, true);
    }

    public static void setMapAntique(ItemStack newMap, Level level) {
        WeatheredMap.setAntique(level, newMap, true);
    }

    public static boolean isAntiqueInk(ItemStack itemstack) {
        return itemstack.is(ModRegistry.ANTIQUE_INK.get());
    }

    public static Integer createAntiqueMapData(MapDataHolder holder, Level level, boolean on, boolean replaceOld) {
        ItemStack dummy = Items.FILLED_MAP.getDefaultInstance();
        dummy.getOrCreateTag().putInt("map", holder.id);
        WeatheredMap.setAntique(level, dummy, on);
        MapItem.getSavedData(dummy, level);
        return holder.id;
    }
}
