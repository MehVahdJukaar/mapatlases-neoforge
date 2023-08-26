package pebjebs.mapatlases.networking;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.network.NetworkEvent;
import pebjebs.mapatlases.MapAtlasesMod;
import pebjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.function.Supplier;

public class S2CSetMapDataPacket {


    private final String mapId;
    private final MapItemSavedData mapData;
    private final boolean isOnJoin;

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
            ServerPlayer player = context.get().getSender();
            Level level = player.level();
            if (level == null) return;

            if (isOnJoin) {
                ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
                mapData.tickCarriedBy(player, atlas);
                mapData.getHoldingPlayer(player);
            }
            level.setMapData(mapId, mapData);

        });
    }

}
