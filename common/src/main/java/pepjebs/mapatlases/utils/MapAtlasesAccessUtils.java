package pepjebs.mapatlases.utils;

import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.CuriosCompat;
import pepjebs.mapatlases.integration.TrinketsCompat;
import pepjebs.mapatlases.item.MapAtlasItem;

public class MapAtlasesAccessUtils {

    public static boolean isValidFilledMap(ItemStack item) {
        MapType mapType = MapType.fromFilledMap(item.getItem());
        return mapType != null && mapType.getMapId(item) != null;
    }

    @Nullable
    public static MapId findMapId(ItemStack itemstack) {
        MapType type = MapType.fromFilledMap(itemstack.getItem());
        if (type == null) return null;
        return type.getMapId(itemstack);
    }

    @Nullable
    public static MapDataHolder findMapFromItemStack(Level level, ItemStack itemStack) {
        MapType type = MapType.fromFilledMap(itemStack.getItem());
        if (type == null) return null;
        MapId id = type.getMapId(itemStack);
        if (id == null) return null;
        return MapDataHolder.get(id, type, level);
    }

    @NotNull
    private static ItemStack getAtlasFromInventory(Inventory inventory, boolean onlyHotbar) {
        int max = onlyHotbar ? 9 : inventory.getContainerSize();
        for (int i = 0; i < max; ++i) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.is(MapAtlasesMod.MAP_ATLAS.get())) {
                return itemStack;
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


    //must match the one below
    public static boolean isValidEmptyMapIngredient(ItemStack bottomItem) {
        int amountToAdd = bottomItem.getCount();
        MapType bottomType = MapType.fromEmptyMap(bottomItem.getItem());
        if (bottomItem.is(Items.PAPER) && MapAtlasesConfig.acceptPaperForEmptyMaps.get()) {
            bottomType = MapType.VANILLA;
        }
        return bottomType != null && amountToAdd > 0;
    }

    @Nullable
    public static Pair<MapType, Integer> getMapCountToAdd(ItemStack atlas, ItemStack bottomItem, Level level) {
        int amountToAdd = bottomItem.getCount();
        MapType bottomType = MapType.fromEmptyMap(bottomItem.getItem());
        if (bottomItem.is(Items.PAPER) && MapAtlasesConfig.acceptPaperForEmptyMaps.get()) {
            bottomType = MapType.VANILLA;
        }
        if (bottomType == null || amountToAdd == 0) return null;
        int existingMapCount = MapAtlasItem.getMaps(atlas, level).getCount() + MapAtlasItem.getEmptyMaps(atlas).getSize();
        if (MapAtlasItem.getMaxMapCount() != -1
                && existingMapCount + bottomItem.getCount() > MapAtlasItem.getMaxMapCount()) {
            amountToAdd = MapAtlasItem.getMaxMapCount() - existingMapCount;
        }
        return Pair.of(bottomType, amountToAdd);
    }

    public static void updateMapDataAndSync(
            MapDataHolder holder,
            ServerPlayer player,
            ItemStack atlas,
            TriState forceBeingCarried
    ) {
        MapAtlasesMod.setMapInInventoryHack(forceBeingCarried);
        //hack. just to be sure so contains will fail
        holder.data.tickCarriedBy(player, atlas);
        MapAtlasesAccessUtils.syncMapDataToClient(holder, player);
        MapAtlasesMod.setMapInInventoryHack(TriState.PASS);
    }


    // will fail if tickCarriedBy isnt sent
    private static void syncMapDataToClient(MapDataHolder holder, ServerPlayer player) {
        //ok so hear me out. we use this to send new map holder to the client when needed. thing is this packet isnt enough on its own
        // i need it for another mod so i'm using some code in moonlight which upgrades it to send center and dimension too (as well as custom colors)
        //TODO: maybe use isComplex  update packet and inventory tick
        Packet<?> p = holder.data.getUpdatePacket(holder.id, player);
        if (p != null) {
            if (MapAtlasesMod.MOONLIGHT) {
                player.connection.send(p);
            } else if (p instanceof ClientboundMapItemDataPacket pp) {
                //send crappy wrapper if we dont.
                // NetworkHelper.sendToClientPlayer(player, new S2CMapPacketWrapper(holder.data, pp));
            }
        }
    }


}
