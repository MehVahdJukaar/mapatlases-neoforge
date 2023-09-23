package pepjebs.mapatlases.networking;

import net.minecraft.client.gui.screens.inventory.LecternScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraftforge.network.NetworkEvent;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.mixin.LecternBlockEntityMixin;
import pepjebs.mapatlases.utils.AtlasLectern;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.function.Supplier;

public class TakeAtlasPacket {


    private final BlockPos pos;

    public TakeAtlasPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public TakeAtlasPacket(BlockPos pos) {
        this.pos = pos;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public void apply(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;
            if(player.level.getBlockEntity(pos) instanceof AtlasLectern lectern){
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
