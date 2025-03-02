package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.moonlight.ClientMarkers;

public class S2CWorldHashPacket implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, S2CWorldHashPacket> TYPE = Message.makeType(
            MapAtlasesMod.res("world_hash"),
            S2CWorldHashPacket::new
    );

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
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarLong(seed);
        buf.writeUtf(name);
    }

    @Override
    public void handle(Context context) {
        ClientMarkers.loadClientMarkers(this.seed, this.name, context.getPlayer().registryAccess());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE.type();
    }
}
