package pebjebs.mapatlases.item;

import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import pebjebs.mapatlases.MapAtlasesMod;
import pebjebs.mapatlases.client.MapAtlasesClient;
import pebjebs.mapatlases.config.MapAtlasesConfig;
import pebjebs.mapatlases.lifecycle.MapAtlasesServerEvents;
import pebjebs.mapatlases.screen.MapAtlasesAtlasOverviewContainerMenu;
import pebjebs.mapatlases.utils.AtlasHolder;
import pebjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapAtlasItem extends Item implements MenuProvider {

    public static final String EMPTY_MAP_NBT = "empty";
    public static final String MAP_LIST_NBT = "maps";

    public MapAtlasItem(Properties settings) {
        super(settings);
    }

    public static int getMaxMapCount() {
        return MapAtlasesConfig.maxMapCount.get();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltip, isAdvanced);

        if (level != null) {
            int mapSize = MapAtlasesAccessUtils.getMapCountFromItemStack(stack);
            int empties = MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(stack);
            if (getMaxMapCount() != -1 && mapSize + empties >= getMaxMapCount()) {
                tooltip.add(Component.translatable("item.map_atlases.atlas.tooltip_full", "", null)
                        .withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY));
            }
            tooltip.add(Component.translatable("item.map_atlases.atlas.tooltip_1", mapSize)
                    .withStyle(ChatFormatting.GRAY));
            if (MapAtlasesConfig.requireEmptyMapsToExpand.get() &&
                    MapAtlasesConfig.enableEmptyMapEntryAndFill.get()) {
                // If there's no maps & no empty maps, the atlas is "inactive", so display how many empty maps
                // they *would* receive if they activated the atlas
                if (mapSize + empties == 0) {
                    empties = MapAtlasesConfig.pityActivationMapCount.get();
                }
                tooltip.add(Component.translatable("item.map_atlases.atlas.tooltip_2", empties)
                        .withStyle(ChatFormatting.GRAY));
            }
            MapItemSavedData mapState = level.getMapData(MapAtlasesClient.getActiveMap());
            if (mapState == null) return;
            tooltip.add(Component.translatable("item.map_atlases.atlas.tooltip_3", 1 << mapState.scale)
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            openHandledAtlasScreen(serverPlayer);
        }
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    public void openHandledAtlasScreen(ServerPlayer player) {
        NetworkHooks.openScreen(player, this, b -> this.writeScreenOpeningData(player, b));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        ItemStack atlas = getAtlasFromLookingLectern(player);
        if (atlas.isEmpty()) {
            atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
        }
        Map<Integer, Pair<String, List<Integer>>> idsToCenters = new HashMap<>();
        Map<String, MapItemSavedData> mapInfos = MapAtlasesAccessUtils.getAllMapInfoFromAtlas(player.level(), atlas);
        for (Map.Entry<String, MapItemSavedData> state : mapInfos.entrySet()) {
            var id = MapAtlasesAccessUtils.getMapIntFromString(state.getKey());
            var centers = Arrays.asList(state.getValue().centerX, state.getValue().centerZ);
            var dimStr = MapAtlasesAccessUtils.getMapItemSavedDataDimKey(state.getValue());
            idsToCenters.put(id, new Pair<>(dimStr, centers));
        }
        var currentIds = MapAtlasesAccessUtils.getCurrentDimMapInfoFromAtlas(player.level(), atlas);
        // TODO: Sometimes throws bc of null when opening Lectern
        String centerMap = MapAtlasesAccessUtils
                .getActiveAtlasMapItemSavedDataServer(currentIds, (ServerPlayer) player).getKey();
        int atlasScale = MapAtlasesAccessUtils.getAtlasBlockScale(player.level(), atlas);
        return new MapAtlasesAtlasOverviewContainerMenu(syncId, inv, idsToCenters, atlas, centerMap, atlasScale);
    }

    public ItemStack getAtlasFromLookingLectern(Player player) {
        HitResult h = player.pick(10, 1, false);
        if (h.getType() == HitResult.Type.BLOCK) {
            BlockEntity e = player.level().getBlockEntity(BlockPos.containing(h.getLocation()));
            if (e instanceof LecternBlockEntity be) {
                ItemStack book = be.getBook();
                if (book.getItem() == MapAtlasesMod.MAP_ATLAS) {
                    return book;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private void sendPlayerLecternAtlasData(ServerPlayer serverPlayer, ItemStack atlas) {
        // Send player all MapItemSavedDatas
        var states = MapAtlasesAccessUtils.getAllMapInfoFromAtlas(serverPlayer.level(), atlas);
        for (var state : states.entrySet()) {
            state.getValue().getHoldingPlayer(serverPlayer);
            MapAtlasesServerEvents.relayMapItemSavedDataSyncToPlayerClient(state, serverPlayer);
        }
    }

    //TODO:port
    //@Override
    public void writeScreenOpeningData(ServerPlayer serverPlayer, FriendlyByteBuf packetByteBuf) {
        ItemStack atlas = getAtlasFromLookingLectern(serverPlayer);
        if (atlas.isEmpty()) {
            atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(serverPlayer);
        } else {
            sendPlayerLecternAtlasData(serverPlayer, atlas);
        }
        if (atlas.isEmpty()) return;
        var mapInfos = MapAtlasesAccessUtils
                .getAllMapInfoFromAtlas(serverPlayer.level(), atlas);
        var currentInfos = MapAtlasesAccessUtils
                .getCurrentDimMapInfoFromAtlas(serverPlayer.level(), atlas);
        String centerMap = MapAtlasesAccessUtils
                .getActiveAtlasMapItemSavedDataServer(currentInfos, serverPlayer).getKey();
        int atlasScale = MapAtlasesAccessUtils.getAtlasBlockScale(serverPlayer.level(), atlas);
        packetByteBuf.writeItem(atlas);
        packetByteBuf.writeUtf(centerMap);
        packetByteBuf.writeInt(atlasScale);
        packetByteBuf.writeInt(mapInfos.size());
        for (Map.Entry<String, MapItemSavedData> state : mapInfos.entrySet()) {
            packetByteBuf.writeInt(MapAtlasesAccessUtils.getMapIntFromString(state.getKey()));
            packetByteBuf.writeUtf(MapAtlasesAccessUtils.getMapItemSavedDataDimKey(state.getValue()));
            packetByteBuf.writeInt(state.getValue().centerX);
            packetByteBuf.writeInt(state.getValue().centerZ);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos blockPos = context.getClickedPos();
        if (context.getPlayer() == null) {
            return super.useOn(context);
        } else {
        }
        Level level = context.getLevel();
        BlockState blockState = level.getBlockState(blockPos);
        ItemStack stack = context.getItemInHand();
        if (blockState.is(Blocks.LECTERN)) {
            boolean didPut = LecternBlock.tryPlaceBook(
                    context.getPlayer(),
                    level,
                    blockPos,
                    blockState,
                    stack
            );
            if (!didPut) {
                return InteractionResult.PASS;
            }
            blockState = level.getBlockState(blockPos);
            LecternBlock.resetBookState(
                    context.getPlayer(), level, blockPos, blockState, true);
            if (level.getBlockEntity(blockPos) instanceof AtlasHolder ah) {
                ah.mapatlases$setAtlas(true);
                //level.sendBlockUpdated(blockPos, blockState, blockState, 3);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (blockState.is(BlockTags.BANNERS)) {
            if (!level.isClientSide) {
                Map<String, MapItemSavedData> currentDimMapInfos = MapAtlasesAccessUtils.getCurrentDimMapInfoFromAtlas(
                        level, stack);
                MapItemSavedData mapState = MapAtlasesAccessUtils.getActiveAtlasMapItemSavedDataServer(
                        currentDimMapInfos, (ServerPlayer) context.getPlayer()).getValue();
                if (mapState == null)
                    return InteractionResult.FAIL;
                boolean didAdd = mapState.toggleBanner(level, blockPos);
                if (!didAdd)
                    return InteractionResult.FAIL;
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return super.useOn(context);
        }
    }
}
