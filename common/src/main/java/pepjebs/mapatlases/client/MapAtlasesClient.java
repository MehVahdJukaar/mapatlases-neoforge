package pepjebs.mapatlases.client;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.capabilities.MapCollectionCap;
import pepjebs.mapatlases.capabilities.MapKey;
import pepjebs.mapatlases.client.screen.AtlasOverviewScreen;
import pepjebs.mapatlases.client.ui.MapAtlasesHUD;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.lifecycle.MapAtlasesClientEvents;
import pepjebs.mapatlases.mixin.MapItemSavedDataAccessor;
import pepjebs.mapatlases.networking.S2CMapPacketWrapper;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.Slice;

import java.util.List;
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
    private static final ThreadLocal<Float> globalDecorationRotation = ThreadLocal.withInitial(() -> 0f);

    @Nullable
    private static MapKey currentActiveMapKey = null;
    private static ItemStack currentActiveAtlas = ItemStack.EMPTY;
    private static boolean isDrawingAtlas = false;

    public static MapAtlasesHUD HUD;

    public static void cachePlayerState(Player player) {
        if (player != Minecraft.getInstance().player) return;
        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
        currentActiveAtlas = atlas;
        if (!atlas.isEmpty()) {
            var maps = MapAtlasItem.getMaps(atlas, player.level());
            maps.fixDuplicates(player.level());
            Slice slice = MapAtlasItem.getSelectedSlice(atlas, player.level().dimension());
            // I hate this
            currentActiveMapKey = MapKey.at(maps.getScale(), player, slice);
            MapDataHolder select = maps.select(currentActiveMapKey);
            if (select == null) {
                MapDataHolder closest = maps.getClosest(player, slice);
                if (closest != null) {
                    currentActiveMapKey = closest.makeKey();
                }
            }
        } else currentActiveMapKey = null;
    }

    public static ItemStack getCurrentActiveAtlas() {
        return currentActiveAtlas;
    }

    public static MapKey getActiveMapKey() {
        return currentActiveMapKey;
    }

    public static void setIsDrawingAtlas(boolean state){
        isDrawingAtlas = state;
    }

    public static boolean isDrawingAtlas() {
        return isDrawingAtlas;
    }

    public static void init() {
        FMLJavaModLoadingContext.get().getModEventBus().register(MapAtlasesClient.class);
        MinecraftForge.EVENT_BUS.register(MapAtlasesClientEvents.class);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        // Register ModelPredicate
        ItemProperties.register(MapAtlasesMod.MAP_ATLAS.get(), MapAtlasesMod.res("atlas"),
                MapAtlasesClient::getPredicateForAtlas);
    }

    @SubscribeEvent
    public static void registerOverlay(RegisterGuiOverlaysEvent event) {
        HUD = new MapAtlasesHUD();
        event.registerBelow(VanillaGuiOverlay.DEBUG_TEXT.id(), "atlas", HUD);
    }

    @SubscribeEvent
    public static void registerKeyBinding(RegisterKeyMappingsEvent event) {
        event.register(OPEN_ATLAS_KEYBIND);
        event.register(DECREASE_MINIMAP_ZOOM);
        event.register(INCREASE_MINIMAP_ZOOM);
        event.register(PLACE_PIN_KEYBIND);
        if (MapAtlasesMod.TWILIGHTFOREST || MapAtlasesMod.SUPPLEMENTARIES) {
            event.register(INCREASE_SLICE);
            event.register(DECREASE_SLICE);
        }
    }


    @Deprecated(forRemoval = true)
    public static float getWorldMapZoomLevel() {
        return globalDecorationScale.get();
    }

    public static void setDecorationsScale(float i) {
        globalDecorationScale.set(i);
    }

    public static void setDecorationRotation(float i) {
        globalDecorationRotation.set(i);
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

    public static void handleMapPacketWrapperPacket(S2CMapPacketWrapper packet) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        Minecraft.getInstance().player.connection.handleMapItemData(packet.packet);

        var data = level.getMapData(MapItem.makeKey(packet.packet.getMapId()));
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


    public static void openScreen(@Nullable BlockPos lecternPos, boolean pinOnly) {
        @Nullable LecternBlockEntity lectern = null;
        ItemStack atlas = ItemStack.EMPTY;
        Player player = Minecraft.getInstance().player;
        if (lecternPos == null) {
            atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
        } else {
            if (player.level().getBlockEntity(lecternPos) instanceof LecternBlockEntity lec) {
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
        MapCollectionCap maps = MapAtlasItem.getMaps(atlas, level);
        //we arent ticking these so we have to fix duplicates
        maps.fixDuplicates(level);
        if (!maps.isEmpty()) {
            Minecraft.getInstance().setScreen(new AtlasOverviewScreen(atlas, lectern, pinOnly));
        }
    }

    //hack
    public static ContainerLevelAccess getClientAccess() {
        return ContainerLevelAccess.create(Minecraft.getInstance().level, BlockPos.ZERO);
    }

    public static void modifyTextDecorationTransform(PoseStack poseStack, float textWidth, float textScale) {
        Float scale = globalDecorationScale.get();
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
        if (scale != null) poseStack.scale(scale, scale, 1);
    }


    //debug stuff
    public static void debugMapUpdated(String mapId) {
        CACHE.put(mapId, 10);
    }

    public static int debugIsMapUpdated(int light, String stringId) {
        Integer value = getValue(stringId);
        if (value != null) {
            int pBlockLight = Mth.clamp((int) (value / (float) 10 * 15f), 0, 15);
            return LightTexture.pack(pBlockLight, pBlockLight);
        }
        return light;
    }

    private static Integer getValue(String key) {
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

    private static final Cache<String, Integer> CACHE = CacheBuilder.newBuilder()
            .maximumSize(100) // Set your desired maximum size
            .expireAfterAccess(10, TimeUnit.SECONDS) // Set your desired time for validity
            .build();

}