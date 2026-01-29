package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.misc.MapAtlasesAccessUtils;
import pepjebs.mapatlases.misc.Slice;

public record C2SRemoveSlicePacket(Slice slice) implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, C2SRemoveSlicePacket> TYPE = Message.makeType(
            MapAtlasesMod.res("remove_slice"),
            C2SRemoveSlicePacket::new
    );

    public C2SRemoveSlicePacket(RegistryFriendlyByteBuf buf) {
        this(Slice.STREAM_CODEC.decode(buf));
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        Slice.STREAM_CODEC.encode(buf, this.slice);
    }

    @Override
    public void handle(Context context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) return;

        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
        if (!atlas.isEmpty()) {
            MapAtlasItem.removeAndDropSliceMaps(slice, atlas, player);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE.type();
    }
}
