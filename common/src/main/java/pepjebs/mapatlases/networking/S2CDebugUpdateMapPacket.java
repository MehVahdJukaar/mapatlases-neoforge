package pepjebs.mapatlases.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import pepjebs.mapatlases.client.MapAtlasesClient;

import java.util.function.Supplier;

public class S2CDebugUpdateMapPacket {
    private final String mapId;

    public S2CDebugUpdateMapPacket(FriendlyByteBuf buf) {
        this.mapId = buf.readUtf();
    }

    public S2CDebugUpdateMapPacket(String map) {
        this.mapId = map;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(mapId);
    }

    public void apply(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            MapAtlasesClient.debugMapUpdated( mapId);
        });
        context.get().setPacketHandled(true);
    }
}
