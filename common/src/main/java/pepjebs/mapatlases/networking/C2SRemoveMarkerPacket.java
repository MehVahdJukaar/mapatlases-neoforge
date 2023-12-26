package pepjebs.mapatlases.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.network.NetworkEvent;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.moonlight.MoonlightCompat;

import java.util.function.Supplier;

public class C2SRemoveMarkerPacket {

    private final int decoHash;
    private final String mapId;

    public C2SRemoveMarkerPacket(FriendlyByteBuf buf) {
        this.decoHash = buf.readVarInt();
        this.mapId = buf.readUtf();

    }

    public C2SRemoveMarkerPacket(String map, int decoId) {
        // Sending hash, hacky.
        // Have to because client doesn't know deco id
        this.decoHash = decoId;
        this.mapId = map;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(decoHash);
        buf.writeUtf(mapId);
    }

    public void apply(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;
            Level level = player.level();
            MapItemSavedData data = level.getMapData(mapId);

            if (data != null) {
                data.decorations.entrySet().removeIf(e -> e.getValue().hashCode() == decoHash);
            }
            if(MapAtlasesMod.MOONLIGHT){
                MoonlightCompat.removeCustomDecoration(data, decoHash);
            }

        });
        context.get().setPacketHandled(true);
    }

}
