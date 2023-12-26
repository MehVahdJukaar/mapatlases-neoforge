package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.minecraft.network.FriendlyByteBuf;

@Deprecated
public class S2CSetActiveMapPacket implements Message {

    private final String mapId;

    public S2CSetActiveMapPacket(FriendlyByteBuf buf) {
        this.mapId = buf.readUtf();
    }

    public S2CSetActiveMapPacket(String mapId) {
        this.mapId = mapId;
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeUtf(mapId);
    }

    @Override
    public void handle(ChannelHandler.Context context) {
            // MapAtlasesClient.setActiveMap(mapId.equals("null") ? null : mapId);
    }
}
