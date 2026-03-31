package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.resources.Identifier;
import pepjebs.mapatlases.client.MapAtlasesClient;


public class S2CMapPacketWrapper implements Message {
    public final ClientboundMapItemDataPacket packet;
    public final int mapId;
    public final Identifier dimension;
    public final int centerX;
    public final int centerZ;

    public S2CMapPacketWrapper(net.minecraft.world.level.saveddata.maps.MapItemSavedData data, ClientboundMapItemDataPacket packet) {
        this.packet = packet;
        this.mapId = packet.mapId().id();
        this.centerX = data.centerX;
        this.centerZ = data.centerZ;
        this.dimension = data.dimension.identifier();
    }

    public S2CMapPacketWrapper(FriendlyByteBuf buf) {
        this.mapId = buf.readVarInt();
        this.dimension = buf.readIdentifier();
        this.centerX = buf.readVarInt();
        this.centerZ = buf.readVarInt();
        this.packet = ClientboundMapItemDataPacket.STREAM_CODEC.decode(asRegistryFriendly(buf));
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeVarInt(mapId);
        buf.writeIdentifier(dimension);
        buf.writeVarInt(centerX);
        buf.writeVarInt(centerZ);
        ClientboundMapItemDataPacket.STREAM_CODEC.encode(asRegistryFriendly(buf), packet);
    }

    @Override
    public void handle(ChannelHandler.Context context) {
        MapAtlasesClient.handleMapPacketWrapperPacket(this);
    }

    private static RegistryFriendlyByteBuf asRegistryFriendly(FriendlyByteBuf buf) {
        if (buf instanceof RegistryFriendlyByteBuf registryFriendlyByteBuf) {
            return registryFriendlyByteBuf;
        }
        throw new IllegalStateException("Expected RegistryFriendlyByteBuf for map packet wrapper serialization");
    }
}
