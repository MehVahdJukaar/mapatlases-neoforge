package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.Slice;

import java.util.Optional;

public record C2SSelectSlicePacket(Slice slice, Optional<BlockPos> lecternPos) implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, C2SSelectSlicePacket> TYPE = Message.makeType(
            MapAtlasesMod.res("select_slice"),
            C2SSelectSlicePacket::new
    );

    public C2SSelectSlicePacket(RegistryFriendlyByteBuf buf) {
        this(Slice.STREAM_CODEC.decode(buf), buf.readOptional(BlockPos.STREAM_CODEC));
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        Slice.STREAM_CODEC.encode(buf, slice);
        buf.writeOptional(lecternPos, BlockPos.STREAM_CODEC);
    }

    @Override
    public void handle(Context context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) return;

        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromLecternOrPlayer(player, lecternPos);
        if (atlas.isEmpty()) return;
        MapAtlasItem.setSelectedSlice(atlas, slice, player.level());
        MapAtlasesAccessUtils.syncLecternAtlas(player, lecternPos);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE.type();
    }
}
