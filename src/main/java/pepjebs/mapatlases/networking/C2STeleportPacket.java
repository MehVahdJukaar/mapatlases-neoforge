package pepjebs.mapatlases.networking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.mixin.MapItemSavedDataAccessor;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

public class C2STeleportPacket {


    private final int x;
    private final int z;
    private final Integer y;
    private ResourceKey<Level> dimension;

    public C2STeleportPacket(FriendlyByteBuf buf) {
        this.x = buf.readVarInt();
        this.z = buf.readVarInt();
        this.y = buf.readOptional(FriendlyByteBuf::readVarInt).orElse(null);
        this.dimension = buf.readResourceKey(Registries.DIMENSION);
    }

    public C2STeleportPacket(int x, int z, @Nullable Integer y, ResourceKey<Level> dimension) {
        this.x = x;
        this.z = z;
        this.y = y;
        this.dimension = dimension;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(x);
        buf.writeVarInt(z);
        buf.writeOptional(Optional.ofNullable(y), FriendlyByteBuf::writeVarInt);
        buf.writeResourceKey(dimension);
    }

    public void apply(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;

            ServerLevel level = player.getServer().getLevel(dimension);

            int y = this.y == null ? (
                   level != player.level() ? level.getMaxBuildHeight() :
                           level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z)
            ) : this.y;


            if (performTeleport(player, level, x, y, z)) {
                player.sendSystemMessage(Component.translatable("commands.teleport.success.location.single",
                        player.getDisplayName(),
                        formatDouble(x),
                        formatDouble(y),
                        formatDouble(z)));
            } else {
                player.sendSystemMessage(Component.translatable("commands.teleport.invalidPosition"));
            }


        });
        context.get().setPacketHandled(true);
    }


    private static boolean performTeleport(ServerPlayer player, ServerLevel pLevel,
                                           double pX, double pY, double pZ

    ) {
        EntityTeleportEvent event = ForgeEventFactory.onEntityTeleportCommand(player, pX, pY, pZ);
        if (event.isCanceled()) return false;
        pX = event.getTargetX();
        pY = event.getTargetY();
        pZ = event.getTargetZ();
        BlockPos blockpos = BlockPos.containing(pX, pY, pZ);
        if (Level.isInSpawnableBounds(blockpos)) {
            if (player.teleportTo(pLevel, pX, pY, pZ, EnumSet.noneOf(RelativeMovement.class),
                    player.getYRot(), player.getXRot())) {

                if (!player.isFallFlying()) {
                    player.setDeltaMovement(player.getDeltaMovement().multiply(1.0D, 0, 1.0D).add(0,-5,0));
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

}
