package pepjebs.mapatlases.integration;

import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.supplementaries.common.items.AntiqueInkItem;
import net.mehvahdjukaar.supplementaries.common.misc.map_data.DepthDataHandler;
import net.mehvahdjukaar.supplementaries.common.misc.map_data.MapLightHandler;
import net.mehvahdjukaar.supplementaries.common.misc.map_data.WeatheredHandler;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class SupplementariesCompat {

    public static void init() {
        if (PlatHelper.getPhysicalSide().isClient()) {
            SupplementariesClientCompat.init();
        }
        // turn on map light
        MapLightHandler.setActive(true);
    }

    public static Optional<Integer> getSlice(@NotNull MapItemSavedData data) {
        return DepthDataHandler.getMapHeight(data);
    }

    public static ItemStack createSliced(Level level, int destX, int destZ, byte scale, boolean b, boolean b1, Integer slice) {
        return DepthDataHandler.createSliceMap(level, destX, destZ, scale, b, b1, slice);
    }

    public static ItemStack createExistingSliced(MapId id) {
        ItemStack stack = new ItemStack(ModRegistry.SLICE_MAP.get());
        stack.set(DataComponents.MAP_ID, id);
        return stack;
    }

    public static int getSliceReach() {
        return (int) (DepthDataHandler.getRangeMultiplier() * 128);
    }

    public static boolean canPlayerSeeDeathMarker(Player p) {
        return false;// TODO  !MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(p).isEmpty();
    }

    public static boolean hasAntiqueInk(ItemStack itemstack) {
        return AntiqueInkItem.hasAntiqueInk(itemstack);
    }

    public static void setAntiqueInk(ItemStack stacks) {
        AntiqueInkItem.setAntiqueInk(stacks, true);
    }

    public static void setMapAntique(ItemStack newMap, Level level) {
        WeatheredHandler.setAntique(level, newMap, true);
    }

    public static boolean isAntiqueInk(ItemStack itemstack) {
        return itemstack.is(ModRegistry.ANTIQUE_INK.get());
    }

    public static MapId createAntiqueMapData(MapItemSavedData data, Level level, boolean on, boolean replaceOld) {
        return WeatheredHandler.createAntiqueMapData(data, level, on, replaceOld);
    }
}
