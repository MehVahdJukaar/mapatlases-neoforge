package pepjebs.mapatlases.networking;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.Optional;
import java.util.function.Supplier;

public class C2S2COpenAtlasScreenPacket {

    @Nullable
    private final BlockPos lecternPos;
    private final boolean pinOnly;

    public C2S2COpenAtlasScreenPacket(FriendlyByteBuf buf) {
        lecternPos = buf.readOptional(FriendlyByteBuf::readBlockPos).orElse(null);
        pinOnly = buf.readBoolean();
    }

    public C2S2COpenAtlasScreenPacket() {
        this(null, false);
    }

    public C2S2COpenAtlasScreenPacket(BlockPos lecternPos, boolean pinOnly) {
        this.lecternPos = lecternPos;
        this.pinOnly = pinOnly;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeOptional(Optional.ofNullable(lecternPos), FriendlyByteBuf::writeBlockPos);
        buf.writeBoolean(pinOnly);
    }

    // we need all this craziness as we need to ensure maps are sent before gui is opened
    public void apply(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {

            if (context.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                // open screen
                MapAtlasesClient.openScreen(lecternPos, pinOnly);
            } else {
                // sends all atlas and then send this but to client
                ServerPlayer player = context.get().getSender();
                if (player == null) return;
                ItemStack atlas = ItemStack.EMPTY;
                if (lecternPos != null) {
                    if (player.level().getBlockEntity(lecternPos) instanceof LecternBlockEntity le) {
                        atlas = le.getBook();
                    }
                } else {
                    atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
                }
                if (atlas.getItem() instanceof MapAtlasItem) {
                    MapAtlasItem.syncAndOpenGui(player, atlas, lecternPos, pinOnly);
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}
