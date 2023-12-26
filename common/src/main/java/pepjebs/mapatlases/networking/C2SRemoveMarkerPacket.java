package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.moonlight.MoonlightCompat;

public class C2SRemoveMarkerPacket implements Message {

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

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeVarInt(decoHash);
        buf.writeUtf(mapId);
    }

    @Override
    public void handle(ChannelHandler.Context context) {
        if (!(context.getSender() instanceof ServerPlayer player)) return;

        Level level = player.level();
        MapItemSavedData data = level.getMapData(mapId);

        if (data != null) {
            data.decorations.entrySet().removeIf(e -> e.getValue().hashCode() == decoHash);
        }
        if(MapAtlasesMod.MOONLIGHT){
            MoonlightCompat.removeCustomDecoration(data, decoHash);
        }

    }
}
