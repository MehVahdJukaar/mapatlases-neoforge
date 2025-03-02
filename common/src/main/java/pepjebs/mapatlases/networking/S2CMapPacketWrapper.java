package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;

/*
public class S2CMapPacketWrapper implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, S2CMapPacketWrapper> TYPE = Message.makeType(
            MapAtlasesMod.res("map_packet_wrapper"),
            S2CMapPacketWrapper::new
    );

    public final ClientboundMapItemDataPacket packet;
    public final ResourceLocation dimension;
    public final int centerX;
    public final int centerZ;

    public S2CMapPacketWrapper(MapItemSavedData data, ClientboundMapItemDataPacket packet) {
        this.packet = packet;
        this.centerX = data.centerX;
        this.centerZ = data.centerZ;
        this.dimension = data.dimension.location();
    }

    public S2CMapPacketWrapper(FriendlyByteBuf buf) {
        this.dimension = buf.readResourceLocation();
        this.centerX = buf.readVarInt();
        this.centerZ = buf.readVarInt();
        this.packet = new ClientboundMapItemDataPacket(buf);

    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeResourceLocation(dimension);
        buf.writeVarInt(centerX);
        buf.writeVarInt(centerZ);
        packet.write(buf);
    }

    @Override
    public void handle(Context context) {
        MapAtlasesClient.handleMapPacketWrapperPacket(this);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE.type();
    }
}
*/