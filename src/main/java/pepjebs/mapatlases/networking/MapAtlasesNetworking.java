package pepjebs.mapatlases.networking;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import pepjebs.mapatlases.MapAtlasesMod;

import java.util.Optional;

public class MapAtlasesNetworking {

    private static final String VERSION = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            MapAtlasesMod.res("channel"),
            () -> VERSION, VERSION::equals, VERSION::equals);
    private static int index = 0;

    public static void register() {
        CHANNEL.registerMessage(index++, S2CSetMapDataPacket.class,
                S2CSetMapDataPacket::write, S2CSetMapDataPacket::new, S2CSetMapDataPacket::apply,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(index++, S2CSetActiveMapPacket.class,
                S2CSetActiveMapPacket::write, S2CSetActiveMapPacket::new, S2CSetActiveMapPacket::apply,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(index++, S2CSyncMapCenterPacket.class,
                S2CSyncMapCenterPacket::write, S2CSyncMapCenterPacket::new, S2CSyncMapCenterPacket::apply,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(index++, C2SCOpenAtlasScreenPacket.class,
                C2SCOpenAtlasScreenPacket::write, C2SCOpenAtlasScreenPacket::new, C2SCOpenAtlasScreenPacket::apply,
                Optional.empty());
        CHANNEL.registerMessage(index++, C2SRequestMapCenterPacket.class,
                C2SRequestMapCenterPacket::write, C2SRequestMapCenterPacket::new, C2SRequestMapCenterPacket::apply,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
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
