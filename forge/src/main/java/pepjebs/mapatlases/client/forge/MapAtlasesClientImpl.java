package pepjebs.mapatlases.client.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import pepjebs.mapatlases.client.ui.MapAtlasesHUD;
import pepjebs.mapatlases.lifecycle.MapAtlasesClientEvents;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class MapAtlasesClientImpl {

    private static final MapAtlasesHUDImpl HUD = new MapAtlasesHUDImpl();

    public static void init(){
        FMLJavaModLoadingContext.get().getModEventBus().addListener(MapAtlasesClientImpl::registerOverlay);
        MinecraftForge.EVENT_BUS.register(MapAtlasesClientImpl.class);
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
        MapAtlasesClientEvents.onLoggedOut();
    }

    public static void decreaseHoodZoom() {
        HUD.decreaseZoom();
    }

    public static void increaseHoodZoom() {
        HUD.increaseZoom();
    }

}
