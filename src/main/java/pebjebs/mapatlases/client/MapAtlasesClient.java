package pebjebs.mapatlases.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
import pebjebs.mapatlases.MapAtlasesMod;
import pebjebs.mapatlases.client.ui.MapAtlasesHUD;
import pebjebs.mapatlases.lifecycle.MapAtlasClientEvents;
import pebjebs.mapatlases.screen.MapAtlasesAtlasOverviewScreen;

public class MapAtlasesClient {

    private static final ThreadLocal<Float> worldMapZoomLevel = new ThreadLocal<>();

    public static String currentMapItemSavedDataId = null;

    public static final Material OVERWORLD_TEXTURE =
            new Material(InventoryMenu.BLOCK_ATLAS,  MapAtlasesMod.res("entity/lectern_atlas"));
    public static final Material NETHER_TEXTURE =
            new Material(InventoryMenu.BLOCK_ATLAS,MapAtlasesMod.res("entity/lectern_atlas_nether"));
    public static final Material END_TEXTURE =
            new Material(InventoryMenu.BLOCK_ATLAS,MapAtlasesMod.res("entity/lectern_atlas_end"));
    public static final Material OTHER_TEXTURE =
            new Material(InventoryMenu.BLOCK_ATLAS,  MapAtlasesMod.res("entity/lectern_atlas_unknown"));



    public static final KeyMapping OPEN_ATLAS_KEYBIND = new KeyMapping(
            "key.map_atlases.open_minimap",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "category.map_atlases.minimap"
    );

    public static void init() {

        /*
        // Register client events
        ClientPlayNetworking.registerGlobalReceiver(MapAtlasesInitAtlasS2CPacket.MAP_ATLAS_INIT,
                MapAtlasClientEvents::mapAtlasClientInit);
        ClientPlayNetworking.registerGlobalReceiver(MapAtlasesInitAtlasS2CPacket.MAP_ATLAS_SYNC,
                MapAtlasClientEvents::mapAtlasClientSync);
        ClientPlayNetworking.registerGlobalReceiver(MapAtlasesServerLifecycleEvents.MAP_ATLAS_ACTIVE_STATE_CHANGE, (
                Minecraft _client,
                ClientPlayNetworkHandler _handler,
                PacketByteBuf buf,
                PacketSender _sender) -> {
            String str = buf.readString();
            if (str.compareTo("null") == 0)
                currentMapItemSavedDataId = null;
            else
                currentMapItemSavedDataId = str;
        });*/




        FMLJavaModLoadingContext.get().getModEventBus().register(MapAtlasesClient.class);

        MinecraftForge.EVENT_BUS.register(MapAtlasClientEvents.class);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event){
        // Register ModelPredicate
        ItemProperties.register( MapAtlasesMod.MAP_ATLAS.get(),MapAtlasesMod.res("atlas"),
                MapAtlasesClient::getPredicateForAtlas);

        MenuScreens.register(MapAtlasesMod.ATLAS_OVERVIEW_HANDLER.get(), MapAtlasesAtlasOverviewScreen::new);

    }

    @SubscribeEvent
    public static void clientSetup(RegisterGuiOverlaysEvent event){
        var hud = new MapAtlasesHUD();
        event.registerBelow(VanillaGuiOverlay.DEBUG_TEXT.id(), "atlas", hud);
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

    public  static float getPredicateForAtlas(
            ItemStack stack,
            ClientLevel world,
            LivingEntity entity,
            int seed) {
        // Using ClientLevel will render default Atlas in inventories
        if (world == null && entity != null)
            world = (ClientLevel) entity.level();
        if (world == null) return 0.0f;

        ResourceKey<Level> dimension = world.dimension();
        if (dimension == Level.OVERWORLD) return 0.1f;
        if (dimension == Level.NETHER) return 0.2f;
        if (dimension == Level.END) return 0.3f;
        return 0.0f;
    }
}
