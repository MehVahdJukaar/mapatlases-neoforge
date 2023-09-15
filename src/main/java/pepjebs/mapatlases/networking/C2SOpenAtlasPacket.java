package pepjebs.mapatlases.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.function.Supplier;

public class C2SOpenAtlasPacket {


    public C2SOpenAtlasPacket(FriendlyByteBuf buf) {
    }

    public C2SOpenAtlasPacket() {
    }

    public void write(FriendlyByteBuf buf) {
    }

    public void apply(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;
            ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
            if (atlas.getItem() instanceof MapAtlasItem ma) {
                ma.openHandledAtlasScreen(player);
            }
        });
        context.get().setPacketHandled(true);
    }
}
