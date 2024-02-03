package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import pepjebs.mapatlases.utils.AtlasLectern;

public class C2STakeAtlasPacket implements Message {


    private final BlockPos pos;

    public C2STakeAtlasPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public C2STakeAtlasPacket(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    @Override
    public void handle(ChannelHandler.Context context) {
        if (!(context.getSender() instanceof ServerPlayer player)) return;

        if(player.level.getBlockEntity(pos) instanceof AtlasLectern lectern){
            if (!player.mayBuild()) {
                return;
            }
            ItemStack itemstack = lectern.mapatlases$removeAtlas();
            if (!player.getInventory().add(itemstack)) {
                player.drop(itemstack, false);
            }
        }
    }
}
