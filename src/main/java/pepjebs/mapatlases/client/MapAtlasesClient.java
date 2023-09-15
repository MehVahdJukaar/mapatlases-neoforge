package pepjebs.mapatlases.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.ui.MapAtlasesHUD;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.lifecycle.MapAtlasesClientEvents;

import java.util.List;

public class MapAtlasesClient {

    private static final ThreadLocal<Float> worldMapZoomLevel = new ThreadLocal<>();

    private static String currentMapItemSavedDataId = null;

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
            "tropicraft:tropics", "thebetweenlands:betweenlands", "blue_skies:blue_skies",
            "the_bumblezone:the_bumblezone");

    public static final KeyMapping OPEN_ATLAS_KEYBIND = new KeyMapping(
            "key.map_atlases.open_minimap",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "category.map_atlases.minimap"
    );

    public static String getActiveMap() {
        return currentMapItemSavedDataId;
    }

    public static void setActiveMap(String mapId) {
        MapAtlasesClient.currentMapItemSavedDataId = mapId;
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

        //MenuScreens.register(MapAtlasesMod.ATLAS_OVERVIEW_HANDLER.get(), MapAtlasesAtlasOverviewScreen::new);

    }

    @SubscribeEvent
    public static void clientSetup(RegisterGuiOverlaysEvent event) {
        event.registerBelow(VanillaGuiOverlay.DEBUG_TEXT.id(), "atlas", new MapAtlasesHUD());
    }

    @SubscribeEvent
    public static void registerKeyBinding(RegisterKeyMappingsEvent event) {
        event.register(OPEN_ATLAS_KEYBIND);
    }

    public static float getWorldMapZoomLevel() {
        if (worldMapZoomLevel.get() == null) return 1.0f;
        return worldMapZoomLevel.get();
    }

    public static void setWorldMapZoomLevel(float i) {
        worldMapZoomLevel.set(i);
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
}
