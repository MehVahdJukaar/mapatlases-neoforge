package pepjebs.mapatlases.utils;

import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.mixin.plugin.MapAtlasesMixinPlugin;

import java.security.InvalidParameterException;
import java.util.*;
import java.util.stream.Collectors;

public class MapAtlasesAccessUtils {

    public static MapState getFirstMapStateFromAtlas(World world, ItemStack atlas) {
        return getMapStateByIndexFromAtlas(world, atlas, 0);
    }

    public static MapState getMapStateByIndexFromAtlas(World world, ItemStack atlas, int i) {
        if (atlas.getNbt() == null) return null;
        int[] mapIds = Arrays.stream(atlas.getNbt().getIntArray(MapAtlasItem.MAP_LIST_NBT)).toArray();
        if (i < 0 || i >= mapIds.length) return null;
        ItemStack map = createMapItemStackFromId(mapIds[i]);
        return FilledMapItem.getMapState(FilledMapItem.getMapId(map), world);
    }

    public static ItemStack createMapItemStackFromId(int id) {
        ItemStack map = new ItemStack(Items.FILLED_MAP);
        NbtCompound tag = new NbtCompound();
        tag.putInt("map", id);
        map.setNbt(tag);
        return map;
    }

    public static String getMapStringFromInt(int i) {
        return "map_" + i;
    }

    public static int getMapIntFromString(String id) {
        if (id == null) {
            MapAtlasesMod.LOGGER.error("Encountered null id when fetching map name. Env: "
                    + FabricLoader.getInstance().getEnvironmentType());
            return 0;
        }
        return Integer.parseInt(id.substring(4));
    }

    public static Map<String, MapState> getAllMapInfoFromAtlas(World world, ItemStack atlas) {
        if (atlas.getNbt() == null) return new HashMap<>();
        int[] mapIds = Arrays.stream(atlas.getNbt().getIntArray(MapAtlasItem.MAP_LIST_NBT)).toArray();
        Map<String, MapState> mapStates = new HashMap<>();
        for (int mapId : mapIds) {
            String mapName = FilledMapItem.getMapName(mapId);
            MapState state = world.getMapState(mapName);
            if (state == null && world instanceof ServerWorld) {
                ItemStack map = createMapItemStackFromId(mapId);
                state = FilledMapItem.getMapState(map, world);
            }
            if (state != null) {
                mapStates.put(mapName, state);
            }
        }
        return mapStates;
    }

    public static Map<String, MapState> getCurrentDimMapInfoFromAtlas(World world, ItemStack atlas) {
        return getAllMapInfoFromAtlas(world, atlas)
                .entrySet()
                .stream()
                .filter(state -> state.getValue().dimension.getValue().compareTo(world.getRegistryKey().getValue()) == 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static ItemStack getAtlasFromInventory(PlayerInventory inventory) {
        return inventory.main.stream()
                .filter(i -> i != null && i.isOf(MapAtlasesMod.MAP_ATLAS))
                .findFirst().orElse(null);
    }

    public static ItemStack getAtlasFromPlayerByConfig(PlayerEntity entity) {
        PlayerInventory inventory = entity.getInventory();
        ItemStack itemStack =  inventory.main.stream()
                .limit(9)
                .filter(i -> i != null && i.isOf(MapAtlasesMod.MAP_ATLAS))
                .findFirst().orElse(null);

        if (MapAtlasesMod.CONFIG != null) {
            if(MapAtlasesMod.CONFIG.activationLocation.equals("INVENTORY")) {
                itemStack =  getAtlasFromInventory(inventory);
            } else if (MapAtlasesMod.CONFIG.activationLocation.equals("HANDS")) {
                itemStack = null;
                ItemStack mainHand = inventory.main.get(inventory.selectedSlot);
                if (mainHand.getItem() == MapAtlasesMod.MAP_ATLAS)
                    itemStack = mainHand;
            }
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

    public static List<ItemStack> getItemStacksFromGrid(RecipeInputInventory inv) {
        List<ItemStack> itemStacks = new ArrayList<>();
        for(int i = 0; i < inv.size(); i++) {
            if (!inv.getStack(i).isEmpty()) {
                itemStacks.add(inv.getStack(i).copy());
            }
        }
        return itemStacks;
    }

    public static String getPlayerDimKey(PlayerEntity player) {
        return player.getWorld().getRegistryKey().getValue().toString();
    }

    public static String getMapStateDimKey(MapState state) {
        return state.dimension.getValue().toString();
    }

    public static double distanceBetweenMapStateAndPlayer(
            MapState mapState,
            PlayerEntity player
    ) {
        return Math.hypot(Math.abs(mapState.centerX - player.getX()),Math.abs(mapState.centerZ - player.getZ()));
    }

    public static Map.Entry<String, MapState> getActiveAtlasMapStateServer(
            Map<String, MapState> currentDimMapInfos,
            ServerPlayerEntity player) {
        Map.Entry<String, MapState> minDistState = null;
        for (Map.Entry<String, MapState> state : currentDimMapInfos.entrySet()) {
            if (minDistState == null) {
                minDistState = state;
                continue;
            }
            if (distanceBetweenMapStateAndPlayer(minDistState.getValue(), player) >
                    distanceBetweenMapStateAndPlayer(state.getValue(), player)) {
                minDistState = state;
            }
        }
        return minDistState;
    }

    public static int getEmptyMapCountFromItemStack(ItemStack atlas) {
        NbtCompound tag = atlas.getNbt();
        return tag != null && tag.contains(MapAtlasItem.EMPTY_MAP_NBT) ? tag.getInt(MapAtlasItem.EMPTY_MAP_NBT) : 0;
    }

    public static int[] getMapIdsFromItemStack(ItemStack atlas) {
        NbtCompound tag = atlas.getNbt();
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
        if (MapAtlasesMod.CONFIG != null) {
            amountToAdd *= MapAtlasesMod.CONFIG.mapEntryValueMultiplier;
        }
        if (MapAtlasItem.getMaxMapCount() != -1
                && existingMapCount + bottomItem.getCount() > MapAtlasItem.getMaxMapCount()) {
            amountToAdd = MapAtlasItem.getMaxMapCount() - existingMapCount;
        }
        return amountToAdd;
    }

    public static int getAtlasBlockScale(World world, ItemStack atlas) {
        if (world == null) {
            throw new InvalidParameterException("Given World was null");
        }
        if (atlas.getItem() != MapAtlasesMod.MAP_ATLAS) {
            throw new InvalidParameterException("Given ItemStack was not an Atlas");
        }
        var mapState = getFirstMapStateFromAtlas(world, atlas);
        return (1 << mapState.scale) * 128;
    }
}
