package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkDir;
import pepjebs.mapatlases.MapAtlasesMod;

public class MapAtlasesNetworking {

    public static final ChannelHandler CHANNEL = ChannelHandler.builder(MapAtlasesMod.MOD_ID)
            .version(5)
            .register(NetworkDir.PLAY_TO_CLIENT, S2CSetActiveMapPacket.class, S2CSetActiveMapPacket::new)
            .register(NetworkDir.PLAY_TO_CLIENT, S2CMapPacketWrapper.class, S2CMapPacketWrapper::new)
            .register(NetworkDir.PLAY_TO_CLIENT, S2CWorldHashPacket.class, S2CWorldHashPacket::new)
            .register(NetworkDir.PLAY_TO_CLIENT, S2CDebugUpdateMapPacket.class, S2CDebugUpdateMapPacket::new)
            // both dir
            .register(NetworkDir.BOTH, C2S2COpenAtlasScreenPacket.class, C2S2COpenAtlasScreenPacket::new)

            .register(NetworkDir.PLAY_TO_SERVER, C2SSelectSlicePacket.class, C2SSelectSlicePacket::new)
            .register(NetworkDir.PLAY_TO_SERVER, C2STeleportPacket.class, C2STeleportPacket::new)
            .register(NetworkDir.PLAY_TO_SERVER, C2SMarkerPacket.class, C2SMarkerPacket::new)
            .register(NetworkDir.PLAY_TO_SERVER, C2SRemoveMarkerPacket.class, C2SRemoveMarkerPacket::new)
            .register(NetworkDir.PLAY_TO_SERVER, C2STakeAtlasPacket.class, C2STakeAtlasPacket::new)


            .build();

    public static void init() {

    }
}
