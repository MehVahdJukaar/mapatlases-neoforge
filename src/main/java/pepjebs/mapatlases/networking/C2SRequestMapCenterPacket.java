package pepjebs.mapatlases.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// vanilla doesn't send this automatically so we have to do it
public class C2SRequestMapCenterPacket {

    private final String mapId;
    public C2SRequestMapCenterPacket(FriendlyByteBuf buf) {
        mapId = buf.readUtf();
    }

    public C2SRequestMapCenterPacket(String mapId) {
        this.mapId = mapId;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(mapId);
    }

    public void apply(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;
            MapItemSavedData data = player.level().getMapData(mapId);
            if(data != null) {
                MapAtlasesNetowrking.sendToClientPlayer(player,
                        new S2CSyncMapCenterPacket(mapId, data.centerX, data.centerZ));
            }
        });
        context.get().setPacketHandled(true);
    }
}
