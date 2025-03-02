package pepjebs.mapatlases.client.neoforge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import pepjebs.mapatlases.client.ui.MapAtlasesHUD;
import pepjebs.mapatlases.lifecycle.MapAtlasesClientEvents;

public class MapAtlasesClientImpl {

    private static final MapAtlasesHUDImpl HUD = new MapAtlasesHUDImpl();

    public static void init(IEventBus bus) {
        bus.addListener(MapAtlasesClientImpl::registerOverlay);
        NeoForge.EVENT_BUS.register(MapAtlasesClientImpl.class);
    }

    public static void registerOverlay(RegisterGuiOverlaysEvent event) {
        event.registerBelow(VanillaGuiOverlay.DEBUG_TEXT.id(), "atlas", HUD);
    }

    private static class MapAtlasesHUDImpl extends MapAtlasesHUD implements IGuiOverlay {

        @Override
        public void render(ForgeGui forgeGui, GuiGraphics graphics, float f, int i, int j) {
            super.render(graphics, f, i, j);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null || event.phase != TickEvent.Phase.END) return;
        MapAtlasesClientEvents.onClientTick(client, level);
    }

    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        MapAtlasesClientEvents.onLoggedOut(event.getPlayer().registryAccess());
    }

    public static void decreaseHoodZoom() {
        HUD.decreaseZoom();
    }

    public static void increaseHoodZoom() {
        HUD.increaseZoom();
    }

}
