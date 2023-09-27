package pepjebs.mapatlases.networking;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import pepjebs.mapatlases.MapAtlasesMod;

import java.util.Optional;

public class MapAtlasesNetowrking {

    private static final String VERSION = "4";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            MapAtlasesMod.res("channel"),
            () -> VERSION, VERSION::equals, VERSION::equals);
    private static int index = 0;

    public static void register() {
        CHANNEL.registerMessage(index++, S2CSetActiveMapPacket.class,
                S2CSetActiveMapPacket::write, S2CSetActiveMapPacket::new, S2CSetActiveMapPacket::apply,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(index++, S2CMapPacketWrapper.class,
                S2CMapPacketWrapper::write, S2CMapPacketWrapper::new, S2CMapPacketWrapper::apply,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(index++, C2S2COpenAtlasScreenPacket.class,
                C2S2COpenAtlasScreenPacket::write, C2S2COpenAtlasScreenPacket::new, C2S2COpenAtlasScreenPacket::apply,
                Optional.empty()); // both dir
        CHANNEL.registerMessage(index++, C2SSelectSlicePacket.class,
                C2SSelectSlicePacket::write, C2SSelectSlicePacket::new, C2SSelectSlicePacket::apply,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(index++, C2STeleportPacket.class,
                C2STeleportPacket::write, C2STeleportPacket::new, C2STeleportPacket::apply,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(index++, C2SMarkerPacket.class,
                C2SMarkerPacket::write, C2SMarkerPacket::new, C2SMarkerPacket::apply,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(index++, C2SRemoveMarkerPacket.class,
                C2SRemoveMarkerPacket::write, C2SRemoveMarkerPacket::new, C2SRemoveMarkerPacket::apply,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(index++, TakeAtlasPacket.class,
                TakeAtlasPacket::write, TakeAtlasPacket::new, TakeAtlasPacket::apply,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }


    public static void sendToClientPlayer(ServerPlayer serverPlayer, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), message);
    }

    public static void sendToServer(Object atlasKeybindPacket) {
        CHANNEL.sendToServer(atlasKeybindPacket);
    }
}
