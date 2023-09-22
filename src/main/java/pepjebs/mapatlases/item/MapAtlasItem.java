package pepjebs.mapatlases.item;

import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.capabilities.MapCollectionCap;
import pepjebs.mapatlases.capabilities.MapKey;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.utils.AtlasHolder;

import java.util.List;

public class MapAtlasItem extends Item {

    protected static final String EMPTY_MAPS_NBT = "empty";
    protected static final String LOCKED_NBT = "locked";
    protected static final String SLICE_NBT = "selected_slice";
    private static final String SHARE_TAG = "map_cap";

    public MapAtlasItem(Properties settings) {
        super(settings);
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new Provider(LazyOptional.of(MapCollectionCap::new));
    }

    // maybe not needed, could be in cap itself
    private record Provider(
            LazyOptional<MapCollectionCap> capInstance) implements ICapabilitySerializable<CompoundTag> {
        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return MapCollectionCap.ATLAS_CAP_TOKEN.orEmpty(cap, capInstance);
        }

        @Override
        public CompoundTag serializeNBT() {
            return capInstance.resolve().get().serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            capInstance.resolve().get().deserializeNBT(nbt);
        }
    }

    public static MapCollectionCap getMaps(ItemStack stack, Level level) {
        MapCollectionCap cap = stack.getCapability(MapCollectionCap.ATLAS_CAP_TOKEN, null).resolve().get();
        if (!cap.isInitialized()) cap.initialize(level);
        return cap;
    }

    public static int getMaxMapCount() {
        return MapAtlasesConfig.maxMapCount.get();
    }

    public static int getEmptyMaps(ItemStack atlas) {
        CompoundTag tag = atlas.getTag();
        return tag != null && tag.contains(EMPTY_MAPS_NBT) ? tag.getInt(EMPTY_MAPS_NBT) : 0;
    }

    public static void setEmptyMaps(ItemStack stack, int count) {
        stack.getOrCreateTag().putInt(EMPTY_MAPS_NBT, count);
    }

    public static void increaseEmptyMaps(ItemStack stack, int count) {
        setEmptyMaps(stack, getEmptyMaps(stack) + count);
    }

    public static boolean isLocked(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(LOCKED_NBT);
    }

    @Nullable
    public static Integer getSelectedSlice(ItemStack stack, ResourceKey<Level> dimension) {
        CompoundTag tag = stack.getTagElement(SLICE_NBT);
        if (tag != null) {
            String string = dimension.location().toString();
            if (tag.contains(string)) return tag.getInt(string);
        }
        return null;
    }

    public static void setSelectedSlice(ItemStack stack, @Nullable Integer slice, ResourceKey<Level> dimension) {
        if (slice != null) {
            CompoundTag tag = stack.getOrCreateTagElement(SLICE_NBT);
            tag.putInt(dimension.location().toString(), slice);
        } else {
            CompoundTag tag = stack.getTagElement(SLICE_NBT);
            if (tag != null) {
                tag.remove(dimension.location().toString());
            }
        }
    }


    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltip, isAdvanced);

        if (level != null && stack.hasTag()) {
            MapCollectionCap maps = getMaps(stack, level);
            int mapSize = maps.getCount();
            int empties = getEmptyMaps(stack);
            if (getMaxMapCount() != -1 && mapSize + empties >= getMaxMapCount()) {
                tooltip.add(Component.translatable("item.map_atlases.atlas.tooltip_full", "", null)
                        .withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY));
            }
            tooltip.add(Component.translatable("item.map_atlases.atlas.tooltip_maps", mapSize)
                    .withStyle(ChatFormatting.GRAY));
            if (MapAtlasesConfig.requireEmptyMapsToExpand.get() &&
                    MapAtlasesConfig.enableEmptyMapEntryAndFill.get()) {
                // If there are no maps & no empty maps, the atlas is "inactive", so display how many empty maps
                // they *would* receive if they activated the atlas
                if (mapSize + empties == 0) {
                    empties = MapAtlasesConfig.pityActivationMapCount.get();
                }
                tooltip.add(Component.translatable("item.map_atlases.atlas.tooltip_empty", empties).withStyle(ChatFormatting.GRAY));
            }

            Pair<String, MapItemSavedData> select = maps.select(MapAtlasesClient.getActiveMapKey());
            if (select == null) return;
            MapItemSavedData activeState = select.getSecond();
            tooltip.add(Component.translatable("filled_map.scale", 1 << activeState.scale).withStyle(ChatFormatting.GRAY));

            if (isLocked(stack)) {
                tooltip.add(Component.translatable("item.map_atlases.atlas.tooltip_locked").withStyle(ChatFormatting.GRAY));
            }
            Integer slice = getSelectedSlice(stack, level.dimension());
            if (slice != null) {
                tooltip.add(Component.translatable("item.map_atlases.atlas.tooltip_slice", slice).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.contains("maps")) {
            MapCollectionCap maps = getMaps(stack, level);
            for (var i : tag.getIntArray("maps")) {
                maps.add(i, level);
            }
            tag.remove("maps");
        }
        if (player.isSecondaryUseActive()) {
            boolean locked = !tag.getBoolean(LOCKED_NBT);
            tag.putBoolean(LOCKED_NBT, locked);
            if (player.level().isClientSide) {
                player.displayClientMessage(Component.translatable(locked ? "message.map_atlases.locked" : "message.map_atlases.unlocked"), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            openHandledAtlasScreen(serverPlayer);
        } else {
            MapAtlasesClient.openScreen(stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public void openHandledAtlasScreen(ServerPlayer player) {
        //TODO: sent packet
    }

    private void sendPlayerLecternAtlasData(ServerPlayer serverPlayer, ItemStack atlas) {
        // Send player all MapItemSavedDatas
        /*
        var states = MapAtlasesAccessUtilsOld.getAllMapInfoFromAtlas(serverPlayer.level(), atlas);
        for (var state : states.entrySet()) {
            state.getValue().getHoldingPlayer(serverPlayer);
            MapAtlasesServerEvents.relayMapItemSavedDataSyncToPlayerClient(state, serverPlayer);
        }*/
    }

    // I hate this
    @Nullable
    @Override
    public CompoundTag getShareTag(ItemStack stack) {
        CompoundTag baseTag = stack.getTag();
        var cap = stack.getCapability(MapCollectionCap.ATLAS_CAP_TOKEN, null).resolve().get();
        if (baseTag == null) baseTag = new CompoundTag();
        baseTag = baseTag.copy();
        baseTag.put(SHARE_TAG, cap.serializeNBT());
        return baseTag;
    }

    @Override
    public void readShareTag(ItemStack stack, @Nullable CompoundTag tag) {
        if (tag != null && tag.contains(SHARE_TAG)) {
            CompoundTag capTag = tag.getCompound(SHARE_TAG);
            tag.remove(SHARE_TAG);
            var cap = stack.getCapability(MapCollectionCap.ATLAS_CAP_TOKEN, null).resolve().get();
            cap.deserializeNBT(capTag);
        }
        stack.setTag(tag);
    }

    // convert lectern
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

                MapCollectionCap maps = getMaps(stack, level);
                var mapState = maps.select(MapKey.at(maps.getScale(), player, getSelectedSlice(stack, level.dimension())));
                if (mapState == null) return InteractionResult.FAIL;
                boolean didAdd = mapState.getSecond().toggleBanner(level, blockPos);
                if (!didAdd)
                    return InteractionResult.FAIL;
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return super.useOn(context);
        }
    }
}
