package pepjebs.mapatlases.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.CuriosCompat;
import pepjebs.mapatlases.integration.TrinketsCompat;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.map_collection.MapGridKey;

import java.util.Optional;

public class MapAtlasesAccessUtils {

    public static boolean canPlayerTeleport(Player player) {
        return MapAtlasesConfig.creativeTeleport.get() && player.isCreative();
    }

    //Helper function
    @Nullable
    public static MapItemSavedData getSavedDataAt(ItemStack atlas, Level level, int x, int z) {
        if (atlas.is(MapAtlasesMod.MAP_ATLAS.get())) {
            MapCollection maps = MapAtlasItem.getMaps(atlas, level);
            Slice slice = MapAtlasItem.getSelectedSlice(atlas, level.dimension());
            MapGridKey key = MapGridKey.at(maps.getScale(), slice, x, z);
            MapDataHolder select = maps.select(key);
            if (select != null) {
                return select.data;
            }
        }
        return null;
    }

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
        return MapDataHolder.find(id, type, level);
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

    public static boolean playerAtlasHasMap(Player player, MapId id, MapType type) {
        ItemStack atlas = getAtlasFromPlayerByConfig(player);
        return !atlas.isEmpty() && MapAtlasItem.getMaps(atlas, player.level()).hasMap(id, type);
    }

    @NotNull
    public static ItemStack getAtlasFromLecternOrPlayer(ServerPlayer player, Optional<BlockPos> lecternPos) {
        if (lecternPos.isEmpty()) return getAtlasFromPlayerByConfig(player);
        LecternBlockEntity lectern = getLecternInReach(player, lecternPos.get());
        if (lectern != null && lectern.getBook().is(MapAtlasesMod.MAP_ATLAS.get())) return lectern.getBook();
        return ItemStack.EMPTY;
    }

    @Nullable
    public static LecternBlockEntity getLecternInReach(ServerPlayer player, BlockPos pos) {
        if (!player.canInteractWithBlock(pos, 4)) return null;
        return player.level().getBlockEntity(pos) instanceof LecternBlockEntity lectern ? lectern : null;
    }

    public static void syncLecternAtlas(ServerPlayer player, Optional<BlockPos> lecternPos) {
        if (lecternPos.isEmpty()) return;
        LecternBlockEntity lectern = getLecternInReach(player, lecternPos.get());
        if (lectern == null) return;
        lectern.setChanged();
        BlockState state = lectern.getBlockState();
        player.level().sendBlockUpdated(lecternPos.get(), state, state, 3);
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


    @Nullable
    public static MapType getEmptyMapType(ItemStack stack) {
        if (stack.isEmpty() || !MapAtlasesConfig.enableEmptyMapEntryAndFill.get()){
            return null;
        }
        if (stack.is(Items.PAPER) && MapAtlasesConfig.acceptPaperForEmptyMaps.get()){
            return MapType.VANILLA;
        }
        return MapType.fromEmptyMap(stack.getItem());
    }

    public static void tickHoldingPlayerAndSync(
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
            player.connection.send(p);
        }
    }


}
