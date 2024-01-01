package pepjebs.mapatlases.fabric;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.client.fabric.MapAtlasesClientImpl;
import pepjebs.mapatlases.lifecycle.MapAtlasesClientEvents;

public class MapAtlasesFabricClient {

    public static void clientInit() {

        MapAtlasesClientImpl.init();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) MapAtlasesClient.cachePlayerState(client.player);
            if (client.level != null) MapAtlasesClientEvents.onClientTick(client, client.level);
        });
    }
}
