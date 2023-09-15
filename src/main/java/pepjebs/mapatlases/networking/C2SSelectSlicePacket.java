package pepjebs.mapatlases.networking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.function.Supplier;

public class C2SSelectSlicePacket {


    @Nullable
    private final BlockPos lecternPos;
    @Nullable
    private final Integer slice;
    private final ResourceKey<Level> dimension;

    public C2SSelectSlicePacket(FriendlyByteBuf buf) {
        dimension = buf.readResourceKey(Registries.DIMENSION);
        if (buf.readBoolean()) {
            slice = null;
        } else slice = buf.readVarInt();
        if(buf.readBoolean()){
            lecternPos = null;
        }else{
            lecternPos = buf.readBlockPos();
        }
    }

    public C2SSelectSlicePacket(@Nullable Integer integer, @Nullable BlockPos lecternPos, ResourceKey<Level> dimension) {
        this.slice = integer;
        this.lecternPos = lecternPos;
        this.dimension = dimension;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceKey(dimension);
        buf.writeBoolean(slice == null);
        if (slice != null) {
            buf.writeVarInt(slice);
        }
        buf.writeBoolean(lecternPos == null);
        if(lecternPos  != null){
            buf.writeBlockPos(lecternPos);
        }
    }

    public void apply(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;
            ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
            if (!atlas.isEmpty()) {
                MapAtlasItem.setSelectedSlice(atlas, slice, dimension);
            }
        });
        context.get().setPacketHandled(true);
    }
}
