package pepjebs.mapatlases.client.fabric;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.client.ui.MapAtlasesHUD;
import pepjebs.mapatlases.lifecycle.MapAtlasesClientEvents;

import java.util.Set;

public class MapAtlasesClientImpl {

    private static final Set<KeyMapping> KEYBINDS = Set.of(
            MapAtlasesClient.DECREASE_SLICE, MapAtlasesClient.INCREASE_SLICE, MapAtlasesClient.DECREASE_MINIMAP_ZOOM,
            MapAtlasesClient.INCREASE_MINIMAP_ZOOM, MapAtlasesClient.OPEN_ATLAS_KEYBIND);

    private static final MapAtlasesHUD HUD = new MapAtlasesHUD();

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(MapAtlasesClientImpl::mapAtlasClientTick);

        HudRenderCallback.EVENT.register(MapAtlasesClientImpl::onRenderHud);
    }

    private static void onRenderHud(PoseStack graphics, float partialTicks) {
        Window window = Minecraft.getInstance().getWindow();
        HUD.render(graphics, partialTicks, window.getScreenWidth(), window.getScreenHeight());
    }

    private static void mapAtlasClientTick(Minecraft minecraft) {
        for (var k : KEYBINDS) {
            if (k.consumeClick()) {
                MapAtlasesClientEvents.onKeyPressed(k.key.getValue(), k.key.getValue());
            }
        }
    }

    public static void decreaseHoodZoom() {
        HUD.decreaseZoom();
    }

    public static void increaseHoodZoom() {
        HUD.increaseZoom();
    }
}
