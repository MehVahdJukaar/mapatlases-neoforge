package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.minecraft.network.FriendlyByteBuf;
import pepjebs.mapatlases.client.MapAtlasesClient;

public class S2CDebugUpdateMapPacket implements Message {
    private final String mapId;

    public S2CDebugUpdateMapPacket(FriendlyByteBuf buf) {
        this.mapId = buf.readUtf();
    }

    public S2CDebugUpdateMapPacket(String map) {
        this.mapId = map;
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeUtf(mapId);

    }

    @Override
    public void handle(ChannelHandler.Context context) {
        MapAtlasesClient.debugMapUpdated( mapId);
    }
}
