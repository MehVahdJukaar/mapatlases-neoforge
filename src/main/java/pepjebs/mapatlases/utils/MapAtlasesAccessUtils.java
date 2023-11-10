package pepjebs.mapatlases.utils;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.capabilities.MapCollectionCap;
import pepjebs.mapatlases.capabilities.MapKey;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.CuriosCompat;
import pepjebs.mapatlases.integration.TrinketsCompat;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.networking.MapAtlasesNetworking;
import pepjebs.mapatlases.networking.S2CMapPacketWrapper;

public class MapAtlasesAccessUtils {


    public static boolean isValidFilledMap(ItemStack item) {
        return MapType.fromItem(item.getItem()) != null;
    }

    public static boolean isValidEmptyMap(ItemStack item) {
        return MapType.isEmptyMap(item.getItem());
    }


    public static MapDataHolder findMapFromItemStack(Level level, ItemStack item) {
        return MapDataHolder.findFromId(level, MapItem.getMapId(item));
    }

    public static int findMapIntFromString(String id) {
        return Integer.parseInt(id.split("_")[1]);
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

    public static MapDataHolder getActiveStateServer(ItemStack stack, Player player) {
        var slice = MapAtlasItem.getSelectedSlice(stack, player.level().dimension());
        MapCollectionCap maps = MapAtlasItem.getMaps(stack, player.level());
        return maps.select(MapKey.at(maps.getScale(), player, slice));
    }

    public static void updateMapDataAndSync(
            MapDataHolder holder,
            ServerPlayer player,
            ItemStack atlas,
            boolean isBeingCarried
    ) {
        MapAtlasesMod.setMapInInventoryHack(isBeingCarried ? InteractionResult.SUCCESS : InteractionResult.FAIL);
        //hack. just to be sure so contains will fail
        holder.data.tickCarriedBy(player, isBeingCarried ? atlas : Items.MAP.getDefaultInstance());
        MapAtlasesAccessUtils.syncMapDataToClient(holder, player);
        if (isBeingCarried) MapAtlasesMod.setMapInInventoryHack(InteractionResult.PASS);
    }

    public static void syncMapDataToClient(MapDataHolder holder, ServerPlayer player) {
        //ok so hear me out. we use this to send new map holder to the client when needed. thing is this packet isnt enough on its own
        // i need it for another mod so i'm using some code in moonlight which upgrades it to send center and dimension too (as well as custom colors)
        //TODO: maybe use isComplex  update packet and inventory tick
        Packet<?> p = holder.data.getUpdatePacket(holder.id, player);
        if (p != null) {
            if (MapAtlasesMod.MOONLIGHT) {
                player.connection.send(p);
            } else if (p instanceof ClientboundMapItemDataPacket pp) {
                //send crappy wrapper if we dont.
                MapAtlasesNetworking.sendToClientPlayer(player, new S2CMapPacketWrapper(holder.data, pp));
            }
        }
    }

}
