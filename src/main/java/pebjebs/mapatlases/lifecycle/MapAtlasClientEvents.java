package pebjebs.mapatlases.lifecycle;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import pebjebs.mapatlases.MapAtlasesMod;
import pebjebs.mapatlases.client.MapAtlasesClient;
import pebjebs.mapatlases.networking.MapAtlasesInitAtlasS2CPacket;
import pebjebs.mapatlases.utils.MapAtlasesAccessUtils;

public class MapAtlasClientEvents {

/*
    @SubscribeEvent
    public static void onKeyPressed(InputEvent.Key event){
        if( event.getKey() == MapAtlasesClient.OPEN_ATLAS_KEYBIND.getKey().getValue()){
            Minecraft client = Minecraft.getInstance();
            if (client.level == null || client.player == null) return;
            ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(client.player);
            if (atlas.isEmpty()) return;
            MapAtlasesOpenGUIC2SPacket p = new MapAtlasesOpenGUIC2SPacket(atlas);
            PacketByteBuf packetByteBuf = new PacketByteBuf(Unpooled.buffer());
            p.write(packetByteBuf);
            client.level.sendPacket(
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
            if (client.level == null || client.player == null) {
                return;
            }
            MapItemSavedData state = p.getMapItemSavedData();
            ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(client.player);
            state.update(client.player, atlas);
            state.getPlayerSyncData(client.player);
            client.level.putClientsideMapItemSavedData(p.getMapId(), state);
        });
    }

    public static void mapAtlasClientSync(
            Minecraft client,
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
    }*/ //TODO: PORT
}
