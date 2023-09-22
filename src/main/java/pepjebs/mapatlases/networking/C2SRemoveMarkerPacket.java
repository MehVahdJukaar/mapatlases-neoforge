package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.network.NetworkEvent;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.MoonlightCompat;

import java.util.function.Supplier;

public class C2SRemoveMarkerPacket {

    private final int hash;
    private final String mapId;

    public C2SRemoveMarkerPacket(FriendlyByteBuf buf) {
        this.hash = buf.readVarInt();
        this.mapId = buf.readUtf();

    }

    public C2SRemoveMarkerPacket(String map, int hash) { //sending hash. hacky
        this.hash = hash;
        this.mapId = map;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(hash);
        buf.writeUtf(mapId);
    }

    public void apply(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;
            Level level = player.level();
            MapItemSavedData data = level.getMapData(mapId);

            if (data != null) {
                data.decorations.entrySet().removeIf(e -> e.getValue().hashCode() == hash);
            }
            if(MapAtlasesMod.MOONLIGHT){
                MoonlightCompat.removeCustomDecoration(data, hash);
            }

        });
        context.get().setPacketHandled(true);
    }

}
