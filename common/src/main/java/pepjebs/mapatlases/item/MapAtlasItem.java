package pepjebs.mapatlases.item;

import com.google.common.base.Preconditions;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Unit;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.jetbrains.annotations.NotNull;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.SupplementariesCompat;
import pepjebs.mapatlases.map_collection.EmptyMaps;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.map_collection.MapSearchKey;
import pepjebs.mapatlases.map_collection.SelectedSlice;
import pepjebs.mapatlases.networking.C2S2COpenAtlasScreenPacket;
import pepjebs.mapatlases.utils.*;

import java.util.List;
import java.util.Optional;

public class MapAtlasItem extends Item {

    public static final String TYPE_NBT = "type";

    public MapAtlasItem(Properties settings) {
        super(settings);
    }

    public static void removeAndDropMap(MapId id, MapType type, ItemStack atlas, ServerPlayer player) {
        MapCollection data = getMaps(atlas, player.level());
        MapDataHolder holder = MapDataHolder.get(id, type, player.level());
        if (holder != null && data != data.removeAndAssigns(atlas, player.level(), id, type)) {
            ItemStack item = holder.createExistingMapItem();
            if (!player.getInventory().add(item)) {
                player.drop(item, false);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        if (PlatHelper.getPhysicalSide().isServer()) return;

        Level level = MapAtlasesClient.getLevel();
        MapCollection maps = getMaps(stack, level);
        int mapSize = maps.getCount();

        tooltipComponents.add(Component.translatable("item.map_atlases.atlas.tooltip_maps", mapSize).withStyle(ChatFormatting.GRAY));

        EmptyMaps emptyMaps = stack.getOrDefault(MapAtlasesMod.EMPTY_MAPS.get(), EmptyMaps.EMPTY);
        tooltipComponents.addAll(emptyMaps.getTooltips(mapSize));

        tooltipComponents.add(Component.translatable("filled_map.scale", 1 << maps.getScale()).withStyle(ChatFormatting.GRAY));

        if (isLocked(stack)) {
            tooltipComponents.add(Component.translatable("item.map_atlases.atlas.tooltip_locked").withStyle(ChatFormatting.GRAY));
        }
        Slice selected = getSelectedSlice(stack, level.dimension());
        var height = selected.height();
        height.ifPresent(integer ->
                tooltipComponents.add(Component.translatable("item.map_atlases.atlas.tooltip_slice", integer).withStyle(ChatFormatting.GRAY)));
        var type = selected.type();
        if (type != MapType.VANILLA) {
            tooltipComponents.add(Component.translatable("item.map_atlases.atlas.tooltip_type", type.getName()).withStyle(ChatFormatting.GRAY));
        }
        if (MapAtlasesMod.SUPPLEMENTARIES && SupplementariesCompat.hasAntiqueInk(stack)) {
            tooltipComponents.add(Component.translatable("item.map_atlases.atlas.supplementaries_antique").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            boolean locked = !stack.has(MapAtlasesMod.LOCKED.get());
            if (locked) {
                stack.remove(MapAtlasesMod.LOCKED.get());
            } else {
                stack.set(MapAtlasesMod.LOCKED.get(), Unit.INSTANCE);
            }
            if (player.level().isClientSide) {
                player.displayClientMessage(Component.translatable(locked ? "message.map_atlases.locked" : "message.map_atlases.unlocked"), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (player instanceof ServerPlayer sp) {
            syncAndOpenGui(sp, stack, Optional.empty(), false);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
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
            if (level.getBlockEntity(blockPos) instanceof AtlasLectern ah) {
                ah.mapatlases$setAtlas(player, stack);
                //height.sendBlockUpdated(blockPos, blockState, blockState, 3);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (blockState.is(BlockTags.BANNERS)) {
            if (!level.isClientSide) {

                MapCollection maps = getMaps(stack, level);
                MapDataHolder mapState = maps.select(MapSearchKey.at(maps.getScale(), player, getSelectedSlice(stack, level.dimension())));
                if (mapState == null) return InteractionResult.FAIL;
                boolean didAdd = mapState.data.toggleBanner(level, blockPos);
                if (!didAdd)
                    return InteractionResult.FAIL;
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            //others deco

            return super.useOn(context);
        }
    }


    // Utilities functions


    public static void syncAndOpenGui(ServerPlayer player, ItemStack atlas, @NotNull Optional<BlockPos> lecternPos, boolean pinOnly) {
        if (atlas.isEmpty()) return;
        //we need to send all data for all dimensions as they are not sent automatically
        MapCollection maps = MapAtlasItem.getMaps(atlas, player.level());
        for (var info : maps.getAllFound()) {
            // update all maps and sends them to player, if needed
            MapAtlasesAccessUtils.updateMapDataAndSync(info, player, atlas, TriState.PASS);
        }
        NetworkHelper.sendToClientPlayer(player, new C2S2COpenAtlasScreenPacket(lecternPos, pinOnly));
    }

    public static void setSelectedSlice(ItemStack stack, Slice slice, Level level) {
        MapType t = slice.type();
        var h = slice.height();
        var dimension = slice.dimension();
        if (h.isEmpty() && t == MapType.VANILLA) {
            SelectedSlice selectedSlice = stack.get(MapAtlasesMod.SELECTED_SLICE.get());
            if (selectedSlice != null) {
                selectedSlice.removeAndAssigns(stack, dimension);
            }

        } else {
            //validate:
            MapCollection maps = getMaps(stack, level);
            if (!maps.getHeightTree(dimension, t).contains(slice.heightOrTop())) {
                return;
            }
            SelectedSlice selectedSlice = stack.getOrDefault(MapAtlasesMod.SELECTED_SLICE.get(), SelectedSlice.EMPTY);
            selectedSlice.addAndAssigns(stack, dimension, slice);
        }
    }

    //TODO:
/*
    public static boolean decreaseSlice(ItemStack atlas, Level level) {
        IMapCollection maps = MapAtlasItem.getMaps(atlas, level);
        int current = selectedSlice.heightOrTop();
        MapType type = selectedSlice.type();
        ResourceKey<Level> dim = selectedSlice.dimension();
        Integer newHeight = maps.getHeightTree(dim, type).floor(current - 1);
        return updateSlice(Slice.of(type, newHeight, dim));
    }

    //TODO: make static
    public static boolean increaseSlice(ItemStack atlas, Level level) {
        IMapCollection maps = MapAtlasItem.getMaps(atlas, level);
        int current = selectedSlice.heightOrTop();
        MapType type = selectedSlice.type();
        ResourceKey<Level> dim = selectedSlice.dimension();
        Integer newHeight = maps.getHeightTree(dim, type).ceiling(current + 1);
        return updateSlice(Slice.of(type, newHeight, dim));
    }*/
    public static MapCollection getMaps(ItemStack stack, Level level) {
        //gets and assure initialized
        var comp = stack.get(MapAtlasesMod.MAP_COLLECTION.get());
        Preconditions.checkNotNull(comp, "Map collection component was null");
        if (!comp.isInitialized()) {
            comp.initialize(level);
        }
        return comp;
    }

    public static int getMaxMapCount() {
        return MapAtlasesConfig.maxMapCount.get();
    }

    public static EmptyMaps getEmptyMaps(ItemStack atlas) {
        return atlas.getOrDefault(MapAtlasesMod.EMPTY_MAPS.get(), EmptyMaps.EMPTY);
    }

    public static boolean isLocked(ItemStack stack) {
        return stack.has(MapAtlasesMod.LOCKED.get());
    }

    @NotNull
    public static Slice getSelectedSlice(ItemStack stack, ResourceKey<Level> dimension) {
        SelectedSlice selectedSlice = stack.get(MapAtlasesMod.SELECTED_SLICE.get());
        if (selectedSlice != null) {
            Slice slice = selectedSlice.get(dimension);
            if (slice != null) return slice;
        }
        return Slice.of(MapType.VANILLA, null, dimension);
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player pPlayer) {
        super.onCraftedBy(stack, level, pPlayer);

        validateSelectedSlices(stack, level);
    }

    private static void validateSelectedSlices(ItemStack pStack, Level level) {
        // Populate default slices
        MapCollection maps = getMaps(pStack, level);
        var dim = maps.getAvailableDimensions();
        for (var d : dim) {
            for (var k : maps.getAvailableTypes(d)) {
                var av = maps.getHeightTree(d, k);
                if (!av.contains(getSelectedSlice(pStack, d).heightOrTop())) {
                    setSelectedSlice(pStack, Slice.of(k, av.first(), d), level);
                }
            }
        }
    }

}
