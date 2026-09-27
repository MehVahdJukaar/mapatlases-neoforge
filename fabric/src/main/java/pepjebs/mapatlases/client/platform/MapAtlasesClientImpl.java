package pepjebs.mapatlases.client.platform;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.client.ui.MapAtlasesHUD;
import pepjebs.mapatlases.lifecycle.MapAtlasesClientEvents;

import java.util.Set;

public class MapAtlasesClientImpl {

    private static final Set<KeyMapping> KEYBINDS = Set.of(MapAtlasesClient.PLACE_PIN_KEYBIND,
            MapAtlasesClient.DECREASE_SLICE, MapAtlasesClient.INCREASE_SLICE, MapAtlasesClient.DECREASE_MINIMAP_ZOOM,
            MapAtlasesClient.INCREASE_MINIMAP_ZOOM, MapAtlasesClient.OPEN_ATLAS_KEYBIND);

    private static final MapAtlasesHUD HUD = new MapAtlasesHUD();

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(MapAtlasesClientImpl::mapAtlasClientTick);
        HudElementRegistry.addLast(MapAtlasesMod.res("atlas"), HUD::render);
    }

    private static void mapAtlasClientTick(Minecraft minecraft) {
        for (var k : KEYBINDS) {
            if (k.consumeClick()) {
                MapAtlasesClientEvents.onKeyPressed(new KeyEvent(k.key.getValue(), k.key.getValue(), 0));
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
