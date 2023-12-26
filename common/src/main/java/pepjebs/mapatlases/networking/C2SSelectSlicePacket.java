package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.MapType;
import pepjebs.mapatlases.utils.Slice;

public class C2SSelectSlicePacket implements Message {


    @Nullable
    private final BlockPos lecternPos;
    private final Slice slice;

    public C2SSelectSlicePacket(Slice slice, @Nullable BlockPos lecternPos) {
        this.slice = slice;
        this.lecternPos = lecternPos;
    }

    public C2SSelectSlicePacket(FriendlyByteBuf buf) {
        var dimension = buf.readResourceKey(Registries.DIMENSION);

        MapType type = MapType.values()[buf.readVarInt()];

        Integer h;
        if (buf.readBoolean()) {
            h = null;
        } else h = buf.readVarInt();
        slice = Slice.of(type, h, dimension);

        if (buf.readBoolean()) {
            lecternPos = null;
        } else {
            lecternPos = buf.readBlockPos();
        }
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeResourceKey(slice.dimension());

        buf.writeVarInt(slice.type().ordinal());

        Integer h = slice.height();
        buf.writeBoolean(h == null);
        if (h != null) {
            buf.writeVarInt(h);
        }

        buf.writeBoolean(lecternPos == null);
        if (lecternPos != null) {
            buf.writeBlockPos(lecternPos);
        }
    }

    @Override
    public void handle(ChannelHandler.Context context) {
        if (!(context.getSender() instanceof ServerPlayer player)) return;

        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
        if (!atlas.isEmpty()) {
            MapAtlasItem.setSelectedSlice(atlas, slice);
        }
    }
}
