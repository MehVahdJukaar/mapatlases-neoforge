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

    private final String decoId;
    private final String mapId;

    public C2SRemoveMarkerPacket(FriendlyByteBuf buf) {
        this.decoId = buf.readUtf();
        this.mapId = buf.readUtf();

    }

    public C2SRemoveMarkerPacket(String map, String decoId) { //sending hash. hacky
        this.decoId = decoId;
        this.mapId = map;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(decoId);
        buf.writeUtf(mapId);
    }

    public void apply(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;
            Level level = player.level();
            MapItemSavedData data = level.getMapData(mapId);

            if (data != null) {
                data.decorations.remove(decoId);
            }
            if(MapAtlasesMod.MOONLIGHT){
                MoonlightCompat.removeCustomDecoration(data, decoId);
            }

        });
        context.get().setPacketHandled(true);
    }

}
