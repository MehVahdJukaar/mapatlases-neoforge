package pepjebs.mapatlases.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import pepjebs.mapatlases.integration.moonlight.ClientMarkers;

import java.util.function.Supplier;

public class S2CWorldHashPacket {
    public final long seed;
    private final String name;

    public S2CWorldHashPacket(ServerPlayer player) {
        Level level = player.level();
        String name = level.getServer().getWorldData().getLevelName();
        long seed = level.getServer().overworld().getSeed();
        this.seed = seed;
        this.name = name;
    }

    public S2CWorldHashPacket(FriendlyByteBuf buf) {
        this.seed = buf.readVarLong();
        this.name = buf.readUtf();

    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarLong(seed);
        buf.writeUtf(name);
    }

    public void apply(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ClientMarkers.loadClientMarkers(this.seed, this.name);

        });
        context.get().setPacketHandled(true);
    }
}
