package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.saveddata.maps.MapId;
import pepjebs.mapatlases.client.MapAtlasesClient;

public class S2CDebugUpdateMapPacket implements Message {
    private final MapId mapId;

    public S2CDebugUpdateMapPacket(FriendlyByteBuf buf) {
        this.mapId = buf.readUtf();
    }

    public S2CDebugUpdateMapPacket(MapId map) {
        this.mapId = map;
    }

    @Override
    public void write(RegistryFriendlyByteBuf registryFriendlyByteBuf) {

        buf.writeUtf(mapId);

    }

    @Override
    public void handle(Context context) {
        MapAtlasesClient.debugMapUpdated( mapId);
    }
}
