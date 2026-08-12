package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.moonlight.ClientMarkers;

public class S2CWorldHashPacket implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, S2CWorldHashPacket> TYPE = Message.makeType(
            MapAtlasesMod.res("world_hash"),
            S2CWorldHashPacket::new
    );

    public final long seed;
    private final String worldId;

    public S2CWorldHashPacket(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        this.seed = server.overworld().getSeed();
        this.worldId = server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString();
    }

    public S2CWorldHashPacket(FriendlyByteBuf buf) {
        this.seed = buf.readVarLong();
        this.worldId = buf.readUtf();
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarLong(seed);
        buf.writeUtf(worldId);
    }

    @Override
    public void handle(Context context) {
        ClientMarkers.loadClientMarkers(this.seed, this.worldId, context.getPlayer().registryAccess());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE.type();
    }
}
