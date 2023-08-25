package pepjebs.mapatlases.networking;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.Identifier;
import pepjebs.mapatlases.MapAtlasesMod;

public class MapAtlasesInitAtlasS2CPacket implements Packet<ClientPlayPacketListener> {

    public static final Identifier MAP_ATLAS_INIT = new Identifier(MapAtlasesMod.MOD_ID, "map_atlas_init");
    public static final Identifier MAP_ATLAS_SYNC = new Identifier(MapAtlasesMod.MOD_ID, "map_atlas_sync");

    private final String mapId;
    private final MapState mapState;

    public MapAtlasesInitAtlasS2CPacket(PacketByteBuf buf) {
        mapId = buf.readString();
        NbtCompound nbt = buf.readNbt();
        if (nbt == null) {
            MapAtlasesMod.LOGGER.warn("Null MapState NBT received by client");
            mapState = null;
        } else {
            mapState = MapState.fromNbt(nbt);
        }
    }

    public MapAtlasesInitAtlasS2CPacket(String mapId1, MapState mapState1) {
        mapId = mapId1;
        mapState = mapState1;
    }

    @Override
    public void write(PacketByteBuf buf) {
        NbtCompound mapAsTag = new NbtCompound();
        mapState.writeNbt(mapAsTag);
        buf.writeString(mapId);
        buf.writeNbt(mapAsTag);
    }

    @Override
    public void apply(ClientPlayPacketListener listener) {
        MinecraftClient.getInstance().execute(() -> {
            if (MinecraftClient.getInstance().world == null) return;
            MinecraftClient.getInstance().world.putClientsideMapState(mapId, mapState);
        });
    }

    public MapState getMapState() {return this.mapState;}

    public String getMapId() {return this.mapId;}
}
