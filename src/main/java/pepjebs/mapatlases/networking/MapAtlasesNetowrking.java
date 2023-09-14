package pepjebs.mapatlases.networking;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import pepjebs.mapatlases.MapAtlasesMod;

import java.util.Optional;

public class MapAtlasesNetowrking {

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

        CHANNEL.registerMessage(index++, C2SOpenAtlasPacket.class,
                C2SOpenAtlasPacket::write, C2SOpenAtlasPacket::new, C2SOpenAtlasPacket::apply,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(index++, C2SRequestMapCenterPacket.class,
                C2SRequestMapCenterPacket::write, C2SRequestMapCenterPacket::new, C2SRequestMapCenterPacket::apply,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }


    public static void sendToClientPlayer(ServerPlayer serverPlayer, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), message);
    }

    public static void sendToServer(Object atlasKeybindPacket) {
        CHANNEL.sendToServer(atlasKeybindPacket);
    }
}
