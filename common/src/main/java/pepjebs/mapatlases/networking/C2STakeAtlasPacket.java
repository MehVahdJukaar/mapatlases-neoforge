package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.utils.AtlasLectern;

public class C2STakeAtlasPacket implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, C2STakeAtlasPacket> TYPE = Message.makeType(
            MapAtlasesMod.res("take_atlas"),
            C2STakeAtlasPacket::new
    );

    private final BlockPos pos;

    public C2STakeAtlasPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public C2STakeAtlasPacket(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    @Override
    public void handle(Context context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) return;

        if (player.level().getBlockEntity(pos) instanceof AtlasLectern lectern) {
            if (!player.mayBuild()) {
                return;
            }
            ItemStack itemstack = lectern.mapatlases$removeAtlas();
            if (!player.getInventory().add(itemstack)) {
                player.drop(itemstack, false);
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE.type();
    }
}
