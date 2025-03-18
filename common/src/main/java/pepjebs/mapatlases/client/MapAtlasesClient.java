package pepjebs.mapatlases.client;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.mehvahdjukaar.moonlight.api.platform.ClientHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.screen.AtlasOverviewScreen;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.map_collection.MapSearchKey;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.MapType;
import pepjebs.mapatlases.utils.Slice;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class MapAtlasesClient {

    public static final Material OVERWORLD_TEXTURE =
            new Material(InventoryMenu.BLOCK_ATLAS, MapAtlasesMod.res("entity/lectern_atlas"));
    public static final Material NETHER_TEXTURE =
            new Material(InventoryMenu.BLOCK_ATLAS, MapAtlasesMod.res("entity/lectern_atlas_nether"));
    public static final Material END_TEXTURE =
            new Material(InventoryMenu.BLOCK_ATLAS, MapAtlasesMod.res("entity/lectern_atlas_end"));
    public static final Material OTHER_TEXTURE =
            new Material(InventoryMenu.BLOCK_ATLAS, MapAtlasesMod.res("entity/lectern_atlas_unknown"));

    public static final Material MAP_BORDER_TEXTURE = new Material(
            ResourceLocation.withDefaultNamespace("textures/atlas/shulker_boxes.png"), //so we have mipmap here too
            MapAtlasesMod.res("gui/screen/map_border"));

    public static final Material MAP_HOVERED_TEXTURE = new Material(
            ResourceLocation.withDefaultNamespace("textures/atlas/shulker_boxes.png"), //so we have mipmap here too
            MapAtlasesMod.res("gui/screen/map_hovered"));

    public static final Material MAP_BACKGROUND_TEXTURE = new Material(
            ResourceLocation.withDefaultNamespace("textures/atlas/shulker_boxes.png"), //so we have mipmap here too
            MapAtlasesMod.res("gui/screen/map_background"));

    //sprites
    public static final ResourceLocation PLAYER_MARKER_SPRITE = MapAtlasesMod.res("player_marker");
    public static final ResourceLocation BOOKMARK_LEFT_SPRITE = MapAtlasesMod.res("bookmark_left");
    public static final ResourceLocation BOOKMARK_LEFT_SELECTED_SPRITE = MapAtlasesMod.res("bookmark_left_selected");
    public static final ResourceLocation BOOKMARK_RIGHT_SPRITE = MapAtlasesMod.res("bookmark_right");
    public static final ResourceLocation BOOKMARK_RIGHT_SELECTED_SPRITE = MapAtlasesMod.res("bookmark_right_selected");
    public static final ResourceLocation SLICE_BOOKMARK_SPRITE = MapAtlasesMod.res("slice_bookmark");

    public static final ResourceLocation CARTOGRAPHY_TABLE_LEFT_SPRITE = MapAtlasesMod.res("cartography_table_left");
    public static final ResourceLocation CARTOGRAPHY_TABLE_LEFT_SELECTED_SPRITE = MapAtlasesMod.res("cartography_table_left_selected");
    public static final ResourceLocation CARTOGRAPHY_TABLE_LEFT_HOVERED_SPRITE = MapAtlasesMod.res("cartography_table_left_hovered");
    public static final ResourceLocation CARTOGRAPHY_TABLE_RIGHT_SPRITE = MapAtlasesMod.res("cartography_table_right");
    public static final ResourceLocation CARTOGRAPHY_TABLE_RIGHT_SELECTED_SPRITE = MapAtlasesMod.res("cartography_table_right_selected");
    public static final ResourceLocation CARTOGRAPHY_TABLE_RIGHT_HOVERED_SPRITE = MapAtlasesMod.res("cartography_table_right_hovered");

    public static final ResourceLocation DELETE_MARKER_SPRITE = MapAtlasesMod.res("delete_marker");
    public static final ResourceLocation FOCUS_MARKER_SPRITE = MapAtlasesMod.res("focus_marker");
    public static final ResourceLocation PIN_BUTTON_SPRITE = MapAtlasesMod.res("pin_button");
    public static final ResourceLocation PIN_BUTTON_HOVERED_SPRITE = MapAtlasesMod.res("pin_button_hovered");
    public static final ResourceLocation SHEAR_BUTTON_SPRITE = MapAtlasesMod.res("shear_button");
    public static final ResourceLocation SHEAR_BUTTON_HOVERED_SPRITE = MapAtlasesMod.res("shear_button_hovered");
    public static final ResourceLocation SHEAR_MAP_SPRITE = MapAtlasesMod.res("shear_map");
    public static final ResourceLocation PLACE_PIN_SPRITE = MapAtlasesMod.res("place_pin");
    public static final ResourceLocation ZOOM_IN_BUTTON_SPRITE = MapAtlasesMod.res("zoom_in_button");
    public static final ResourceLocation ZOOM_IN_BUTTON_HOVERED_SPRITE = MapAtlasesMod.res("zoom_in_button_hovered");

    public static final ResourceLocation SLICE_DOWN_SPRITE = MapAtlasesMod.res("slice_down");
    public static final ResourceLocation SLICE_DOWN_HOVERED_SPRITE = MapAtlasesMod.res("slice_down_hovered");
    public static final ResourceLocation SLICE_DOWN_INACTIVE_SPRITE = MapAtlasesMod.res("slice_down_inactive");
    public static final ResourceLocation SLICE_UP_SPRITE = MapAtlasesMod.res("slice_up");
    public static final ResourceLocation SLICE_UP_HOVERED_SPRITE = MapAtlasesMod.res("slice_up_hovered");
    public static final ResourceLocation SLICE_UP_INACTIVE_SPRITE = MapAtlasesMod.res("slice_up_inactive");


    public static final ResourceLocation MAP_OVERWORLD_SPRITE = MapAtlasesMod.res("map_overworld");
    public static final ResourceLocation MAP_AETHER_SPRITE = MapAtlasesMod.res("map_the_aether");
    public static final ResourceLocation MAP_END_SPRITE = MapAtlasesMod.res("map_end");
    public static final ResourceLocation MAP_NETHER_SPRITE = MapAtlasesMod.res("map_nether");
    public static final ResourceLocation MAP_BUMBLEZONE_SPRITE = MapAtlasesMod.res("map_the_bumblezone");

    public static final ResourceLocation MAP_TYPE_VANILLA_SPRITE = MapAtlasesMod.res("map_type_vanilla");
    public static final ResourceLocation MAP_TYPE_MAGIC_SPRITE = MapAtlasesMod.res("map_type_magic");
    public static final ResourceLocation MAP_TYPE_MAZE_SPRITE = MapAtlasesMod.res("map_type_maze");
    public static final ResourceLocation MAP_TYPE_ORE_SPRITE = MapAtlasesMod.res("map_type_ore");


    public static final ResourceLocation ATLAS_OVERLAY_TEXTURE = MapAtlasesMod.res("textures/gui/screen/atlas_overlay.png");
    public static final ResourceLocation ATLAS_BACKGROUND_TEXTURE = MapAtlasesMod.res("textures/gui/screen/atlas_background.png");
    public static final ResourceLocation ATLAS_BACKGROUND_TEXTURE_BIG = MapAtlasesMod.res("textures/gui/screen/atlas_background_big.png");
    public static final ResourceLocation GUI_ICONS_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/icons.png");
    public static final ResourceLocation MAP_HUD_BACKGROUND_TEXTURE = MapAtlasesMod.res("textures/gui/hud/map_background.png");
    public static final ResourceLocation MAP_HUD_FOREGROUND_TEXTURE = MapAtlasesMod.res("textures/gui/hud/map_foreground.png");


    public static final List<String> DIMENSION_TEXTURE_ORDER = List.of(Level.OVERWORLD.location().toString(),
            Level.NETHER.location().toString(), Level.END.location().toString(),
            "aether:the_aether", "twilightforest:twilight_forest", "undergarden:undergarden",
            "tropicraft:tropics", "thebetweenlands:betweenlands", "blue_skies:everbright",
            "the_bumblezone:the_bumblezone");

    public static final KeyMapping OPEN_ATLAS_KEYBIND = new KeyMapping(
            "key.map_atlases.open_minimap",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "category.map_atlases.minimap"
    );

    public static final KeyMapping PLACE_PIN_KEYBIND = new KeyMapping(
            "key.map_atlases.place_pin",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "category.map_atlases.minimap"
    );

    public static final KeyMapping INCREASE_MINIMAP_ZOOM = new KeyMapping(
            "key.map_atlases.zoom_in_minimap",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_ADD,
            "category.map_atlases.minimap"
    );

    public static final KeyMapping DECREASE_MINIMAP_ZOOM = new KeyMapping(
            "key.map_atlases.zoom_out_minimap",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_SUBTRACT,
            "category.map_atlases.minimap"
    );

    public static final KeyMapping INCREASE_SLICE = new KeyMapping(
            "key.map_atlases.increase_slice",
            InputConstants.UNKNOWN.getValue(),
            "category.map_atlases.minimap"
    );

    public static final KeyMapping DECREASE_SLICE = new KeyMapping(
            "key.map_atlases.decrease_slice",
            InputConstants.UNKNOWN.getValue(),
            "category.map_atlases.minimap"
    );


    private static final ThreadLocal<Float> globalDecorationScale = ThreadLocal.withInitial(() -> 1f);
    private static final ThreadLocal<Float> globalDecorationTextScale = ThreadLocal.withInitial(() -> 1f);
    private static final ThreadLocal<Float> globalDecorationRotation = ThreadLocal.withInitial(() -> 0f);

    @Nullable
    private static MapSearchKey currentActiveMapKey = null;
    private static MapDataHolder currentActiveMap = null;
    private static ItemStack currentActiveAtlas = ItemStack.EMPTY;
    private static boolean isDrawingAtlas = false;


    public static void cachePlayerState(Player player) {

        if (player != Minecraft.getInstance().player) return;
        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
        currentActiveAtlas = atlas;
        currentActiveMap = null;
        currentActiveMapKey = null;
        if (!atlas.isEmpty()) {
            MapCollection maps = MapAtlasItem.getMaps(atlas, player.level());
            maps.updateNotSynced(player.level());
            Slice slice = MapAtlasItem.getSelectedSlice(atlas, player.level().dimension());
            // I hate this
            currentActiveMapKey = MapSearchKey.at(maps.getScale(), player, slice);
            MapDataHolder select = maps.select(currentActiveMapKey);
            if (select == null) {
                select = maps.getClosest(player, slice);
            }
            if (select != null) {
                currentActiveMapKey = select.makeKey();
                currentActiveMap = select;
            }
        }
    }

    public static ItemStack getCurrentActiveAtlas() {
        return currentActiveAtlas;
    }

    public static MapSearchKey getActiveMapKey() {
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

    public static void init() {
        ClientHelper.addKeyBindRegistration(MapAtlasesClient::registerKeyBinding);
        ClientHelper.addClientSetup(MapAtlasesClient::clientSetup);
    }

    public static void clientSetup() {
        // Register ModelPredicate
        ItemProperties.register(MapAtlasesMod.MAP_ATLAS.get(), MapAtlasesMod.res("atlas"),
                MapAtlasesClient::getPredicateForAtlas);
    }

    public static void registerKeyBinding(ClientHelper.KeyBindEvent event) {
        event.register(OPEN_ATLAS_KEYBIND);
        event.register(DECREASE_MINIMAP_ZOOM);
        event.register(INCREASE_MINIMAP_ZOOM);
        event.register(PLACE_PIN_KEYBIND);
        if (MapAtlasesMod.TWILIGHTFOREST || MapAtlasesMod.SUPPLEMENTARIES) {
            event.register(INCREASE_SLICE);
            event.register(DECREASE_SLICE);
        }
    }

    public static float getPredicateForAtlas(ItemStack stack, ClientLevel world, LivingEntity entity, int seed) {
        // Using ClientLevel will render default Atlas in inventories
        if (world == null && entity != null)
            world = (ClientLevel) entity.level();
        if (world == null) return 0.0f;
        boolean unlocked = !MapAtlasItem.isLocked(stack);

        ResourceKey<Level> dimension = world.dimension();
        int i = DIMENSION_TEXTURE_ORDER.indexOf(dimension.location().toString());
        if (i == -1) return unlocked ? 0.96f : 1;
        return i / 10f + (unlocked ? 0 : 0.05f);
    }

    /*
    public static void handleMapPacketWrapperPacket(S2CMapPacketWrapper packet) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        Minecraft.getInstance().player.connection.handleMapItemData(packet.packet);

        var data = level.getMapData(packet.packet.mapId());
        if (data instanceof MapItemSavedDataAccessor d) {
            try {
                d.setCenterX(packet.centerX);
                d.setCenterZ(packet.centerZ);
                d.setDimension(ResourceKey.create(Registries.DIMENSION, packet.dimension));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    */

    public static void openScreen(Optional<BlockPos> lecternPos, boolean pinOnly) {
        @Nullable LecternBlockEntity lectern = null;
        ItemStack atlas = ItemStack.EMPTY;
        Player player = Minecraft.getInstance().player;
        if (lecternPos.isEmpty()) {
            atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
        } else {
            if (player.level().getBlockEntity(lecternPos.get()) instanceof LecternBlockEntity lec) {
                lectern = lec;
                atlas = lec.getBook();
            }
        }
        if (atlas.getItem() instanceof MapAtlasItem) {
            openScreen(atlas, lectern, pinOnly);
        }
    }

    public static void openScreen(ItemStack atlas, @Nullable LecternBlockEntity lectern, boolean pinOnly) {
        ClientLevel level = Minecraft.getInstance().level;
        var maps = MapAtlasItem.getMaps(atlas, level);
        //we arent ticking these so we have to fix duplicates
        maps.updateNotSynced(level);
        if (!maps.isEmpty()) {
            Minecraft.getInstance().setScreen(new AtlasOverviewScreen(atlas, lectern, pinOnly));
        }
    }

    //hack
    public static ContainerLevelAccess getClientAccess() {
        return ContainerLevelAccess.create(Minecraft.getInstance().level, BlockPos.ZERO);
    }

    public static void modifyTextDecorationTransform(PoseStack poseStack, float textWidth, float textScale) {
        Float scale = globalDecorationTextScale.get();
        if (scale != null) {
            float s = textWidth * textScale / 2.0F;
            poseStack.translate(s, -4, 0);

            Float rot = globalDecorationRotation.get();
            if (rot != null) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(rot));
            }
            poseStack.translate(-s * scale, 4 * scale, 0);

            poseStack.scale(scale, scale, 1);
        }
    }

    public static void modifyDecorationTransform(PoseStack poseStack) {
        Float rot = globalDecorationRotation.get();
        if (rot != null) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(rot));
        }
        Float scale = globalDecorationScale.get();
        if (scale != null) {
            poseStack.scale(scale, scale, 1);
        }
    }

    @Deprecated(forRemoval = true)
    public static float getWorldMapZoomLevel() {
        return globalDecorationScale.get();
    }

    public static void setDecorationsScale(float i) {
        globalDecorationScale.set(i);
    }

    public static void setDecorationsTextScale(float i) {
        globalDecorationTextScale.set(i);
    }

    public static void setDecorationRotation(float i) {
        globalDecorationRotation.set(i);
    }

    //debug stuff
    public static void debugMapUpdated(MapId mapId, MapType mapType) {
        CACHE.put(Pair.of(mapId, mapType), 10);
    }

    public static int debugIsMapUpdated(int light, MapId stringId, MapType mapType) {
        Integer value = getValue(stringId, mapType);
        if (value != null) {
            int pBlockLight = Mth.clamp((int) (value / (float) 10 * 15f), 0, 15);
            return LightTexture.pack(pBlockLight, pBlockLight);
        }
        return light;
    }

    private static Integer getValue(MapId id, MapType type) {
        var key = Pair.of(id, type);
        Integer value = CACHE.getIfPresent(key);
        if (value != null) {
            value--; // Decrease the counter
            if (value <= 0) {
                // Value has reached its limit, remove it from the cache
                CACHE.invalidate(key);
            } else {
                // Update the value in the cache
                CACHE.put(key, value);
            }
        }
        return value;
    }

    private static final Cache<Pair<MapId, MapType>, Integer> CACHE = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterAccess(10, TimeUnit.SECONDS)
            .build();

    @ExpectPlatform
    public static void decreaseHoodZoom() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void increaseHoodZoom() {
        throw new AssertionError();
    }

    public static Level getLevel() {
        return Minecraft.getInstance().level;
    }
}