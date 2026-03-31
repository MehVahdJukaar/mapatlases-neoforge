package pepjebs.mapatlases.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import pepjebs.mapatlases.MapAtlasesMod;
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

        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> {
            if(joined)MapAtlasesServerEvents.onPlayerJoin(player);
        });
    }


}
