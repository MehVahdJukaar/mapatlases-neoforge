package pepjebs.mapatlases.lifecycle;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.networking.MapAtlasesInitAtlasS2CPacket;
import pepjebs.mapatlases.networking.MapAtlasesOpenGUIC2SPacket;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

public class MapAtlasesClientLifecycleEvents {

    public static void mapAtlasClientTick(MinecraftClient client) {
        while (MapAtlasesClient.displayMapGUIBinding.wasPressed()) {
            if (client.world == null || client.player == null) return;
            ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(client.player);
            if (atlas.isEmpty()) return;
            MapAtlasesOpenGUIC2SPacket p = new MapAtlasesOpenGUIC2SPacket(atlas);
            PacketByteBuf packetByteBuf = new PacketByteBuf(Unpooled.buffer());
            p.write(packetByteBuf);
            client.world.sendPacket(
                    new CustomPayloadC2SPacket(MapAtlasesOpenGUIC2SPacket.MAP_ATLAS_OPEN_GUI, packetByteBuf));
        }
    }

    public static void mapAtlasClientInit(
            MinecraftClient client,
            ClientPlayNetworkHandler _handler,
            PacketByteBuf buf,
            PacketSender _sender) {
        MapAtlasesInitAtlasS2CPacket p = new MapAtlasesInitAtlasS2CPacket(buf);
        client.execute(() -> {
            if (client.world == null || client.player == null) {
                return;
            }
            MapState state = p.getMapState();
            ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(client.player);
            state.update(client.player, atlas);
            state.getPlayerSyncData(client.player);
            client.world.putClientsideMapState(p.getMapId(), state);
        });
    }

    public static void mapAtlasClientSync(
            MinecraftClient client,
            ClientPlayNetworkHandler handler,
            PacketByteBuf buf,
            PacketSender _sender) {
        try {
            MapUpdateS2CPacket p = new MapUpdateS2CPacket(buf);
            client.execute(() -> {
                handler.onMapUpdate(p);
            });
        } catch (ArrayIndexOutOfBoundsException e) {
            MapAtlasesMod.LOGGER.error("Bad Minecraft MapUpdate packet sent to client by server");
            MapAtlasesMod.LOGGER.error(e);
        }
    }
}
