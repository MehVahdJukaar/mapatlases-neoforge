package pepjebs.mapatlases.networking;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import pepjebs.mapatlases.utils.AtlasLectern;

import java.util.function.Supplier;

public class C2STakeAtlasPacket {


    private final BlockPos pos;

    public C2STakeAtlasPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public C2STakeAtlasPacket(BlockPos pos) {
        this.pos = pos;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public void apply(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;
            if(player.level().getBlockEntity(pos) instanceof AtlasLectern lectern){
                if (!player.mayBuild()) {
                    return;
                }
                ItemStack itemstack = lectern.mapatlases$removeAtlas();
                if (!player.getInventory().add(itemstack)) {
                    player.drop(itemstack, false);
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}
