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
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.CuriosCompat;
import pepjebs.mapatlases.integration.TrinketsCompat;
import pepjebs.mapatlases.item.MapAtlasItem;

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
            MapAtlasesMod.LOGGER.error("Encountered null id when fetching map name. Env: {}", FMLEnvironment.dist);
            return 0;
        }
        return Integer.parseInt(id.substring(4));
    }

    // KEEP NAME
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

    // KEEP NAME
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
        if (!(atlas.getItem() instanceof MapAtlasItem)) {
            throw new InvalidParameterException("Given ItemStack was not an Atlas");
        }
        var mapState = getFirstMapItemSavedDataFromAtlas(world, atlas);
        return (1 << mapState.scale) * 128;
    }
}
