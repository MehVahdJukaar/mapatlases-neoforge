package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.saveddata.maps.MapId;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.utils.MapType;

public class S2CDebugUpdateMapPacket implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, S2CDebugUpdateMapPacket> TYPE = Message.makeType(
            MapAtlasesMod.res("debug_update_map"),
            S2CDebugUpdateMapPacket::new
    );

    private final MapId mapId;
    private final MapType mapType;

    public S2CDebugUpdateMapPacket(FriendlyByteBuf buf) {
        this.mapId = MapId.STREAM_CODEC.decode(buf);
        this.mapType = MapType.STREAM_CODEC.decode(buf);
    }

    public S2CDebugUpdateMapPacket(MapId map, MapType mapType) {
        this.mapType = mapType;
        this.mapId = map;
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        MapId.STREAM_CODEC.encode(buf, mapId);
        MapType.STREAM_CODEC.encode(buf, mapType);
    }

    @Override
    public void handle(Context context) {
        MapAtlasesClient.debugMapUpdated(mapId, mapType);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE.type();
    }
}
