package pepjebs.mapatlases.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.NotNull;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.CuriosCompat;
import pepjebs.mapatlases.integration.TrinketsCompat;
import pepjebs.mapatlases.item.MapAtlasItem;

import java.security.InvalidParameterException;
import java.util.*;
import java.util.stream.Collectors;

@Deprecated(forRemoval = true)
public class MapAtlasesAccessUtilsOld {

    public static ItemStack createMapItemStackFromId(int id) {
        ItemStack map = new ItemStack(Items.FILLED_MAP);
        map.getOrCreateTag().putInt("map", id);
        return map;
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

    public static List<ItemStack> getItemStacksFromGrid(CraftingContainer inv) {
        List<ItemStack> itemStacks = new ArrayList<>();
        for (var i : inv.getItems()) {
            if (!i.isEmpty()) {
                itemStacks.add(i.copy());
            }
        }
        return itemStacks;
    }

    @Deprecated(forRemoval = true)
    public static String getMapItemSavedDataDimKey(MapItemSavedData state) {
        return state.dimension.location().toString();
    }

    public static double distanceBetweenMapItemSavedDataAndPlayer(
            MapItemSavedData mapState,
            Player player
    ) {
        return Math.hypot(Math.abs(mapState.centerX - player.getX()), Math.abs(mapState.centerZ - player.getZ()));
    }

    // KEEP NAME
    //TODO: mae one per dimension
    public static Map.Entry<String, MapItemSavedData> getActiveAtlasMapStateServer(
            Map<String, MapItemSavedData> currentDimMapInfos,
            ServerPlayer player) {
        Map.Entry<String, MapItemSavedData> minDistState = null;
        for (Map.Entry<String, MapItemSavedData> state : currentDimMapInfos.entrySet()) {
            if (minDistState == null) {
                minDistState = state;
                continue;
            }
            if (distanceBetweenMapItemSavedDataAndPlayer(minDistState.getValue(), player) >
                    distanceBetweenMapItemSavedDataAndPlayer(state.getValue(), player)) {
                minDistState = state;
            }
        }
        return minDistState;
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


}
