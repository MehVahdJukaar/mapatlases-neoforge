package pebjebs.mapatlases.networking;

import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import pebjebs.mapatlases.MapAtlasesMod;

import java.util.Optional;

public class MapAtlasNetowrking {

    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            MapAtlasesMod.res("channel"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        CHANNEL.registerMessage(0, MapAtlasesInitAtlasS2CPacket.class,
                MapAtlasesInitAtlasS2CPacket::write, MapAtlasesInitAtlasS2CPacket::new, MapAtlasesInitAtlasS2CPacket::apply,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

}
