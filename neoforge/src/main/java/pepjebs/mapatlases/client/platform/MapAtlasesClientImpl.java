package pepjebs.mapatlases.client.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.common.NeoForge;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.AtlasInHandRenderer;
import pepjebs.mapatlases.client.ui.MapAtlasesHUD;
import pepjebs.mapatlases.lifecycle.MapAtlasesClientEvents;

public class MapAtlasesClientImpl {

    //minecraft doesnt exist yet when this class loads
    private static MapAtlasesHUD hud;

    public static void init(IEventBus bus) {
        bus.addListener(MapAtlasesClientImpl::registerOverlay);
        NeoForge.EVENT_BUS.register(MapAtlasesClientImpl.class);
    }

    public static void registerOverlay(RegisterGuiLayersEvent event) {
        hud = new MapAtlasesHUD();
        event.registerAboveAll(MapAtlasesMod.res("atlas"), hud::render);
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        ItemStack stack = event.getItemStack();
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !AtlasInHandRenderer.shouldRenderAsMap(stack, player)) return;
        event.setCanceled(true);

        ItemInHandRenderer renderer = mc.gameRenderer.itemInHandRenderer;
        PoseStack poseStack = event.getPoseStack();
        boolean isMainHand = event.getHand() == InteractionHand.MAIN_HAND;
        poseStack.pushPose();
        if (isMainHand && player.getOffhandItem().isEmpty()) {
            renderer.renderTwoHandedMap(poseStack, event.getSubmitNodeCollector(), event.getPackedLight(),
                    event.getInterpolatedPitch(), event.getEquipProgress(), event.getSwingProgress());
        } else {
            HumanoidArm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
            renderer.renderOneHandedMap(poseStack, event.getSubmitNodeCollector(), event.getPackedLight(),
                    event.getEquipProgress(), arm, event.getSwingProgress(), stack);
        }
        poseStack.popPose();
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
        hud.decreaseZoom();
    }

    public static void increaseHoodZoom() {
        hud.increaseZoom();
    }

}
