package pepjebs.mapatlases.integration;

import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.supplementaries.common.items.AntiqueInkItem;
import net.mehvahdjukaar.supplementaries.common.misc.map_data.DepthDataHandler;
import net.mehvahdjukaar.supplementaries.common.misc.map_data.MapLightHandler;
import net.mehvahdjukaar.supplementaries.common.misc.map_data.WeatheredHandler;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.NotNull;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.utils.AtlasMap;
import pepjebs.mapatlases.utils.MapType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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

    public static int getSliceReach() {
        return (int) (DepthDataHandler.getRangeMultiplier() * 128);
    }

    public static boolean canPlayerSeeDeathMarker(Player p) {
        return false;// TODO  !AtlasLookup.getAtlasFromPlayerByConfig(p).isEmpty();
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

    // swaps every non antique map for an antique copy. server only
    public static void convertAllMapsToAntique(ItemStack atlas, Level level) {
        MapCollection maps = MapAtlasItem.getMaps(atlas, level);
        List<AtlasMap> replaced = new ArrayList<>();
        Map<MapType, List<MapId>> antiqueIds = new EnumMap<>(MapType.class);
        for (AtlasMap holder : maps.getAllFound()) {
            if (WeatheredHandler.getAntiqueData(holder.data).isAntique()) continue;
            MapId antiqueId = WeatheredHandler.createAntiqueMapData(holder.data, level, true, false);
            if (antiqueId == null) continue;
            replaced.add(holder);
            antiqueIds.computeIfAbsent(holder.type, t -> new ArrayList<>()).add(antiqueId);
        }
        if (!replaced.isEmpty()) {
            atlas.set(MapAtlasesMod.MAP_COLLECTION.get(), maps.getIds().minus(replaced).plus(antiqueIds));
        }
    }
}
