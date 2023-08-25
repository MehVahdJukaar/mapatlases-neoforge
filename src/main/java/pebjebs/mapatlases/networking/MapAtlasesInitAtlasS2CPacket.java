package pebjebs.mapatlases.networking;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import pebjebs.mapatlases.MapAtlasesMod;

import java.util.Optional;
import java.util.function.Supplier;

public class MapAtlasesInitAtlasS2CPacket {


    private final String mapId;
    private final MapItemSavedData mapState;

    public MapAtlasesInitAtlasS2CPacket(FriendlyByteBuf buf) {
        mapId = buf.readUtf();
        CompoundTag nbt = buf.readNbt();
        if (nbt == null) {
            MapAtlasesMod.LOGGER.warn("Null MapItemSavedData NBT received by client");
            mapState = null;
        } else {
            mapState = MapItemSavedData.load(nbt);
        }
    }

    public MapAtlasesInitAtlasS2CPacket(String mapId1, MapItemSavedData mapState1) {
        mapId = mapId1;
        mapState = mapState1;
    }

    public void write(FriendlyByteBuf buf) {
        CompoundTag mapAsTag = new CompoundTag();
        mapState.save(mapAsTag);
        buf.writeUtf(mapId);
        buf.writeNbt(mapAsTag);
    }

    public void apply(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            Level level = context.get().getSender().level();
            if (level == null) return;
            level.setMapData(mapId, mapState);
        });
    }

}
