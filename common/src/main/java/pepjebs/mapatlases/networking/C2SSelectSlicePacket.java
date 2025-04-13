package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.MapType;
import pepjebs.mapatlases.utils.Slice;

import java.util.Optional;

public class C2SSelectSlicePacket implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, C2SSelectSlicePacket> TYPE = Message.makeType(
            MapAtlasesMod.res("select_slice"),
            C2SSelectSlicePacket::new
    );

    @NotNull
    private final Optional<BlockPos> lecternPos;
    private final Slice slice;

    public C2SSelectSlicePacket(Slice slice, @NotNull Optional<BlockPos> lecternPos) {
        this.slice = slice;
        this.lecternPos = lecternPos;
    }

    public C2SSelectSlicePacket(FriendlyByteBuf buf) {
        var dimension = buf.readResourceKey(Registries.DIMENSION);

        MapType type = MapType.values()[buf.readVarInt()];

        Optional<Integer> h = buf.readOptional(FriendlyByteBuf::readVarInt);
        slice = new Slice(type, h, dimension);

        lecternPos = buf.readOptional(object -> object.readBlockPos());
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeResourceKey(slice.dimension());
        buf.writeVarInt(slice.type().ordinal());
        buf.writeOptional(slice.height(), FriendlyByteBuf::writeVarInt);
        buf.writeOptional(lecternPos, (object, object2) -> object.writeBlockPos(object2));
    }

    @Override
    public void handle(Context context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) return;

        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
        if (!atlas.isEmpty()) {
            MapAtlasItem.setSelectedSlice(atlas, slice, player.level());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE.type();
    }
}
