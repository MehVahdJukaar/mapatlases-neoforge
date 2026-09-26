package pepjebs.mapatlases.item;

import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
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
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.SupplementariesCompat;
import pepjebs.mapatlases.map_collection.EmptyMaps;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.map_collection.MapGridKey;
import pepjebs.mapatlases.map_collection.MapIds;
import pepjebs.mapatlases.map_collection.SelectedSlices;
import pepjebs.mapatlases.networking.C2S2COpenAtlasScreenPacket;
import pepjebs.mapatlases.utils.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MapAtlasItem extends Item {

    public MapAtlasItem(Properties settings) {
        super(settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        if (PlatHelper.getPhysicalSide().isServer()) return;

        Level level = MapAtlasesClient.getLevel();
        MapCollection maps = getMaps(stack, level);
        int mapSize = maps.getCount();

        tooltipComponents.add(Component.translatable("item.map_atlases.atlas.tooltip_maps", mapSize).withStyle(ChatFormatting.GRAY));

        addEmptyMapsTooltip(getEmptyMaps(stack), mapSize, tooltipComponents);

        tooltipComponents.add(Component.translatable("filled_map.scale", 1 << maps.getScale()).withStyle(ChatFormatting.GRAY));

        if (isLocked(stack)) {
            tooltipComponents.add(Component.translatable("item.map_atlases.atlas.tooltip_locked").withStyle(ChatFormatting.GRAY));
        }
        Slice selected = getSelectedSlice(stack, level.dimension());
        selected.height().ifPresent(h ->
                tooltipComponents.add(Component.translatable("item.map_atlases.atlas.tooltip_slice", h).withStyle(ChatFormatting.GRAY)));
        if (selected.type() != MapType.VANILLA) {
            tooltipComponents.add(Component.translatable("item.map_atlases.atlas.tooltip_type", selected.type().getName()).withStyle(ChatFormatting.GRAY));
        }
        if (MapAtlasesMod.SUPPLEMENTARIES && SupplementariesCompat.hasAntiqueInk(stack)) {
            tooltipComponents.add(Component.translatable("item.map_atlases.atlas.supplementaries_antique").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            boolean wasLocked = toggleLocked(stack);
            if (level.isClientSide) {
                player.displayClientMessage(Component.translatable(wasLocked ? "message.map_atlases.locked" : "message.map_atlases.unlocked"), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (player instanceof ServerPlayer sp) {
            syncAndOpenGui(sp, stack, Optional.empty(), false);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
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
            if (level.getBlockEntity(blockPos) instanceof AtlasLectern ah) {
                ah.mapatlases$setAtlas(player, stack);
                //height.sendBlockUpdated(blockPos, blockState, blockState, 3);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (blockState.is(BlockTags.BANNERS)) {
            if (!level.isClientSide) {
                MapCollection maps = getMaps(stack, level);
                AtlasMap mapUnderPlayer = maps.getMapAt(MapGridKey.atEntityPosition(maps.getScale(), getSelectedSlice(stack, level.dimension()), player));
                if (mapUnderPlayer == null || !mapUnderPlayer.data.toggleBanner(level, blockPos)) return InteractionResult.FAIL;
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        //others deco
        return super.useOn(context);
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player pPlayer) {
        super.onCraftedBy(stack, level, pPlayer);

        if (!level.isClientSide && MapAtlasesMod.SUPPLEMENTARIES && SupplementariesCompat.hasAntiqueInk(stack)) {
            SupplementariesCompat.convertAllMapsToAntique(stack, level);
        }
        validateSelectedSlices(stack, level);
    }



    //Static utility functions

    public static void syncAndOpenGui(ServerPlayer player, ItemStack atlas, @NotNull Optional<BlockPos> lecternPos, boolean pinOnly) {
        if (atlas.isEmpty()) return;
        //we need to send all data for all dimensions as they are not sent automatically
        MapCollection maps = MapAtlasItem.getMaps(atlas, player.level());
        // a lectern atlas isnt in the inventory, so vanilla would drop the holder and never send anything
        TriState carried = lecternPos.isPresent() ? TriState.SET_TRUE : TriState.PASS;
        for (var info : maps.getAllFound()) {
            // update all maps and sends them to player, if needed
            info.tickCarriedByAndSync(player, atlas, carried);
        }
        NetworkHelper.sendToClientPlayer(player, new C2S2COpenAtlasScreenPacket(lecternPos, pinOnly));
    }

    public static void removeAndDropMap(MapId id, MapType type, ItemStack atlas, ServerPlayer player) {
        Level level = player.level();
        AtlasMap holder = AtlasMap.find(id, type, level);
        if (holder != null && removeMaps(atlas, level, List.of(holder))) {
            giveMapToPlayer(player, holder);
        }
    }

    public static boolean removeMaps(ItemStack atlas, Level level, Collection<AtlasMap> holders) {
        if (!getMaps(atlas, level).removeAndAssign(atlas, holders)) return false;
        //dont leave a slice selected that has no maps left
        MapCollection remaining = getMaps(atlas, level);
        for (AtlasMap h : holders) {
            var dim = h.slice.dimension();
            boolean sliceGone = remaining.getMapsInSlice(h.slice).isEmpty();
            if (sliceGone && getSelectedSlice(atlas, dim).equals(h.slice)) {
                setSelectedSlice(atlas, remaining.closestAvailableSlice(dim, Slice.defaultVanillaFor(dim)), level);
            }
        }
        return true;
    }

    private static void giveMapToPlayer(ServerPlayer player, AtlasMap holder) {
        player.getInventory().placeItemBackInInventory(holder.createExistingMapItem());
    }

    //TODO: optimize
    @Nullable
    public static AtlasMap createMapAt(ServerPlayer player, ItemStack atlas, MapGridKey key) {
        Level level = player.level();
        MapCollection maps = MapAtlasItem.getMaps(atlas, level);
        if (maps.getCount() == 0 && MapAtlasItem.getEmptyMaps(atlas).getTotalCount() == 0) {
            // If the Atlas is "inactive", give it a pity Empty Map count
            MapAtlasItem.getEmptyMaps(atlas).setAndAssign(atlas, MapType.VANILLA, MapAtlasesConfig.pityActivationMapCount.get());
        }

        Slice slice = key.slice;
        boolean consumesEmptyMap = MapAtlasesConfig.requireEmptyMapsToExpand.get() && !player.isCreative();
        if (consumesEmptyMap && MapAtlasItem.getEmptyMaps(atlas).getCount(slice) == 0) return null;

        //validate height
        var height = slice.height();
        if (height.isPresent() && !maps.getHeightTree(level.dimension(), slice.type()).contains(height.get())) {
            MapAtlasesMod.LOGGER.error("Invalid height for slice: {} height: {}", slice, height.get());
            return null;
        }

        ItemStack newMap = slice.createNewMap(key.mapX, key.mapZ, maps.getScale(), level, atlas);
        MapId newMapId = newMap.get(DataComponents.MAP_ID);
        if (newMapId == null) return null;
        if (!maps.addAndAssign(atlas, level, slice.type(), newMapId)) return null;

        AtlasMap newData = AtlasMap.find(newMapId, slice.type(), level);
        // for custom map data to be sent immediately... crappy and hacky. TODO: change custom map data impl
        if (newData != null) {
            newData.tickCarriedByAndSync(player, newMap, TriState.SET_TRUE);
        }
        if (consumesEmptyMap) {
            //remove 1 map
            MapAtlasItem.getEmptyMaps(atlas).addAndAssign(atlas, slice, -1);
        }
        return newData;
    }

    public static boolean canPlayerTeleport(Player player) {
        return MapAtlasesConfig.creativeTeleport.get() && player.isCreative();
    }

    public static void removeAndDropSliceMaps(Slice slice, ItemStack atlas, ServerPlayer player) {
        Level level = player.level();
        Collection<AtlasMap> allInSlice = getMaps(atlas, level).getMapsInSlice(slice);
        if (!removeMaps(atlas, level, allInSlice)) return;
        for (AtlasMap holder : allInSlice) {
            giveMapToPlayer(player, holder);
        }
    }

    private static void addEmptyMapsTooltip(EmptyMaps emptyMaps, int mapCount, List<Component> tooltipComponents) {
        int emptyCount = emptyMaps.getTotalCount();
        int maxMapCount = MapAtlasesConfig.maxMapCount.get();
        if (maxMapCount != -1 && mapCount + emptyCount >= maxMapCount) {
            tooltipComponents.add(Component.translatable("item.map_atlases.atlas.tooltip_full", "", null)
                    .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
            return;
        }

        if (mapCount + emptyCount == 0) {
            int pity = MapAtlasesConfig.pityActivationMapCount.get();
            boolean usesEmptyMaps = MapAtlasesConfig.requireEmptyMapsToExpand.get() && MapAtlasesConfig.enableEmptyMapEntryAndFill.get();
            // If there are no maps & no empty maps, the atlas is "inactive", so display how many empty maps
            // they *would* receive if they activated the atlas
            if (usesEmptyMaps && pity > 0) {
                tooltipComponents.add(Component.translatable("item.map_atlases.atlas.tooltip_empty", pity).withStyle(ChatFormatting.GRAY));
            }
            return;
        }

        Map<MapType, Integer> countsPerType = emptyMaps.getAll();
        boolean hasNonVanilla = countsPerType.keySet().stream().anyMatch(type -> type != MapType.VANILLA);
        for (var entry : countsPerType.entrySet()) {
            MapType type = entry.getKey();
            int empties = entry.getValue();
            if (hasNonVanilla) {
                tooltipComponents.add(Component.translatable("item.map_atlases.atlas.tooltip_empty_type", type.getName(), empties).withStyle(ChatFormatting.GRAY));
            } else {
                tooltipComponents.add(Component.translatable("item.map_atlases.atlas.tooltip_empty", empties).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    public static void setSelectedSlice(ItemStack stack, Slice slice, Level level) {
        var dimension = slice.dimension();
        if (slice.equals(Slice.defaultVanillaFor(dimension))) {
            SelectedSlices selectedSlice = stack.get(MapAtlasesMod.SELECTED_SLICES.get());
            if (selectedSlice != null) selectedSlice.removeAndAssign(stack, dimension);
            return;
        }
        //validate:
        MapCollection maps = getMaps(stack, level);
        if (!maps.getHeightTree(dimension, slice.type()).contains(slice.heightOrTop())) return;
        stack.getOrDefault(MapAtlasesMod.SELECTED_SLICES.get(), SelectedSlices.EMPTY).putAndAssign(stack, dimension, slice);
    }

    public static MapCollection getMaps(ItemStack stack, Level level) {
        return stack.getOrDefault(MapAtlasesMod.MAP_COLLECTION.get(), MapIds.EMPTY).resolve(level);
    }

    public static int getFreeMapSlots(ItemStack atlas, Level level) {
        return MapAtlasesConfig.maxMapCount.get() - getMaps(atlas, level).getCount() - getEmptyMaps(atlas).getTotalCount();
    }

    //how many of a stack of empty maps this atlas actually takes in
    public static int countEmptyMapsToAdd(ItemStack atlas, ItemStack emptyMaps, Level level) {
        return Math.min(emptyMaps.getCount(), getFreeMapSlots(atlas, level));
    }

    public static EmptyMaps getEmptyMaps(ItemStack atlas) {
        return atlas.getOrDefault(MapAtlasesMod.EMPTY_MAPS.get(), EmptyMaps.EMPTY);
    }

    public static boolean isLocked(ItemStack stack) {
        return stack.has(MapAtlasesMod.LOCKED.get());
    }

    // returns the state before toggling
    public static boolean toggleLocked(ItemStack stack) {
        boolean locked = isLocked(stack);
        if (locked) {
            stack.remove(MapAtlasesMod.LOCKED.get());
        } else {
            stack.set(MapAtlasesMod.LOCKED.get(), Unit.INSTANCE);
        }
        return locked;
    }

    @NotNull
    public static Slice getSelectedSlice(ItemStack stack, ResourceKey<Level> dimension) {
        SelectedSlices selectedSlice = stack.get(MapAtlasesMod.SELECTED_SLICES.get());
        if (selectedSlice != null) {
            Slice slice = selectedSlice.get(dimension);
            if (slice != null) return slice;
        }
        return Slice.defaultVanillaFor(dimension);
    }

    private static void validateSelectedSlices(ItemStack stack, Level level) {
        // Populate default slices
        MapCollection maps = getMaps(stack, level);
        for (var dim : maps.getAvailableDimensions()) {
            Slice selected = getSelectedSlice(stack, dim);
            setSelectedSlice(stack, maps.closestAvailableSlice(dim, selected), level);
        }
    }

}
