package pepjebs.mapatlases.client;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.screen.AtlasOverviewScreen;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.IMapCollection;
import pepjebs.mapatlases.map_collection.MapKey;
import pepjebs.mapatlases.mixin.MapItemSavedDataAccessor;
import pepjebs.mapatlases.networking.C2S2COpenAtlasScreenPacket;
import pepjebs.mapatlases.networking.MapAtlasesNetworking;
import pepjebs.mapatlases.networking.S2CMapPacketWrapper;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.Slice;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MapAtlasesClient {
    public static final Identifier MAP_PLAYER_DECORATION_TEXTURE =
            Identifier.withDefaultNamespace("textures/map/decorations/player.png");
    public static final Identifier ATLAS_OVERLAY_TEXTURE = MapAtlasesMod.res("textures/gui/screen/atlas_overlay.png");
    public static final Identifier ATLAS_BACKGROUND_TEXTURE = MapAtlasesMod.res("textures/gui/screen/atlas_background.png");
    public static final Identifier ATLAS_BACKGROUND_TEXTURE_BIG = MapAtlasesMod.res("textures/gui/screen/atlas_background_big.png");
    public static final Identifier GUI_ICONS_TEXTURE = Identifier.withDefaultNamespace("textures/gui/icons.png");
    public static final Identifier MAP_HUD_BACKGROUND_TEXTURE = MapAtlasesMod.res("textures/gui/hud/map_background.png");
    public static final Identifier MAP_HUD_FOREGROUND_TEXTURE = MapAtlasesMod.res("textures/gui/hud/map_foreground.png");
    public static final Identifier MAP_BACKGROUND_TEXTURE = MapAtlasesMod.res("textures/gui/screen/map_background.png");
    public static final Identifier MAP_BORDER_TEXTURE = MapAtlasesMod.res("textures/gui/screen/map_border.png");
    public static final Identifier MAP_HOVERED_TEXTURE = MapAtlasesMod.res("textures/gui/screen/map_hovered.png");

    public static final List<String> DIMENSION_TEXTURE_ORDER = List.of(
            Level.OVERWORLD.identifier().toString(),
            Level.NETHER.identifier().toString(),
            Level.END.identifier().toString()
    );

    private static final KeyMapping.Category MAP_ATLASES_CATEGORY =
            KeyMapping.Category.register(pepjebs.mapatlases.MapAtlasesMod.res("minimap"));

    public static final KeyMapping OPEN_ATLAS_KEYBIND = new KeyMapping(
            "key.map_atlases.open_minimap",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            MAP_ATLASES_CATEGORY
    );

    public static final KeyMapping PLACE_PIN_KEYBIND = new KeyMapping(
            "key.map_atlases.place_pin",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            MAP_ATLASES_CATEGORY
    );

    public static final KeyMapping INCREASE_MINIMAP_ZOOM = new KeyMapping(
            "key.map_atlases.zoom_in_minimap",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_ADD,
            MAP_ATLASES_CATEGORY
    );

    public static final KeyMapping DECREASE_MINIMAP_ZOOM = new KeyMapping(
            "key.map_atlases.zoom_out_minimap",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_SUBTRACT,
            MAP_ATLASES_CATEGORY
    );

    public static final KeyMapping INCREASE_SLICE = new KeyMapping(
            "key.map_atlases.increase_slice",
            InputConstants.UNKNOWN.getValue(),
            MAP_ATLASES_CATEGORY
    );

    public static final KeyMapping DECREASE_SLICE = new KeyMapping(
            "key.map_atlases.decrease_slice",
            InputConstants.UNKNOWN.getValue(),
            MAP_ATLASES_CATEGORY
    );

    @Nullable
    private static MapKey currentActiveMapKey;
    private static MapDataHolder currentActiveMap;
    private static ItemStack currentActiveAtlas = ItemStack.EMPTY;
    @Nullable
    private static PendingOpenScreen pendingOpenScreen;
    private static boolean isDrawingAtlas;
    private static float decorationScale = 1.0F;
    private static float decorationTextScale = 1.0F;
    private static float decorationRotation;

    public static void init() {
        MapAtlasesNetworking.initClient();
    }

    public static void cachePlayerState(Player player) {
        if (player != Minecraft.getInstance().player) {
            return;
        }

        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
        currentActiveAtlas = atlas;
        currentActiveMap = null;
        currentActiveMapKey = null;
        if (!atlas.isEmpty()) {
            var maps = MapAtlasItem.getMaps(atlas, player.level());
            maps.addNotSynced(player.level());
            Slice slice = MapAtlasItem.getSelectedSlice(atlas, player.level().dimension());
            currentActiveMapKey = MapKey.at(maps.getScale(), player, slice);
            MapDataHolder selected = maps.select(currentActiveMapKey);
            if (selected == null) {
                selected = maps.getClosest(player, slice);
            }
            if (selected != null) {
                currentActiveMapKey = selected.makeKey();
                currentActiveMap = selected;
            }
        }
        tryOpenPendingScreen();
    }

    public static ItemStack getCurrentActiveAtlas() {
        return currentActiveAtlas;
    }

    public static MapKey getActiveMapKey() {
        return currentActiveMapKey;
    }

    public static MapDataHolder getActiveMap() {
        return currentActiveMap;
    }

    public static void setIsDrawingAtlas(boolean state) {
        isDrawingAtlas = state;
    }

    public static boolean isDrawingAtlas() {
        return isDrawingAtlas;
    }

    public static float getPredicateForAtlas(ItemStack stack, ClientLevel world, LivingEntity entity, int seed) {
        if (world == null && entity != null) {
            world = (ClientLevel) entity.level();
        }
        if (world == null) {
            return 0.0F;
        }
        boolean unlocked = !MapAtlasItem.isLocked(stack);
        ResourceKey<Level> dimension = world.dimension();
        int i = DIMENSION_TEXTURE_ORDER.indexOf(dimension.identifier().toString());
        if (i == -1) {
            return unlocked ? 0.96F : 1.0F;
        }
        return i / 10.0F + (unlocked ? 0.0F : 0.05F);
    }

    public static void handleMapPacketWrapperPacket(S2CMapPacketWrapper packet) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return;
        }

        minecraft.player.connection.handleMapItemData(packet.packet);

        var data = level.getMapData(new MapId(packet.mapId));
        if (data instanceof MapItemSavedDataAccessor accessor) {
            accessor.setCenterX(packet.centerX);
            accessor.setCenterZ(packet.centerZ);
            accessor.setDimension(ResourceKey.create(Registries.DIMENSION, packet.dimension));
        }
        tryOpenPendingScreen();
    }

    public static void openScreen(
            C2S2COpenAtlasScreenPacket.OpenSource source,
            @Nullable InteractionHand hand,
            @Nullable BlockPos lecternPos,
            boolean pinOnly
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }

        LecternBlockEntity lectern = null;
        ItemStack atlas = switch (source) {
            case HAND -> hand == null ? ItemStack.EMPTY : player.getItemInHand(hand);
            case ACTIVE_ATLAS -> MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
            case LECTERN -> {
                if (lecternPos != null && player.level().getBlockEntity(lecternPos) instanceof LecternBlockEntity lecternBlockEntity) {
                    lectern = lecternBlockEntity;
                    yield lecternBlockEntity.getBook();
                }
                yield ItemStack.EMPTY;
            }
        };

        if (atlas.getItem() instanceof MapAtlasItem) {
            openScreen(source, hand, lecternPos, atlas, lectern, pinOnly);
        }
    }

    public static void openScreen(
            C2S2COpenAtlasScreenPacket.OpenSource source,
            @Nullable InteractionHand hand,
            @Nullable BlockPos lecternPos,
            ItemStack atlas,
            @Nullable LecternBlockEntity lectern,
            boolean pinOnly
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        IMapCollection maps = MapAtlasItem.getMaps(atlas, level);
        maps.addNotSynced(level);
        int persistedIds = maps.getAllIds().length;
        int resolvedMaps = maps.getAll().size();
        if (persistedIds > 0 && resolvedMaps == persistedIds) {
            minecraft.setScreen(new AtlasOverviewScreen(atlas, lectern, pinOnly));
            pendingOpenScreen = null;
        } else if (persistedIds > 0) {
            pendingOpenScreen = new PendingOpenScreen(source, hand, lecternPos, pinOnly);
        }
    }

    public static void clearPendingOpenScreen() {
        pendingOpenScreen = null;
    }

    private static void tryOpenPendingScreen() {
        if (pendingOpenScreen == null) {
            return;
        }
        PendingOpenScreen pending = pendingOpenScreen;
        openScreen(pending.source(), pending.hand(), pending.lecternPos(), pending.pinOnly());
    }

    public static ContainerLevelAccess getClientAccess() {
        return ContainerLevelAccess.create(Minecraft.getInstance().level, BlockPos.ZERO);
    }

    public static void modifyTextDecorationTransform(PoseStack poseStack, float textWidth, float textScale) {
        float s = textWidth * textScale / 2.0F;
        poseStack.translate(s, -4, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(decorationRotation));
        poseStack.translate(-s * decorationTextScale, 4 * decorationTextScale, 0);
        poseStack.scale(decorationTextScale, decorationTextScale, 1);
    }

    public static void modifyDecorationTransform(PoseStack poseStack) {
        poseStack.mulPose(Axis.ZP.rotationDegrees(decorationRotation));
        poseStack.scale(decorationScale, decorationScale, 1);
    }

    public static void setDecorationsScale(float i) {
        decorationScale = i;
    }

    public static float getDecorationsScale() {
        return decorationScale;
    }

    public static void setDecorationsTextScale(float i) {
        decorationTextScale = i;
    }

    public static void setDecorationRotation(float i) {
        decorationRotation = i;
    }

    public static Identifier getDecorationTexture(MapDecoration decoration) {
        Identifier sprite = decoration.getSpriteLocation();
        String path = sprite.getPath();
        if (path.contains("/") || path.endsWith(".png")) {
            return sprite;
        }
        return Identifier.fromNamespaceAndPath(sprite.getNamespace(), "textures/map/decorations/" + path + ".png");
    }

    public static void debugMapUpdated(String mapId) {
        CACHE.put(mapId, 10);
    }

    public static int debugIsMapUpdated(int light, String stringId) {
        Integer value = CACHE.getIfPresent(stringId);
        if (value != null) {
            value--;
            if (value <= 0) {
                CACHE.invalidate(stringId);
            } else {
                CACHE.put(stringId, value);
            }
            int packedLight = Mth.clamp((int) (value / 10.0F * 15.0F), 0, 15);
            return packedLight | packedLight << 20;
        }
        return light;
    }

    public static int uploadFrequency() {
        return 1;
    }

    public static Map<String, MapDecoration> getMutableDecorations(MapItemSavedData data) {
        if (data instanceof MapItemSavedDataAccessor accessor) {
            return accessor.getDecorations();
        }
        throw new IllegalStateException("Expected accessor-backed MapItemSavedData for atlas decoration access");
    }

    public static void markDecorationsDirty(MapItemSavedData data) {
        if (data instanceof MapItemSavedDataAccessor accessor) {
            accessor.invokeSetDecorationsDirty();
            return;
        }
        throw new IllegalStateException("Expected accessor-backed MapItemSavedData for atlas decoration dirtying");
    }

    public static void decreaseHoodZoom() {
        pepjebs.mapatlases.client.fabric.MapAtlasesClientImpl.decreaseHoodZoom();
    }

    public static void increaseHoodZoom() {
        pepjebs.mapatlases.client.fabric.MapAtlasesClientImpl.increaseHoodZoom();
    }

    private static final Cache<String, Integer> CACHE = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterAccess(10, TimeUnit.SECONDS)
            .build();

    private record PendingOpenScreen(
            C2S2COpenAtlasScreenPacket.OpenSource source,
            @Nullable InteractionHand hand,
            @Nullable BlockPos lecternPos,
            boolean pinOnly
    ) {
    }
}
