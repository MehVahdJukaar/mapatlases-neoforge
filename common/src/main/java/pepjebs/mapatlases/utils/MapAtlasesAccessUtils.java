package pepjebs.mapatlases.utils;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.CuriosCompat;
import pepjebs.mapatlases.integration.TrinketsCompat;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.networking.MapAtlasesNetworking;
import pepjebs.mapatlases.networking.S2CMapPacketWrapper;

public class MapAtlasesAccessUtils {


    public static boolean isValidFilledMap(ItemStack item) {
        return MapType.fromItem(item.getItem()) != null && getMapId(item) != null;
    }

    public static boolean isValidEmptyMap(ItemStack item) {
        return MapType.isEmptyMap(item.getItem());
    }


    public static MapDataHolder findMapFromItemStack(Level level, ItemStack item) {
        Integer mapId = getMapId(item);
        return mapId == null ? null : MapDataHolder.findFromId(level, mapId);
    }

    @Nullable
    public static Integer getMapId(ItemStack item) {
        MapId mapId = item.get(DataComponents.MAP_ID);
        return mapId == null ? null : mapId.id();
    }

    public static int findMapIntFromString(String id) {
        return Integer.parseInt(id.split("_")[1]);
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
        ItemStack atlasFromMainHand = player.getMainHandItem();
        if (atlasFromMainHand.is(MapAtlasesMod.MAP_ATLAS.get())) {
            return atlasFromMainHand;
        }
        // then offhand
        if (loc.hasOffhand()) {
            ItemStack atlasFromOffHand = player.getOffhandItem();
            if (atlasFromOffHand.is(MapAtlasesMod.MAP_ATLAS.get())) {
                return atlasFromOffHand;
            }
        }
        //then curios
        ItemStack atlasFromCurio = getAtlasFromCurioOrTrinket(player);
        if (!atlasFromCurio.isEmpty()) {
            return atlasFromCurio;
        }
        if (loc.scanAll()) {
            return getAtlasFromInventory(inventory, false);
        } else if (loc.hasHotbar()) {
            return getAtlasFromInventory(inventory, true);
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack getAtlasFromCurioOrTrinket(Player player) {
        if (MapAtlasesMod.CURIOS) {
            ItemStack itemStack = CuriosCompat.getAtlasInCurio(player);
            if (!itemStack.isEmpty()) return itemStack;
        }
        if (MapAtlasesMod.TRINKETS) {
            ItemStack itemStack = TrinketsCompat.getAtlasInTrinket(player);
            if (!itemStack.isEmpty()) return itemStack;
        }
        return ItemStack.EMPTY;
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

    public static void updateMapDataAndSync(
            MapDataHolder holder,
            ServerPlayer player,
            ItemStack atlas,
            TriState forceBeingCarried
    ) {
        MapAtlasesMod.setMapInInventoryHack(forceBeingCarried);
        //hack. just to be sure so contains will fail
        holder.data.tickCarriedBy(player, atlas, null);
        MapAtlasesAccessUtils.syncMapDataToClient(holder, player);
        MapAtlasesMod.setMapInInventoryHack(TriState.PASS);
    }


    // will fail if tickCarriedBy isnt sent
    private static void syncMapDataToClient(MapDataHolder holder, ServerPlayer player) {
        //ok so hear me out. we use this to send new map holder to the client when needed. thing is this packet isnt enough on its own
        // i need it for another mod so i'm using some code in moonlight which upgrades it to send center and dimension too (as well as custom colors)
        //TODO: maybe use isComplex  update packet and inventory tick
        Packet<?> p = holder.data.getUpdatePacket(new MapId(holder.id), player);
        if (p != null) {
            if (MapAtlasesMod.MOONLIGHT) {
                player.connection.send(p);
            } else if (p instanceof ClientboundMapItemDataPacket pp) {
                //send crappy wrapper if we dont.
                MapAtlasesNetworking.CHANNEL.sendToClientPlayer(player, new S2CMapPacketWrapper(holder.data, pp));
            }
        }
    }

}
