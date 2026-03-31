package pepjebs.mapatlases.client.fabric;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.lifecycle.MapAtlasesClientEvents;

import java.util.Set;

public class MapAtlasesClientImpl {

    private static final Set<KeyMapping> KEYBINDS = Set.of(
            MapAtlasesClient.PLACE_PIN_KEYBIND,
            MapAtlasesClient.DECREASE_SLICE,
            MapAtlasesClient.INCREASE_SLICE,
            MapAtlasesClient.DECREASE_MINIMAP_ZOOM,
            MapAtlasesClient.INCREASE_MINIMAP_ZOOM,
            MapAtlasesClient.OPEN_ATLAS_KEYBIND
    );

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(MapAtlasesClientImpl::mapAtlasClientTick);
    }

    private static void mapAtlasClientTick(Minecraft minecraft) {
        for (var k : KEYBINDS) {
            if (k.consumeClick()) {
                MapAtlasesClientEvents.onKeyPressed(k);
            }
        }
    }

    public static void decreaseHoodZoom() {
    }

    public static void increaseHoodZoom() {
    }
}
