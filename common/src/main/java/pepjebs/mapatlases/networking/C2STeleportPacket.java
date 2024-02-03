package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.PlatStuff;

import java.util.Locale;
import java.util.Optional;

public class C2STeleportPacket implements Message {


    private final int x;
    private final int z;
    private final Integer y;
    private final ResourceKey<Level> dimension;

    public C2STeleportPacket(FriendlyByteBuf buf) {
        this.x = buf.readVarInt();
        this.z = buf.readVarInt();
        this.y = buf.readOptional(FriendlyByteBuf::readVarInt).orElse(null);
        this.dimension = buf.readResourceKey(Registry.DIMENSION_REGISTRY);
    }

    public C2STeleportPacket(int x, int z, @Nullable Integer y, ResourceKey<Level> dimension) {
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
        BlockPos blockpos = new BlockPos(pX, pY, pZ);
        if (Level.isInSpawnableBounds(blockpos)) {
            player.teleportTo(pLevel, pX, pY, pZ, player.getYRot(), player.getXRot());

            if (!player.isFallFlying()) {
                player.setDeltaMovement(player.getDeltaMovement().multiply(1.0D, 0, 1.0D).add(0, -5, 0));
                player.setOnGround(true);
            }
            return true;
        }
        return false;
    }

    private static String formatDouble(double pValue) {
        return String.format(Locale.ROOT, "%f", pValue);
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeVarInt(x);
        buf.writeVarInt(z);
        buf.writeOptional(Optional.ofNullable(y), FriendlyByteBuf::writeVarInt);
        buf.writeResourceKey(dimension);
    }

    @Override
    public void handle(ChannelHandler.Context context) {
        if (!(context.getSender() instanceof ServerPlayer player)) return;

        ServerLevel level = player.getServer().getLevel(dimension);

        int y;
        if (this.y == null) {
            var chunk = level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z), ChunkStatus.FULL, false);
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
            y = this.y;
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
}
