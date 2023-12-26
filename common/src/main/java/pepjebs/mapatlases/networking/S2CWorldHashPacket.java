package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import pepjebs.mapatlases.integration.moonlight.ClientMarkers;

public class S2CWorldHashPacket implements Message {
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

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeVarLong(seed);
        buf.writeUtf(name);
    }

    @Override
    public void handle(ChannelHandler.Context context) {
        ClientMarkers.loadClientMarkers(this.seed, this.name);

    }
}
