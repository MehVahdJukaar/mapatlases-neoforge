package pepjebs.mapatlases.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.client.fabric.MapAtlasesClientImpl;
import pepjebs.mapatlases.lifecycle.MapAtlasesClientEvents;
import pepjebs.mapatlases.lifecycle.MapAtlasesServerEvents;

public class MapAtlasesFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        MapAtlasesMod.init();


        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for(var p : server.getPlayerList().getPlayers()){
                MapAtlasesServerEvents.onPlayerTick(p);
            }
        });
        if (PlatHelper.getPhysicalSide().isClient()) {
            MapAtlasesClientImpl.init();
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if(client.player != null) MapAtlasesClient.cachePlayerState(client.player);
                if(client.level != null) MapAtlasesClientEvents.onClientTick(client, client.level);
            });
        }

    }
}
