package pepjebs.mapatlases.integration;

import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.supplementaries.common.items.SliceMapItem;
import net.mehvahdjukaar.supplementaries.common.misc.AntiqueInkHelper;
import net.mehvahdjukaar.supplementaries.common.misc.MapLightHandler;
import net.mehvahdjukaar.supplementaries.common.misc.map_markers.WeatheredMap;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class SupplementariesCompat {

    public static void init() {
        if (PlatHelper.getPhysicalSide().isClient()) {
            SupplementariesClientCompat.init();
        }
        // turn on map light
        MapLightHandler.setActive(true);
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

    public static Integer createAntiqueMapData(MapItemSavedData data, Level level, boolean on, boolean replaceOld) {
        return WeatheredMap.createAntiqueMapData(data, level, on, replaceOld);
    }
}
