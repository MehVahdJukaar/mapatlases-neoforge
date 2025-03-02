package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;

public class MapAtlasesNetworking {


    private static void registerMessages(NetworkHelper.RegisterMessagesEvent event) {
        //event.registerClientBound(S2CMapPacketWrapper.TYPE);
        event.registerClientBound(S2CWorldHashPacket.TYPE);
        event.registerClientBound(S2CDebugUpdateMapPacket.TYPE);
        event.registerBidirectional(C2S2COpenAtlasScreenPacket.TYPE);
        event.registerServerBound(C2SSelectSlicePacket.TYPE);
        event.registerServerBound(C2STeleportPacket.TYPE);
        event.registerServerBound(C2SMarkerPacket.TYPE);
        event.registerServerBound(C2SRemoveMarkerPacket.TYPE);
        event.registerServerBound(C2STakeAtlasPacket.TYPE);
        event.registerServerBound(C2SRemoveMapPacket.TYPE);
    }

    public static void init() {
        NetworkHelper.addNetworkRegistration(MapAtlasesNetworking::registerMessages,6);

    }

}
