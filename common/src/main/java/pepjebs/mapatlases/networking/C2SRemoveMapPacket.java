package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapId;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.MapType;

public class C2SRemoveMapPacket implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, C2SRemoveMapPacket> TYPE = Message.makeType(
            MapAtlasesMod.res("remove_map"),
            C2SRemoveMapPacket::new
    );

    private final MapId mapId;
    private final MapType mapType;

    public C2SRemoveMapPacket(MapId mapId, MapType type) {
        this.mapId = mapId;
        this.mapType = type;
    }

    public C2SRemoveMapPacket(FriendlyByteBuf buf) {
        this.mapId = MapId.STREAM_CODEC.decode(buf);
        this.mapType = MapType.STREAM_CODEC.decode(buf);
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        MapId.STREAM_CODEC.encode(buf, mapId);
        MapType.STREAM_CODEC.encode(buf, mapType);
    }

    @Override
    public void handle(Context context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) return;

        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
        if (!atlas.isEmpty()) {
            MapAtlasItem.removeAndDropMap(mapId, mapType, atlas, player);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE.type();
    }
}
