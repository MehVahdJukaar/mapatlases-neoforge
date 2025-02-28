package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.PlatStuff;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;

public class C2STeleportPacket implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, C2STeleportPacket> TYPE = Message.makeType(
            MapAtlasesMod.res("teleport"),
            C2STeleportPacket::new
    );

    private final int x;
    private final int z;
    private final Optional<Integer> y;
    private final ResourceKey<Level> dimension;

    public C2STeleportPacket(FriendlyByteBuf buf) {
        this.x = buf.readVarInt();
        this.z = buf.readVarInt();
        this.y = buf.readOptional(FriendlyByteBuf::readVarInt);
        this.dimension = buf.readResourceKey(Registries.DIMENSION);
    }

    public C2STeleportPacket(int x, int z, Optional<Integer> y, ResourceKey<Level> dimension) {
        this.x = x;
        this.z = z;
        this.y = y;
        this.dimension = dimension;
    }

    private static boolean performTeleport(ServerPlayer player, ServerLevel pLevel,
                                           double pX, double pY, double pZ

    ) {
        var result = PlatStuff.fireTeleportEvent(player, pX, pY, pZ);
        if (result.getFirst()) return false;
        pX = result.getSecond().x;
        pY = result.getSecond().y;
        pZ = result.getSecond().z;
        BlockPos blockpos = BlockPos.containing(pX, pY, pZ);
        if (Level.isInSpawnableBounds(blockpos)) {
            if (player.teleportTo(pLevel, pX, pY, pZ, EnumSet.noneOf(RelativeMovement.class),
                    player.getYRot(), player.getXRot())) {

                if (!player.isFallFlying()) {
                    player.setDeltaMovement(player.getDeltaMovement().multiply(1.0D, 0, 1.0D).add(0, -5, 0));
                    player.setOnGround(true);
                }
                return true;
            }
        }
        return false;
    }

    private static String formatDouble(double pValue) {
        return String.format(Locale.ROOT, "%f", pValue);
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(x);
        buf.writeVarInt(z);
        buf.writeOptional(y, FriendlyByteBuf::writeVarInt);
        buf.writeResourceKey(dimension);
    }

    @Override
    public void handle(Context context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) return;

        ServerLevel level = player.getServer().getLevel(dimension);

        int y;
        if (this.y.isEmpty()) {
            ChunkAccess chunk = level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z), ChunkStatus.FULL, false);
            if (chunk == null || (chunk instanceof LevelChunk lc && lc.isEmpty())) {
                y = level.getMaxBuildHeight();
                MinecraftServer server = level.getServer();
                server.tell(new TickTask(server.getTickCount(), () -> {
                    performTeleport(player, level, x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
                }));
            } else {
                y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);

            }
        } else {
            y = this.y.get();
        }


        if (performTeleport(player, level, x, y, z)) {
            player.sendSystemMessage(Component.translatable("commands.teleport.success.location.single",
                    player.getDisplayName(),
                    formatDouble(x),
                    formatDouble(y),
                    formatDouble(z)));
        } else {
            player.sendSystemMessage(Component.translatable("commands.teleport.invalidPosition"));
        }

    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE.type();
    }
}
