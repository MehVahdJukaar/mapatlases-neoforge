package pebjebs.mapatlases.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import pebjebs.mapatlases.client.MapAtlasesClient;

import java.util.function.Supplier;

public class S2CSetActiveMapPacket {

    private final String mapId;

    public S2CSetActiveMapPacket(FriendlyByteBuf buf) {
        this.mapId = buf.readUtf();
    }

    public S2CSetActiveMapPacket(String mapId) {
        this.mapId = mapId;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(mapId);
    }

    public void apply(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            MapAtlasesClient.setActiveMap(mapId.equals("null") ? null : mapId);
        });
    }

}
