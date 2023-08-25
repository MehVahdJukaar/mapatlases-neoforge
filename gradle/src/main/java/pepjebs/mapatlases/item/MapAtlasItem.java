package pepjebs.mapatlases.item;

import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.map.MapState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.lifecycle.MapAtlasesServerLifecycleEvents;
import pepjebs.mapatlases.screen.MapAtlasesAtlasOverviewScreenHandler;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapAtlasItem extends Item implements ExtendedScreenHandlerFactory {

    public static final String EMPTY_MAP_NBT = "empty";
    public static final String MAP_LIST_NBT = "maps";

    public static final BooleanProperty HAS_ATLAS = BooleanProperty.of("has_atlas");

    public MapAtlasItem(Settings settings) {
        super(settings);
    }

    public static int getMaxMapCount() {
        if (MapAtlasesMod.CONFIG != null) {
            return MapAtlasesMod.CONFIG.maxMapCount;
        }
        return 128;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        if (world != null && world.isClient) {
            int mapSize = MapAtlasesAccessUtils.getMapCountFromItemStack(stack);
            int empties = MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(stack);
            if (getMaxMapCount() != -1 && mapSize + empties >= getMaxMapCount()) {
                tooltip.add(Text.translatable("item.map_atlases.atlas.tooltip_full", "", null)
                        .formatted(Formatting.ITALIC).formatted(Formatting.GRAY));
            }
            tooltip.add(Text.translatable("item.map_atlases.atlas.tooltip_1", mapSize)
                    .formatted(Formatting.GRAY));
            if (MapAtlasesMod.CONFIG == null || (MapAtlasesMod.CONFIG.requireEmptyMapsToExpand && MapAtlasesMod.CONFIG.enableEmptyMapEntryAndFill)) {
                // If there's no maps & no empty maps, the atlas is "inactive", so display how many empty maps
                // they *would* receive if they activated the atlas
                if (mapSize + empties == 0 && MapAtlasesMod.CONFIG != null) {
                    empties = MapAtlasesMod.CONFIG.pityActivationMapCount;
                }
                tooltip.add(Text.translatable("item.map_atlases.atlas.tooltip_2", empties)
                        .formatted(Formatting.GRAY));
            }
            MapState mapState = world.getMapState(MapAtlasesClient.currentMapStateId);
            if (mapState == null) return;
            tooltip.add(Text.translatable("item.map_atlases.atlas.tooltip_3", 1 << mapState.scale)
                    .formatted(Formatting.GRAY));
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        openHandledAtlasScreen(world, player);
        return TypedActionResult.consume(player.getStackInHand(hand));
    }

    public void openHandledAtlasScreen(World world, PlayerEntity player) {
        player.openHandledScreen(this);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable(getTranslationKey());
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        ItemStack atlas = getAtlasFromLookingLectern(player);
        if (atlas.isEmpty()) {
            atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
        }
        Map<Integer, Pair<String,List<Integer>>> idsToCenters = new HashMap<>();
        Map<String, MapState> mapInfos = MapAtlasesAccessUtils.getAllMapInfoFromAtlas(player.getWorld(), atlas);
        for (Map.Entry<String, MapState> state : mapInfos.entrySet()) {
            var id = MapAtlasesAccessUtils.getMapIntFromString(state.getKey());
            var centers = Arrays.asList(state.getValue().centerX, state.getValue().centerZ);
            var dimStr = MapAtlasesAccessUtils.getMapStateDimKey(state.getValue());
            idsToCenters.put(id, new Pair<>(dimStr, centers));
        }
        var currentIds = MapAtlasesAccessUtils.getCurrentDimMapInfoFromAtlas(player.getWorld(), atlas);
        // TODO: Sometimes throws bc of null when opening Lectern
        String centerMap = MapAtlasesAccessUtils
                .getActiveAtlasMapStateServer(currentIds, (ServerPlayerEntity) player).getKey();
        int atlasScale = MapAtlasesAccessUtils.getAtlasBlockScale(player.getWorld(), atlas);
        return new MapAtlasesAtlasOverviewScreenHandler(syncId, inv, idsToCenters, atlas, centerMap, atlasScale);
    }

    public ItemStack getAtlasFromLookingLectern(PlayerEntity player) {
        HitResult h = player.raycast(10, 1, false);
        if (h.getType() == HitResult.Type.BLOCK) {
            BlockEntity e = player.getWorld().getBlockEntity(BlockPos.ofFloored(h.getPos()));
            if (e instanceof LecternBlockEntity) {
                ItemStack book = ((LecternBlockEntity) e).getBook();
                if (book.getItem() == MapAtlasesMod.MAP_ATLAS) {
                    return book;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private void sendPlayerLecternAtlasData(ServerPlayerEntity serverPlayerEntity, ItemStack atlas){
        // Send player all MapStates
        var states =
                MapAtlasesAccessUtils.getAllMapInfoFromAtlas(serverPlayerEntity.getWorld(), atlas);
        for (var state : states.entrySet()) {
            state.getValue().getPlayerSyncData(serverPlayerEntity);
            MapAtlasesServerLifecycleEvents.relayMapStateSyncToPlayerClient(state, serverPlayerEntity);
        }
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity serverPlayerEntity, PacketByteBuf packetByteBuf) {
        ItemStack atlas = getAtlasFromLookingLectern(serverPlayerEntity);
        if (atlas.isEmpty()) {
            atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(serverPlayerEntity);
        } else {
            sendPlayerLecternAtlasData(serverPlayerEntity, atlas);
        }
        if (atlas.isEmpty()) return;
        var mapInfos = MapAtlasesAccessUtils.getAllMapInfoFromAtlas(
                serverPlayerEntity.getWorld(), atlas);
        var currentInfos = MapAtlasesAccessUtils.getCurrentDimMapInfoFromAtlas(
                serverPlayerEntity.getWorld(), atlas);
        String centerMap = MapAtlasesAccessUtils
                .getActiveAtlasMapStateServer(currentInfos, serverPlayerEntity).getKey();
        int atlasScale = MapAtlasesAccessUtils.getAtlasBlockScale(serverPlayerEntity.getWorld(), atlas);
        packetByteBuf.writeItemStack(atlas);
        packetByteBuf.writeString(centerMap);
        packetByteBuf.writeInt(atlasScale);
        packetByteBuf.writeInt(mapInfos.size());
        for (Map.Entry<String, MapState> state : mapInfos.entrySet()) {
            packetByteBuf.writeInt(MapAtlasesAccessUtils.getMapIntFromString(state.getKey()));
            packetByteBuf.writeString(MapAtlasesAccessUtils.getMapStateDimKey(state.getValue()));
            packetByteBuf.writeInt(state.getValue().centerX);
            packetByteBuf.writeInt(state.getValue().centerZ);
        }
    }

    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer() == null || context.getWorld() == null
                || context.getStack() == null || context.getBlockPos() == null)
            return super.useOnBlock(context);
        BlockState blockState = context.getWorld().getBlockState(context.getBlockPos());
        if (blockState.isOf(Blocks.LECTERN)) {
            boolean didPut = LecternBlock.putBookIfAbsent(
                    context.getPlayer(),
                    context.getWorld(),
                    context.getBlockPos(),
                    blockState,
                    context.getStack()
            );
            if (!didPut) {
                return ActionResult.PASS;
            }
            blockState = context.getWorld().getBlockState(context.getBlockPos());
            LecternBlock.setHasBook(
                    context.getPlayer(), context.getWorld(), context.getBlockPos(), blockState, true);
            context.getWorld().setBlockState(context.getBlockPos(), blockState.with(HAS_ATLAS, true));
            return ActionResult.success(context.getWorld().isClient);
        } if (blockState.isIn(BlockTags.BANNERS)) {
            if (!context.getWorld().isClient) {
                Map<String, MapState> currentDimMapInfos = MapAtlasesAccessUtils.getCurrentDimMapInfoFromAtlas(
                        context.getWorld(), context.getStack());
                MapState mapState = MapAtlasesAccessUtils.getActiveAtlasMapStateServer(
                        currentDimMapInfos, (ServerPlayerEntity) context.getPlayer()).getValue();
                if (mapState == null)
                    return ActionResult.FAIL;
                boolean didAdd = mapState.addBanner(context.getWorld(), context.getBlockPos());
                if (!didAdd)
                    return ActionResult.FAIL;
            }
            return ActionResult.success(context.getWorld().isClient);
        } else {
            return super.useOnBlock(context);
        }
    }
}
