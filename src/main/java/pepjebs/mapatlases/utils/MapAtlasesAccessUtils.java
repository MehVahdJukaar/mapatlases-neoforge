package pepjebs.mapatlases.utils;

import com.mojang.datafixers.util.Pair;
import net.mehvahdjukaar.moonlight.api.map.CustomMapData;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.NotNull;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.capabilities.MapCollectionCap;
import pepjebs.mapatlases.capabilities.MapKey;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.CuriosCompat;
import pepjebs.mapatlases.integration.TrinketsCompat;
import pepjebs.mapatlases.item.MapAtlasItem;

import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MapAtlasesAccessUtils {



    public static boolean isValidFilledMap(ItemStack item) {
        return  Slice.Type.fromItem(item.getItem()) != null;
    }

    public static Pair<String, MapItemSavedData> findMapFromId(Level level, int id) {
        //try all known types
        for (var t : Slice.Type.values()) {
            var d = t.getMapData(level, id);
            if (d != null) return d;
        }
        return null;
    }

    public static Pair<String, MapItemSavedData> findMapFromItemStack(Level level, ItemStack item) {
        return findMapFromId(level, MapItem.getMapId(item));
    }

    public static int findMapIntFromString(String id) {
        return Integer.parseInt(id.split("_")[1]);
        /*
        for (var t : Slice.Type.values()) {
            var i = t.findKey(id);
            if (i != null) return i;
        }
        throw new IllegalStateException("Unable to find map id for key " + id);
         */
    }

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
        return MapAtlasItem.getMaps(atlas, level).getAll().stream().collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
    }

    // KEEP NAME
    @Deprecated(forRemoval = true)
    public static Map.Entry<String, MapItemSavedData> getActiveAtlasMapStateServer(
            Map<String, MapItemSavedData> currentDimMapInfos,
            ServerPlayer player) {
        ItemStack atlas = getAtlasFromPlayerByConfig(player);
        var a = getActiveStateServer(atlas, player);
        return new AbstractMap.SimpleEntry<>(a.getFirst(), a.getSecond());
    }

    public static Pair<String, MapItemSavedData> getActiveStateServer(ItemStack stack, Player player) {
        var slice = MapAtlasItem.getSelectedSlice(stack, player.level.dimension());
        MapCollectionCap maps = MapAtlasItem.getMaps(stack, player.level);
        return maps.select(MapKey.at(maps.getScale(), player, slice));
    }


    public static void updateMapDataAndSync(Pair<String, MapItemSavedData> mapInfo, ServerPlayer player, ItemStack atlas) {
        updateMapDataAndSync(mapInfo.getSecond(), findMapIntFromString(mapInfo.getFirst()), player, atlas);
    }

    public static void updateMapDataAndSync(
            MapItemSavedData data,
            int mapId,
            ServerPlayer player,
            ItemStack atlas
    ) {
        MapAtlasesMod.setMapInInentoryHack(true);
        data.tickCarriedBy(player, atlas);
        MapAtlasesAccessUtils.syncMapDataToClient(data, mapId, player);
        MapAtlasesMod.setMapInInentoryHack(false);
    }

    public static void syncMapDataToClient(MapItemSavedData data, int id, ServerPlayer player) {
        //ok so hear me out. we use this to send new map data to the client when needed. thing is this packet isnt enough on its own
        // i need it for another mod so i'm using some code in moonlight which upgrades it to send center and dimension too (as well as custom colors)
        //TODO: maybe use isComplex  update packet and inventory tick
        Packet<?> p = data.getUpdatePacket(id, player);
        if (p != null) {
            if (MapAtlasesMod.MOONLIGHT) {
                player.connection.send(p);
            } else if (p instanceof ClientboundMapItemDataPacket pp) {
                //send crappy wrapper if we dont.
                MapAtlasesNetowrking.sendToClientPlayer(player, new S2CMapPacketWrapper(data, pp));
            }
        }
    }

}
