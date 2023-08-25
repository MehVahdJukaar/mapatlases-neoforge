package pebjebs.mapatlases.utils;

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
import net.minecraftforge.fml.loading.FMLEnvironment;
import pebjebs.mapatlases.MapAtlasesMod;
import pebjebs.mapatlases.client.ActivationLocation;
import pebjebs.mapatlases.config.MapAtlasesClientConfig;
import pebjebs.mapatlases.config.MapAtlasesConfig;
import pebjebs.mapatlases.item.MapAtlasItem;
import pebjebs.mapatlases.mixin.plugin.MapAtlasesMixinPlugin;

import java.security.InvalidParameterException;
import java.util.*;
import java.util.stream.Collectors;

public class MapAtlasesAccessUtils {

    public static MapItemSavedData getFirstMapItemSavedDataFromAtlas(Level world, ItemStack atlas) {
        return getMapItemSavedDataByIndexFromAtlas(world, atlas, 0);
    }

    public static MapItemSavedData getMapItemSavedDataByIndexFromAtlas(Level world, ItemStack atlas, int i) {
        if (atlas.getTag() == null) return null;
        int[] mapIds = Arrays.stream(atlas.getTag().getIntArray(MapAtlasItem.MAP_LIST_NBT)).toArray();
        if (i < 0 || i >= mapIds.length) return null;
        ItemStack map = createMapItemStackFromId(mapIds[i]);
        return MapItem.getSavedData(MapItem.getMapId(map), world);
    }

    public static ItemStack createMapItemStackFromId(int id) {
        ItemStack map = new ItemStack(Items.FILLED_MAP);
        CompoundTag tag = new CompoundTag();
        tag.putInt("map", id);
        map.setTag(tag);
        return map;
    }

    public static String getMapStringFromInt(int i) {
        return "map_" + i;
    }

    public static int getMapIntFromString(String id) {
        if (id == null) {
            MapAtlasesMod.LOGGER.error("Encountered null id when fetching map name. Env: {}", FMLEnvironment.dist );
            return 0;
        }
        return Integer.parseInt(id.substring(4));
    }

    public static Map<String, MapItemSavedData> getAllMapInfoFromAtlas(Level world, ItemStack atlas) {
        if (atlas.getTag() == null) return new HashMap<>();
        int[] mapIds = Arrays.stream(atlas.getTag().getIntArray(MapAtlasItem.MAP_LIST_NBT)).toArray();
        Map<String, MapItemSavedData> mapStates = new HashMap<>();
        for (int mapId : mapIds) {
            String mapName = MapItem.makeKey(mapId);
            MapItemSavedData state = world.getMapData(mapName);
            if (state == null && world instanceof ServerLevel) {
                ItemStack map = createMapItemStackFromId(mapId);
                state = MapItem.getSavedData(map, world);
            }
            if (state != null) {
                mapStates.put(mapName, state);
            }
        }
        return mapStates;
    }

    public static Map<String, MapItemSavedData> getCurrentDimMapInfoFromAtlas(Level world, ItemStack atlas) {
        return getAllMapInfoFromAtlas(world, atlas)
                .entrySet()
                .stream()
                .filter(state -> state.getValue().dimension.location().compareTo(world.dimension().location()) == 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static ItemStack getAtlasFromInventory(Inventory inventory) {
        return inventory.items.stream()
                .filter(i -> i != null && i.is(MapAtlasesMod.MAP_ATLAS.get()))
                .findFirst().orElse(null);
    }

    public static ItemStack getAtlasFromPlayerByConfig(Player entity) {
        Inventory inventory = entity.getInventory();
        ItemStack itemStack = inventory.items.stream()
                .limit(9)
                .filter(i -> i != null && i.is(MapAtlasesMod.MAP_ATLAS.get()))
                .findFirst().orElse(null);

        var loc = MapAtlasesClientConfig.activationLocation.get();
        if (loc == ActivationLocation.INVENTORY) {
            itemStack = getAtlasFromInventory(inventory);
        } else if (loc == ActivationLocation.HANDS) {
            itemStack = null;
            ItemStack mainHand = inventory.items.get(inventory.selectedSlot);
            if (mainHand.getItem() == MapAtlasesMod.MAP_ATLAS)
                itemStack = mainHand;
        }
        if (itemStack == null && inventory.offHand.get(0).getItem() == MapAtlasesMod.MAP_ATLAS)
            itemStack = inventory.offHand.get(0);
        if (itemStack == null
                && MapAtlasesMixinPlugin.isTrinketsLoaded()
                && TrinketsApi.getTrinketComponent(entity).isPresent()
                && TrinketsApi.getTrinketComponent(entity).get().getEquipped(MapAtlasesMod.MAP_ATLAS).size() > 0) {
            itemStack = TrinketsApi.getTrinketComponent(entity)
                    .get().getEquipped(MapAtlasesMod.MAP_ATLAS).get(0).getRight();
        }
        return itemStack != null ? itemStack : ItemStack.EMPTY;
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

    public static String getPlayerDimKey(Player player) {
        return player.level().dimension().location().toString();
    }

    public static String getMapItemSavedDataDimKey(MapItemSavedData state) {
        return state.dimension.location().toString();
    }

    public static double distanceBetweenMapItemSavedDataAndPlayer(
            MapItemSavedData mapState,
            Player player
    ) {
        return Math.hypot(Math.abs(mapState.centerX - player.getX()), Math.abs(mapState.centerZ - player.getZ()));
    }

    public static Map.Entry<String, MapItemSavedData> getActiveAtlasMapItemSavedDataServer(
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

    public static int getEmptyMapCountFromItemStack(ItemStack atlas) {
        CompoundTag tag = atlas.getTag();
        return tag != null && tag.contains(MapAtlasItem.EMPTY_MAP_NBT) ? tag.getInt(MapAtlasItem.EMPTY_MAP_NBT) : 0;
    }

    public static int[] getMapIdsFromItemStack(ItemStack atlas) {
        CompoundTag tag = atlas.getTag();
        return tag != null && tag.contains(MapAtlasItem.MAP_LIST_NBT)
                ? tag.getIntArray(MapAtlasItem.MAP_LIST_NBT)
                : new int[]{};
    }

    public static int getMapCountFromItemStack(ItemStack atlas) {
        return getMapIdsFromItemStack(atlas).length;
    }

    public static int getMapCountToAdd(ItemStack atlas, ItemStack bottomItem) {
        int amountToAdd = bottomItem.getCount();
        int existingMapCount = MapAtlasesAccessUtils.getMapCountFromItemStack(atlas)
                + MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(atlas);
        amountToAdd *= MapAtlasesConfig.mapEntryValueMultiplier.get();
        if (MapAtlasItem.getMaxMapCount() != -1
                && existingMapCount + bottomItem.getCount() > MapAtlasItem.getMaxMapCount()) {
            amountToAdd = MapAtlasItem.getMaxMapCount() - existingMapCount;
        }
        return amountToAdd;
    }

    public static int getAtlasBlockScale(Level world, ItemStack atlas) {
        if (world == null) {
            throw new InvalidParameterException("Given Level was null");
        }
        if (atlas.getItem() != MapAtlasesMod.MAP_ATLAS) {
            throw new InvalidParameterException("Given ItemStack was not an Atlas");
        }
        var mapState = getFirstMapItemSavedDataFromAtlas(world, atlas);
        return (1 << mapState.scale) * 128;
    }
}
