package pepjebs.mapatlases.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.network.NetworkEvent;
import pepjebs.mapatlases.client.MapAtlasesClient;

import java.util.function.Supplier;

public class S2CMapPacketWrapper {
    public final ClientboundMapItemDataPacket packet;
    public final ResourceLocation dimension;
    public final int centerX;
    public final int centerZ;

    public S2CMapPacketWrapper(MapItemSavedData data, ClientboundMapItemDataPacket packet) {
        this.packet = packet;
        this.centerX = data.x;
        this.centerZ = data.z;
        this.dimension = data.dimension.location();
    }

    public S2CMapPacketWrapper(FriendlyByteBuf buf) {
        this.dimension = buf.readResourceLocation();
        this.centerX = buf.readVarInt();
        this.centerZ = buf.readVarInt();
        this.packet = new ClientboundMapItemDataPacket(buf);

    }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(dimension);
        buf.writeVarInt(centerX);
        buf.writeVarInt(centerZ);
        packet.write(buf);
    }

    public void apply(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            MapAtlasesClient.handleMapPacketWrapperPacket(this);
        });
        context.get().setPacketHandled(true);
    }
}
