package pepjebs.mapatlases.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
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
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.client.screen.MapAtlasesAtlasOverviewScreen;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.lifecycle.MapAtlasesServerEvents;
import pepjebs.mapatlases.utils.AtlasHolder;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtilsOld;

import java.util.List;
import java.util.Map;

public class MapAtlasItem extends Item {

    public static final String EMPTY_MAP_NBT = "empty";
    public static final String MAP_LIST_NBT = "maps";
    public static final String LOCKED_NBT = "locked";
    public static final String SLICE_NBT = "selected_slice";

    public MapAtlasItem(Properties settings) {
        super(settings);
    }

    public static int getMaxMapCount() {
        return MapAtlasesConfig.maxMapCount.get();
    }

    public static boolean isLocked(ItemStack stack) {
        var tag = stack.getTag();
        return tag != null && tag.getBoolean(LOCKED_NBT);
    }

    @Nullable
    public static Integer getSelectedSlice(ItemStack stack){
        var tag = stack.getTag();
        if(tag != null && tag.contains(SLICE_NBT)){
            return tag.getInt(SLICE_NBT);
        }return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltip, isAdvanced);

        if (level != null) {
            int mapSize = MapAtlasesAccessUtilsOld.getMapCountFromItemStack(stack);
            int empties = MapAtlasesAccessUtilsOld.getEmptyMapCountFromItemStack(stack);
            if (getMaxMapCount() != -1 && mapSize + empties >= getMaxMapCount()) {
                tooltip.add(Component.translatable("item.map_atlases.atlas.tooltip_full", "", null)
                        .withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY));
            }
            tooltip.add(Component.translatable("item.map_atlases.atlas.tooltip_maps", mapSize)
                    .withStyle(ChatFormatting.GRAY));
            if (MapAtlasesConfig.requireEmptyMapsToExpand.get() &&
                    MapAtlasesConfig.enableEmptyMapEntryAndFill.get()) {
                // If there's no maps & no empty maps, the atlas is "inactive", so display how many empty maps
                // they *would* receive if they activated the atlas
                if (mapSize + empties == 0) {
                    empties = MapAtlasesConfig.pityActivationMapCount.get();
                }
                tooltip.add(Component.translatable("item.map_atlases.atlas.tooltip_empty", empties)
                        .withStyle(ChatFormatting.GRAY));
            }
            MapItemSavedData mapState = level.getMapData(MapAtlasesClient.getActiveMap());
            if (mapState == null) return;
            tooltip.add(Component.translatable("item.map_atlases.atlas.tooltip_scale", 1 << mapState.scale)
                    .withStyle(ChatFormatting.GRAY));

            if(isLocked(stack)){
                tooltip.add(Component.translatable("item.map_atlases.atlas.tooltip_locked")
                        .withStyle(ChatFormatting.GRAY));
            }
            Integer slice = getSelectedSlice(stack);
            if(slice != null){
                tooltip.add(Component.translatable("item.map_atlases.atlas.tooltip_slice", slice)
                        .withStyle(ChatFormatting.GRAY));
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            ItemStack stack = player.getItemInHand(hand);
            CompoundTag tag = stack.getOrCreateTag();
            boolean locked = !tag.getBoolean(LOCKED_NBT);
            tag.putBoolean(LOCKED_NBT, locked);
            if(player.level().isClientSide){
                player.displayClientMessage(Component.translatable( locked ? "message.map_atlases.locked" : "message.map_atlases.unlocked"),true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            openHandledAtlasScreen(serverPlayer);
        } else {
            openScreen(player);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    public void openHandledAtlasScreen(ServerPlayer player) {
        //TODO: sent packet
    }

    public void openScreen(Player player) {
        ItemStack atlas = getAtlasFromLookingLectern(player);
        if (atlas.isEmpty()) {
            atlas = MapAtlasesAccessUtilsOld.getAtlasFromPlayerByConfig(player);
        }
        Minecraft.getInstance().setScreen(new MapAtlasesAtlasOverviewScreen(Component.translatable(getDescriptionId()), atlas));
    }

    public ItemStack getAtlasFromLookingLectern(Player player) {
        HitResult h = player.pick(10, 1, false);
        if (h.getType() == HitResult.Type.BLOCK) {
            BlockEntity e = player.level().getBlockEntity(BlockPos.containing(h.getLocation()));
            if (e instanceof LecternBlockEntity be) {
                ItemStack book = be.getBook();
                if (book.is(MapAtlasesMod.MAP_ATLAS.get())) {
                    return book;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private void sendPlayerLecternAtlasData(ServerPlayer serverPlayer, ItemStack atlas) {
        // Send player all MapItemSavedDatas
        var states = MapAtlasesAccessUtilsOld.getAllMapInfoFromAtlas(serverPlayer.level(), atlas);
        for (var state : states.entrySet()) {
            state.getValue().getHoldingPlayer(serverPlayer);
            MapAtlasesServerEvents.relayMapItemSavedDataSyncToPlayerClient(state, serverPlayer);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return super.useOn(context);
        }
        BlockPos blockPos = context.getClickedPos();

        Level level = context.getLevel();
        BlockState blockState = level.getBlockState(blockPos);
        ItemStack stack = context.getItemInHand();
        if (blockState.is(Blocks.LECTERN)) {
            boolean didPut = LecternBlock.tryPlaceBook(
                    player,
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
                    player, level, blockPos, blockState, true);
            if (level.getBlockEntity(blockPos) instanceof AtlasHolder ah) {
                ah.mapatlases$setAtlas(true);
                //level.sendBlockUpdated(blockPos, blockState, blockState, 3);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (blockState.is(BlockTags.BANNERS)) {
            if (!level.isClientSide) {
                Map<String, MapItemSavedData> currentDimMapInfos = MapAtlasesAccessUtilsOld.getCurrentDimMapInfoFromAtlas(
                        level, stack);
                MapItemSavedData mapState = MapAtlasesAccessUtilsOld.getActiveAtlasMapStateServer(
                        currentDimMapInfos, (ServerPlayer) player).getValue();
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
