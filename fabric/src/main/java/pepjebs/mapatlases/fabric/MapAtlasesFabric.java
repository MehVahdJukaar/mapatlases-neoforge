package pepjebs.mapatlases.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.impl.client.rendering.ColorResolverRegistryImpl;
import net.fabricmc.fabric.mixin.client.rendering.WorldRendererMixin;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.lifecycle.MapAtlasesClientEvents;
import pepjebs.mapatlases.lifecycle.MapAtlasesServerEvents;

public class MapAtlasesFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        MapAtlasesMod.init();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (var p : server.getPlayerList().getPlayers()) {
                MapAtlasesServerEvents.onPlayerTick(p);
            }
        });
        if (PlatHelper.getPhysicalSide().isClient()) {
            MapAtlasesFabricClient.clientInit();

            ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->  MapAtlasesClientEvents.onLoggedOut(handler.getLevel().registryAccess()));
        }

        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> {
            if(joined)MapAtlasesServerEvents.onPlayerJoin(player);
        });
    }


}
