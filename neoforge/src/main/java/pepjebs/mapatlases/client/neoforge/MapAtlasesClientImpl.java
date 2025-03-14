package pepjebs.mapatlases.client.neoforge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.ui.MapAtlasesHUD;
import pepjebs.mapatlases.lifecycle.MapAtlasesClientEvents;
import twilightforest.network.MagicMapPacket;

public class MapAtlasesClientImpl {

    private static final MapAtlasesHUDImpl HUD = new MapAtlasesHUDImpl();

    public static void init(IEventBus bus) {
        bus.addListener(MapAtlasesClientImpl::registerOverlay);
        NeoForge.EVENT_BUS.register(MapAtlasesClientImpl.class);
    }

    public static void registerOverlay(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.DEBUG_OVERLAY, MapAtlasesMod.res("atlas"), HUD);
    }

    private static class MapAtlasesHUDImpl extends MapAtlasesHUD implements LayeredDraw.Layer {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null) return;
        MapAtlasesClientEvents.onClientTick(client, level);
    }

    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        if (event.getPlayer() != null) {
            MapAtlasesClientEvents.onLoggedOut(event.getPlayer().registryAccess());
        }
    }

    public static void decreaseHoodZoom() {
        HUD.decreaseZoom();
    }

    public static void increaseHoodZoom() {
        HUD.increaseZoom();
    }

}
