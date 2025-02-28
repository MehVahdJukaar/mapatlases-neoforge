package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

public class C2SRemoveMapPacket implements Message {

    public final TypeAndCodec<RegistryFriendlyByteBuf, C2SRemoveMapPacket> CODEC = Message.makeType(
            MapAtlasesMod.res("remove_map"),
            C2SRemoveMapPacket::new
    );

    private final int mapId;

    public C2SRemoveMapPacket(FriendlyByteBuf buf) {
        this.mapId = buf.readInt();
    }

    public C2SRemoveMapPacket(int mapId) {
        this.mapId = mapId;
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeInt(mapId);
    }

    @Override
    public void handle(Context context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) return;

        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
        if (!atlas.isEmpty()) {
            MapAtlasItem.removeAndDropMap(atlas, mapId, player);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return CODEC.type();
    }
}
