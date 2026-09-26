package pepjebs.mapatlases.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.CuriosCompat;
import pepjebs.mapatlases.integration.TrinketsCompat;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.MapCollection;

import java.util.Optional;

public class AtlasLookup {

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
}
