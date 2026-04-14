package pepjebs.mapatlases.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.SupplementariesCompat;
import pepjebs.mapatlases.map_collection.IMapCollection;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.map_collection.MapKey;
import pepjebs.mapatlases.networking.C2S2COpenAtlasScreenPacket;
import pepjebs.mapatlases.networking.MapAtlasesNetworking;
import pepjebs.mapatlases.utils.*;

import java.util.List;
import java.util.function.Consumer;

public class MapAtlasItem extends Item {

    protected static final String EMPTY_MAPS_NBT = "empty";
    protected static final String LOCKED_NBT = "locked";
    protected static final String SELECTED_NBT = "selected";
    public static final String HEIGHT_NBT = "height";
    public static final String TYPE_NBT = "type";

    public MapAtlasItem(Properties settings) {
        super(settings);
    }

    public static void removeMap(ItemStack atlas, int mapId, ServerPlayer player) {
        //TODO: remove map
        var data = IMapCollection.get(atlas, player.level());
        MapDataHolder holder = MapDataHolder.findFromId(player.level(), mapId);
        boolean removed = data.remove(holder);
        if (removed) {
            ItemStack item = holder.createExistingMapItem();
            if (!player.getInventory().add(item)) {
                player.drop(item, false);
            }
        }
    }


    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay,
                                Consumer<Component> tooltip, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, isAdvanced);
        int[] mapIds = getStoredMapIds(stack, null);
        int mapSize = mapIds.length;
        int empties = getEmptyMaps(stack);
        if (getMaxMapCount() != -1 && mapSize + empties >= getMaxMapCount()) {
            tooltip.accept(Component.translatable("item.map_atlases.atlas.tooltip_full")
                    .withStyle(ChatFormatting.ITALIC)
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.accept(Component.translatable("item.map_atlases.atlas.tooltip_maps", mapSize)
                .withStyle(ChatFormatting.GRAY));
        if (MapAtlasesConfig.requireEmptyMapsToExpand.get() &&
                MapAtlasesConfig.enableEmptyMapEntryAndFill.get()) {
            if (mapSize + empties == 0) {
                empties = MapAtlasesConfig.pityActivationMapCount.get();
            }
            tooltip.accept(Component.translatable("item.map_atlases.atlas.tooltip_empty", empties)
                    .withStyle(ChatFormatting.GRAY));
        }

        MapItemSavedData firstMap = mapIds.length > 0 ? context.mapData(new MapId(mapIds[0])) : null;
        int scale = firstMap != null ? 1 << firstMap.scale : 1;
        tooltip.accept(Component.translatable("item.map_atlases.atlas.tooltip_scale", scale)
                .withStyle(ChatFormatting.GRAY));

        if (isLocked(stack)) {
            tooltip.accept(Component.translatable("item.map_atlases.atlas.tooltip_locked")
                    .withStyle(ChatFormatting.GRAY));
        }
        if (MapAtlasesMod.SUPPLEMENTARIES && SupplementariesCompat.hasAntiqueInk(stack)) {
            tooltip.accept(Component.translatable("item.map_atlases.atlas.supplementaries_antique")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag tag = ItemStackData.getOrEmpty(stack);
        convertOldAtlas(level, stack);
        if (player.isSecondaryUseActive()) {
            boolean wasLocked = stack.has(MapAtlasLockIcon.LOCKED);
            if (wasLocked) {
                stack.remove(MapAtlasLockIcon.LOCKED);
                player.level().playSound(null, player, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.7F, 2f);
            } else {
                stack.set(MapAtlasLockIcon.LOCKED, true);
                player.level().playSound(null, player, SoundEvents.BOOK_PUT, SoundSource.PLAYERS, 1.7F, 2f);
            }
            ItemStackData.update(stack, t -> t.remove(LOCKED_NBT));
            if (player.level().isClientSide()) {
                // player.sendSystemMessage(Component.translatable(!wasLocked ? "message.map_atlases.locked" : "message.map_atlases.unlocked"));
            }
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        }
        if (level.isClientSide()) {
            MapAtlasesNetworking.CHANNEL.sendToServer(C2S2COpenAtlasScreenPacket.forHand(hand));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void convertOldAtlas(Level level, ItemStack stack) {
        CompoundTag tag = ItemStackData.getTag(stack);
        //convert old atlas
        if (tag != null && tag.contains("maps")) {
            IMapCollection maps = getMaps(stack, level);
            for (var i : tag.getIntArray("maps").orElseGet(() -> new int[0])) {
                maps.add(i, level);
            }
            ItemStackData.update(stack, t -> t.remove("maps"));
        }
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
            if (level.getBlockEntity(blockPos) instanceof AtlasLectern ah && ah.mapatlases$setAtlas(player, stack)) {
                return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
            }
            return super.useOn(context);
        }
        if (blockState.is(BlockTags.BANNERS)) {
            if (!level.isClientSide()) {

                IMapCollection maps = getMaps(stack, level);
                MapDataHolder mapState = maps.select(MapKey.at(maps.getScale(), player, getSelectedSlice(stack, level.dimension())));
                if (mapState == null) return InteractionResult.FAIL;
                boolean didAdd = mapState.data.toggleBanner(level, blockPos);
                if (!didAdd)
                    return InteractionResult.FAIL;
            }
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        } else {
            return this.use(level, player, context.getHand());
        }
    }


    // Utilities functions


    public static void syncAndOpenGui(
            ServerPlayer player,
            ItemStack atlas,
            C2S2COpenAtlasScreenPacket.OpenSource source,
            @Nullable InteractionHand hand,
            @Nullable BlockPos lecternPos,
            boolean pinOnly
    ) {
        if (atlas.isEmpty()) {
            return;
        }
        //we need to send all data for all dimensions as they are not sent automatically
        IMapCollection maps = MapAtlasItem.getMaps(atlas, player.level());
        for (var info : maps.getAll()) {
            // Force atlas pages to be treated as carried during initial GUI sync so old pages
            // are resent even if vanilla no longer recognizes atlas stacks as map carriers.
            MapAtlasesAccessUtils.updateMapDataAndSync(info, player, atlas, TriState.SET_TRUE);
        }
        // Fabric 26.1 is not reliably syncing the atlas custom data to the client before GUI open.
        // Force a full inventory/menu refresh so the client atlas stack has the current map id list.
        player.inventoryMenu.broadcastFullState();
        if (player.containerMenu != player.inventoryMenu) {
            player.containerMenu.broadcastFullState();
        }
        MapAtlasesNetworking.CHANNEL.sendToClientPlayer(player, new C2S2COpenAtlasScreenPacket(source, hand, lecternPos, pinOnly));
    }

    public static void setSelectedSlice(ItemStack stack, Slice slice) {
        MapType t = slice.type();
        Integer h = slice.height();
        var dimension = slice.dimension();
        String dimensionId = dimension.identifier().toString();
        if (h == null && t == MapType.VANILLA) {
            CompoundTag tag = ItemStackData.getTag(stack);
            if (tag != null) {
                CompoundTag selected = tag.getCompound(SELECTED_NBT).orElse(null);
                if (selected != null) {
                    ItemStackData.update(stack, fullTag -> fullTag.getCompound(SELECTED_NBT).ifPresent(s -> s.remove(dimensionId)));
                }
            }

        } else {
            ItemStackData.update(stack, fullTag -> {
                CompoundTag selected = fullTag.getCompound(SELECTED_NBT).orElseGet(CompoundTag::new);
                selected.put(dimensionId, slice.save());
                fullTag.put(SELECTED_NBT, selected);
            });
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
    public static IMapCollection getMaps(ItemStack stack, Level level) {
       return IMapCollection.get(stack, level);
    }

    public static int[] getStoredMapIds(ItemStack stack, @Nullable Level level) {
        int[] componentIds = stack.get(MapAtlasesMod.ATLAS_MAP_IDS.get());
        CompoundTag tag = ItemStackData.getTag(stack);
        int[] legacyIds = tag != null ? tag.getIntArray(MapCollection.MAP_LIST_NBT).orElseGet(() -> new int[0]) : new int[0];
        if (componentIds == null || componentIds.length == 0) {
            return level != null && legacyIds.length == 0 ? getMaps(stack, level).getAllIds() : legacyIds;
        }
        if (legacyIds.length == 0) {
            return componentIds;
        }
        java.util.TreeSet<Integer> merged = new java.util.TreeSet<>();
        for (int id : componentIds) {
            merged.add(id);
        }
        for (int id : legacyIds) {
            merged.add(id);
        }
        return merged.stream().mapToInt(Integer::intValue).toArray();
    }

    public static int getMaxMapCount() {
        return MapAtlasesConfig.maxMapCount.get();
    }

    public static int getEmptyMaps(ItemStack atlas) {
        CompoundTag tag = ItemStackData.getTag(atlas);
        return tag != null && tag.contains(EMPTY_MAPS_NBT) ? tag.getInt(EMPTY_MAPS_NBT).orElse(0) : 0;
    }

    public static void setEmptyMaps(ItemStack stack, int count) {
        ItemStackData.update(stack, tag -> tag.putInt(EMPTY_MAPS_NBT, count));
    }

    public static void increaseEmptyMaps(ItemStack stack, int count) {
        setEmptyMaps(stack, getEmptyMaps(stack) + count);
    }

    public static boolean isLocked(ItemStack stack) {
        return stack.has(MapAtlasLockIcon.LOCKED);
    }

    @NotNull
    public static Slice getSelectedSlice(ItemStack stack, ResourceKey<Level> dimension) {
        CompoundTag fullTag = ItemStackData.getTag(stack);
        CompoundTag tag = fullTag == null ? null : fullTag.getCompound(SELECTED_NBT).orElse(null);
        if (tag != null) {
            String string = dimension.identifier().toString();
            if (tag.contains(string)) {
                var t = tag.getCompound(string).orElse(null);
                if (t != null) {
                    return Slice.parse(t, dimension);
                }
            }
        }
        return Slice.of(MapType.VANILLA, null, dimension);
    }

    @Override
    public void onCraftedBy(ItemStack stack, Player pPlayer) {
        super.onCraftedBy(stack, pPlayer);

        Level level = pPlayer.level();
        convertOldAtlas(level, stack);
        validateSelectedSlices(stack, level);
    }

    private static void validateSelectedSlices(ItemStack pStack, Level level) {
        // Populate default slices
        var maps = getMaps(pStack, level);
        var dim = maps.getAvailableDimensions();
        for (var d : dim) {
            for (var k : maps.getAvailableTypes(d)) {
                var av = maps.getHeightTree(d, k);
                if (!av.contains(getSelectedSlice(pStack, d).heightOrTop())) {
                    setSelectedSlice(pStack, Slice.of(k, av.first(), d));
                }
            }
        }
    }

}
