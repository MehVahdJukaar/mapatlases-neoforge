package pepjebs.mapatlases.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.network.NetworkEvent;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.integration.ClientMarker;

import java.util.Objects;
import java.util.function.Supplier;

public class S2CWorldHashPacket {
    public final int hash;

    public S2CWorldHashPacket(ServerPlayer player) {
        Level level = player.level();
        String name = level.getServer().getWorldData().getLevelName();
        long seed = level.getServer().overworld().getSeed();
        int hash = Objects.hash(name, seed);
        this.hash = hash;
    }

    public S2CWorldHashPacket(FriendlyByteBuf buf) {
        this.hash = buf.readVarInt();

    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(hash);

    }

    public void apply(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ClientMarker.loadClientMarkers(this.hash);
        });
        context.get().setPacketHandled(true);
    }
}
