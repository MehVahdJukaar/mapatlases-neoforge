package pepjebs.mapatlases.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import pepjebs.mapatlases.client.MapAtlasesClient;

import java.util.function.Supplier;

public class S2CSyncMapCenterPacket {

    public final String mapId;
    public final int centerX;
    public final int centerZ;

    public S2CSyncMapCenterPacket(FriendlyByteBuf buf) {
        mapId = buf.readUtf();
        centerX = buf.readVarInt();
        centerZ = buf.readVarInt();
    }

    public S2CSyncMapCenterPacket(String mapId, int x, int z) {
        this.mapId = mapId;
        this.centerX = x;
        this.centerZ = z;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(mapId);
        buf.writeVarInt(centerX);
        buf.writeVarInt(centerZ);
    }

    public void apply(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT)
                MapAtlasesClient.setMapCenter(this);
        });
        context.get().setPacketHandled(true);
    }

}
