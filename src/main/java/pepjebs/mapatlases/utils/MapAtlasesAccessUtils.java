package pepjebs.mapatlases.utils;

import com.mojang.datafixers.util.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.CuriosCompat;
import pepjebs.mapatlases.integration.TrinketsCompat;
import pepjebs.mapatlases.item.MapAtlasItem;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MapAtlasesAccessUtils {


    public static ItemStack createMapItemStackFromId(int id) {
        ItemStack map = new ItemStack(Items.FILLED_MAP);
        map.getOrCreateTag().putInt("map", id);
        return map;
    }

    public static int getMapIntFromString(String id) {
        if (id == null) {
            MapAtlasesMod.LOGGER.error("Encountered null id when fetching map name. Env: {}", FMLEnvironment.dist);
            return 0;
        }
        return Integer.parseInt(id.substring(4));
    }

    @NotNull
    private static ItemStack getAtlasFromInventory(Inventory inventory, boolean onlyHotbar) {
        int max = onlyHotbar ? 9 : inventory.getContainerSize();
        for (int i = 0; i < max; ++i) {
            ItemStack itemstack = inventory.getItem(i);
            if (itemstack.is(MapAtlasesMod.MAP_ATLAS.get())) {
                return itemstack;
            }
        }
        return ItemStack.EMPTY;
    }

    @NotNull
    public static ItemStack getAtlasFromPlayerByConfig(Player player) {
        Inventory inventory = player.getInventory();
        var loc = MapAtlasesConfig.activationLocation.get();
        // first scan hand
        ItemStack itemStack = player.getMainHandItem();
        if (itemStack.is(MapAtlasesMod.MAP_ATLAS.get())) {
            return itemStack;
        }
        // then offhand
        if (loc.hasOffhand()) {
            itemStack = player.getOffhandItem();
            if (itemStack.is(MapAtlasesMod.MAP_ATLAS.get())) {
                return itemStack;
            }
        }
        //then curios
        if (MapAtlasesMod.CURIOS) {
            itemStack = CuriosCompat.getAtlasInCurio(player);
            if (!itemStack.isEmpty()) return itemStack;
        }
        if (MapAtlasesMod.TRINKETS) {
            itemStack = TrinketsCompat.getAtlasInTrinket(player);
            if (!itemStack.isEmpty()) return itemStack;
        }
        if (loc.scanAll()) {
            itemStack = getAtlasFromInventory(inventory, false);
        } else if (loc.hasHotbar()) {
            itemStack = getAtlasFromInventory(inventory, true);
        }
        return itemStack;
    }


    public static int getMapCountToAdd(ItemStack atlas, ItemStack bottomItem, Level level) {
        int amountToAdd = bottomItem.getCount();
        int existingMapCount = MapAtlasItem.getMaps(atlas, level).getCount() + MapAtlasItem.getEmptyMaps(atlas);
        amountToAdd *= MapAtlasesConfig.mapEntryValueMultiplier.get();
        if (MapAtlasItem.getMaxMapCount() != -1
                && existingMapCount + bottomItem.getCount() > MapAtlasItem.getMaxMapCount()) {
            amountToAdd = MapAtlasItem.getMaxMapCount() - existingMapCount;
        }
        return amountToAdd;
    }



    // KEEP NAME
    @Deprecated(forRemoval = true)
    public static Map<String, MapItemSavedData> getAllMapInfoFromAtlas(Level level, ItemStack atlas) {
        if (atlas.getTag() == null) return new HashMap<>();
        int[] mapIds = Arrays.stream(atlas.getTag().getIntArray("a")).toArray();
        Map<String, MapItemSavedData> mapStates = new HashMap<>();
        for (int mapId : mapIds) {
            String mapName = MapItem.makeKey(mapId);
            MapItemSavedData state = level.getMapData(mapName);
            if (state == null && level instanceof ServerLevel) {
                ItemStack map = createMapItemStackFromId(mapId);
                state = MapItem.getSavedData(map, level);
            }
            if (state != null) {
                mapStates.put(mapName, state);
            }
        }
        return mapStates;
    }

    // KEEP NAME
    @Deprecated(forRemoval = true)
    public static Map.Entry<String, MapItemSavedData> getActiveAtlasMapStateServer(
            Map<String, MapItemSavedData> currentDimMapInfos,
            ServerPlayer player) {
        return null;
    }

}
