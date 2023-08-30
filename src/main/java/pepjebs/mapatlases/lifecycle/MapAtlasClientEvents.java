package pepjebs.mapatlases.lifecycle;

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


*/ //TODO: PORT

}
