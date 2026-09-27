package pepjebs.mapatlases.client;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import net.mehvahdjukaar.candlelight.api.PlatformImpl;
import net.mehvahdjukaar.moonlight.api.platform.ClientHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
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
import pepjebs.mapatlases.map_collection.MapGridKey;
import pepjebs.mapatlases.utils.AtlasLookup;
import pepjebs.mapatlases.utils.AtlasMap;
import pepjebs.mapatlases.utils.MapType;
import pepjebs.mapatlases.utils.Slice;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class MapAtlasesClient {

    public static final SpriteId OVERWORLD_TEXTURE =
            new SpriteId(TextureAtlas.LOCATION_BLOCKS, MapAtlasesMod.res("entity/lectern_atlas"));
    public static final SpriteId NETHER_TEXTURE =
            new SpriteId(TextureAtlas.LOCATION_BLOCKS, MapAtlasesMod.res("entity/lectern_atlas_nether"));
    public static final SpriteId END_TEXTURE =
            new SpriteId(TextureAtlas.LOCATION_BLOCKS, MapAtlasesMod.res("entity/lectern_atlas_end"));
    public static final SpriteId OTHER_TEXTURE =
            new SpriteId(TextureAtlas.LOCATION_BLOCKS, MapAtlasesMod.res("entity/lectern_atlas_unknown"));

    //not Sheets.SHULKER_SHEET, that would load Sheets too early
    private static final Identifier SHULKER_SHEET = Identifier.withDefaultNamespace("textures/atlas/shulker_boxes.png");

    public static final SpriteId MAP_BORDER_TEXTURE = new SpriteId(
            SHULKER_SHEET, //so we have mipmap here too
            MapAtlasesMod.res("gui/screen/map_border"));

    public static final SpriteId MAP_HOVERED_TEXTURE = new SpriteId(
            SHULKER_SHEET, //so we have mipmap here too
            MapAtlasesMod.res("gui/screen/map_hovered"));

    public static final SpriteId MAP_BACKGROUND_TEXTURE = new SpriteId(
            SHULKER_SHEET, //so we have mipmap here too
            MapAtlasesMod.res("gui/screen/map_background"));

    //sprites
    public static final Identifier PLAYER_MARKER_SPRITE = MapAtlasesMod.res("player_marker");
    public static final Identifier BOOKMARK_LEFT_SPRITE = MapAtlasesMod.res("bookmark_left");
    public static final Identifier BOOKMARK_LEFT_SELECTED_SPRITE = MapAtlasesMod.res("bookmark_left_selected");
    public static final Identifier BOOKMARK_RIGHT_SPRITE = MapAtlasesMod.res("bookmark_right");
    public static final Identifier BOOKMARK_RIGHT_SELECTED_SPRITE = MapAtlasesMod.res("bookmark_right_selected");
    public static final Identifier SLICE_BOOKMARK_SPRITE = MapAtlasesMod.res("slice_bookmark");

    public static final Identifier CARTOGRAPHY_TABLE_LEFT_SPRITE = MapAtlasesMod.res("cartography_table_left");
    public static final Identifier CARTOGRAPHY_TABLE_LEFT_HOVERED_SPRITE = MapAtlasesMod.res("cartography_table_left_hovered");
    public static final Identifier CARTOGRAPHY_TABLE_RIGHT_SPRITE = MapAtlasesMod.res("cartography_table_right");
    public static final Identifier CARTOGRAPHY_TABLE_RIGHT_HOVERED_SPRITE = MapAtlasesMod.res("cartography_table_right_hovered");

    public static final Identifier DELETE_MARKER_SPRITE = MapAtlasesMod.res("delete_marker");
    public static final Identifier FOCUS_MARKER_SPRITE = MapAtlasesMod.res("focus_marker");
    public static final Identifier PIN_BUTTON_SPRITE = MapAtlasesMod.res("pin_button");
    public static final Identifier PIN_BUTTON_HOVERED_SPRITE = MapAtlasesMod.res("pin_button_hovered");
    public static final Identifier SHEAR_BUTTON_SPRITE = MapAtlasesMod.res("shear_button");
    public static final Identifier SHEAR_BUTTON_HOVERED_SPRITE = MapAtlasesMod.res("shear_button_hovered");
    public static final Identifier SHEAR_MAP_SPRITE = MapAtlasesMod.res("shear_map");
    public static final Identifier SHEAR_MAP_READY_SPRITE = MapAtlasesMod.res("shear_map_ready");
    public static final Identifier PLACE_PIN_SPRITE = MapAtlasesMod.res("place_pin");
    public static final Identifier PLACE_PIN_READY_SPRITE = MapAtlasesMod.res("place_pin_ready");
    public static final Identifier ZOOM_IN_BUTTON_SPRITE = MapAtlasesMod.res("zoom_in_button");
    public static final Identifier ZOOM_IN_BUTTON_HOVERED_SPRITE = MapAtlasesMod.res("zoom_in_button_hovered");

    public static final Identifier SLICE_DOWN_SPRITE = MapAtlasesMod.res("slice_down");
    public static final Identifier SLICE_DOWN_HOVERED_SPRITE = MapAtlasesMod.res("slice_down_hovered");
    public static final Identifier SLICE_DOWN_INACTIVE_SPRITE = MapAtlasesMod.res("slice_down_inactive");
    public static final Identifier SLICE_UP_SPRITE = MapAtlasesMod.res("slice_up");
    public static final Identifier SLICE_UP_HOVERED_SPRITE = MapAtlasesMod.res("slice_up_hovered");
    public static final Identifier SLICE_UP_INACTIVE_SPRITE = MapAtlasesMod.res("slice_up_inactive");

    public static final Identifier FILTER_SPRITE = MapAtlasesMod.res("filter");
    public static final Identifier FILTER_HOVERED_SPRITE = MapAtlasesMod.res("filter_hovered");
    public static final Identifier FILTER_ACTIVE_SPRITE = MapAtlasesMod.res("filter_active");

    public static final Identifier MAP_OVERWORLD_SPRITE = MapAtlasesMod.res("map_overworld");
    public static final Identifier MAP_AETHER_SPRITE = MapAtlasesMod.res("map_the_aether");
    public static final Identifier MAP_END_SPRITE = MapAtlasesMod.res("map_end");
    public static final Identifier MAP_NETHER_SPRITE = MapAtlasesMod.res("map_nether");
    public static final Identifier MAP_BUMBLEZONE_SPRITE = MapAtlasesMod.res("map_the_bumblezone");

    public static final Identifier MAP_TYPE_VANILLA_SPRITE = MapAtlasesMod.res("map_type_vanilla");
    public static final Identifier MAP_TYPE_MAGIC_SPRITE = MapAtlasesMod.res("map_type_magic");
    public static final Identifier MAP_TYPE_MAZE_SPRITE = MapAtlasesMod.res("map_type_maze");
    public static final Identifier MAP_TYPE_ORE_SPRITE = MapAtlasesMod.res("map_type_ore");


    public static final Identifier ATLAS_OVERLAY_TEXTURE = MapAtlasesMod.res("textures/gui/screen/atlas_overlay.png");
    public static final Identifier ATLAS_BACKGROUND_TEXTURE = MapAtlasesMod.res("textures/gui/screen/atlas_background.png");
    public static final Identifier ATLAS_BACKGROUND_TEXTURE_BIG = MapAtlasesMod.res("textures/gui/screen/atlas_background_big.png");
    public static final Identifier CROSSHAIR_SPRITE = Identifier.withDefaultNamespace("hud/crosshair");
    public static final Identifier MAP_HUD_BACKGROUND_TEXTURE = MapAtlasesMod.res("textures/gui/hud/map_background.png");
    public static final Identifier MAP_HUD_FOREGROUND_TEXTURE = MapAtlasesMod.res("textures/gui/hud/map_foreground.png");


    public static final List<String> DIMENSION_TEXTURE_ORDER = List.of(Level.OVERWORLD.identifier().toString(),
            Level.NETHER.identifier().toString(), Level.END.identifier().toString(),
            "aether:the_aether", "twilightforest:twilight_forest", "undergarden:undergarden",
            "tropicraft:tropics", "thebetweenlands:betweenlands", "blue_skies:everbright",
            "the_bumblezone:the_bumblezone");

    public static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(MapAtlasesMod.res("minimap"));

    public static final KeyMapping OPEN_ATLAS_KEYBIND = new KeyMapping(
            "key.map_atlases.open_minimap",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            KEY_CATEGORY
    );

    public static final KeyMapping PLACE_PIN_KEYBIND = new KeyMapping(
            "key.map_atlases.place_pin",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            KEY_CATEGORY
    );

    public static final KeyMapping INCREASE_MINIMAP_ZOOM = new KeyMapping(
            "key.map_atlases.zoom_in_minimap",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_ADD,
            KEY_CATEGORY
    );

    public static final KeyMapping DECREASE_MINIMAP_ZOOM = new KeyMapping(
            "key.map_atlases.zoom_out_minimap",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_SUBTRACT,
            KEY_CATEGORY
    );

    public static final KeyMapping INCREASE_SLICE = new KeyMapping(
            "key.map_atlases.increase_slice",
            InputConstants.UNKNOWN.getValue(),
            KEY_CATEGORY
    );

    public static final KeyMapping DECREASE_SLICE = new KeyMapping(
            "key.map_atlases.decrease_slice",
            InputConstants.UNKNOWN.getValue(),
            KEY_CATEGORY
    );

    @Nullable
    private static MapGridKey currentActiveMapKey = null;
    private static AtlasMap currentActiveMap = null;
    private static ItemStack currentActiveAtlas = ItemStack.EMPTY;
    private static boolean isDrawingAtlas = false;


    public static void refreshActiveAtlas(Player player) {

        if (player != Minecraft.getInstance().player) return;
        ItemStack atlas = AtlasLookup.getAtlasFromPlayerByConfig(player);
        currentActiveAtlas = atlas;
        currentActiveMap = null;
        currentActiveMapKey = null;
        if (!atlas.isEmpty()) {
            MapCollection maps = MapAtlasItem.getMaps(atlas, player.level());
            maps.resolvePendingMaps(player.level());
            Slice slice = MapAtlasItem.getSelectedSlice(atlas, player.level().dimension());
            // I hate this
            currentActiveMapKey = MapGridKey.atEntityPosition(maps.getScale(), slice, player);
            AtlasMap select = maps.getMapAt(currentActiveMapKey);
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

    public static MapGridKey getActiveMapKey() {
        return currentActiveMapKey;
    }

    public static AtlasMap getActiveMap() {
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

    public static void openScreen(Optional<BlockPos> lecternPos, boolean pinOnly) {
        @Nullable LecternBlockEntity lectern = null;
        ItemStack atlas = ItemStack.EMPTY;
        Player player = Minecraft.getInstance().player;
        if (lecternPos.isEmpty()) {
            atlas = AtlasLookup.getAtlasFromPlayerByConfig(player);
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
        maps.resolvePendingMaps(level);
        if (!maps.isEmpty()) {
            Minecraft.getInstance().setScreen(new AtlasOverviewScreen(atlas, lectern, pinOnly));
        }
    }

    //hack
    public static ContainerLevelAccess getClientAccess() {
        return ContainerLevelAccess.create(Minecraft.getInstance().level, BlockPos.ZERO);
    }

    //debug stuff
    public static void debugMapUpdated(MapId mapId, MapType mapType) {
        CACHE.put(Pair.of(mapId, mapType), 10);
    }

    public static int debugIsMapUpdated(int tint, MapId stringId, MapType mapType) {
        Integer value = getValue(stringId, mapType);
        if (value != null) {
            int brightness = Mth.clamp((int) (value / 10f * 255), 0, 255);
            return ARGB.multiply(tint, ARGB.color(255, brightness, brightness, brightness));
        }
        return tint;
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

    @PlatformImpl
    public static void decreaseHoodZoom() {
        throw new AssertionError();
    }

    @PlatformImpl
    public static void increaseHoodZoom() {
        throw new AssertionError();
    }

    public static Level getLevel() {
        return Minecraft.getInstance().level;
    }
}