package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkDir;
import pepjebs.mapatlases.MapAtlasesMod;

public class MapAtlasesNetworking {

    public static final ChannelHandler CHANNEL = ChannelHandler.createChannel(MapAtlasesMod.res("channel"));


    public static void init() {

        CHANNEL.register(NetworkDir.PLAY_TO_CLIENT, S2CMapPacketWrapper.class, S2CMapPacketWrapper::new);
        CHANNEL.register(NetworkDir.PLAY_TO_CLIENT, S2CDebugUpdateMapPacket.class, S2CDebugUpdateMapPacket::new);
        // both dir
        CHANNEL.register(NetworkDir.BOTH, C2S2COpenAtlasScreenPacket.class, C2S2COpenAtlasScreenPacket::new);

        CHANNEL.register(NetworkDir.PLAY_TO_SERVER, C2SSelectSlicePacket.class, C2SSelectSlicePacket::new);
        CHANNEL.register(NetworkDir.PLAY_TO_SERVER, C2STeleportPacket.class, C2STeleportPacket::new);
        CHANNEL.register(NetworkDir.PLAY_TO_SERVER, C2SMarkerPacket.class, C2SMarkerPacket::new);
        CHANNEL.register(NetworkDir.PLAY_TO_SERVER, C2SRemoveMarkerPacket.class, C2SRemoveMarkerPacket::new);
        CHANNEL.register(NetworkDir.PLAY_TO_SERVER, C2STakeAtlasPacket.class, C2STakeAtlasPacket::new);

    }
}
