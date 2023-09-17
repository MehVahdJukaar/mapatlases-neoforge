package pepjebs.mapatlases.networking;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;

import java.util.function.Supplier;

public class S2CSetMapDataPacket {

    public final String mapId;
    public final MapItemSavedData mapData;
    public final boolean isOnJoin;

    public S2CSetMapDataPacket(FriendlyByteBuf buf) {
        mapId = buf.readUtf();
        CompoundTag nbt = buf.readNbt();
        if (nbt == null) {
            MapAtlasesMod.LOGGER.warn("Null MapItemSavedData NBT received by client");
            mapData = null;
        } else {
            mapData = MapItemSavedData.load(nbt);
        }
        isOnJoin = buf.readBoolean();
    }

    public S2CSetMapDataPacket(String mapId, MapItemSavedData mapData, boolean isOnJoin) {
        this.mapId = mapId;
        this.mapData = mapData;
        this.isOnJoin = isOnJoin;
    }

    public void write(FriendlyByteBuf buf) {
        CompoundTag mapAsTag = new CompoundTag();
        mapData.save(mapAsTag);
        buf.writeUtf(mapId);
        buf.writeNbt(mapAsTag);
        buf.writeBoolean(isOnJoin);
    }

    public void apply(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT)
                MapAtlasesClient.setClientMapData(this);
        });
        context.get().setPacketHandled(true);
    }


}
