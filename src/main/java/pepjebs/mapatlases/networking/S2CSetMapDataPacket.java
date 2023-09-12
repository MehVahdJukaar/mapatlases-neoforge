package pepjebs.mapatlases.networking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.apache.commons.compress.archivers.sevenz.CLI;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtilsOld;

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

    @OnlyIn(Dist.CLIENT)
    public void apply(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;
            Level level = player.level();

            //TODO: send less data and dont tick likehere. also send all data regardles of atlas or not
            if (isOnJoin) {
                ItemStack atlas = MapAtlasesAccessUtilsOld.getAtlasFromPlayerByConfig(player);
                mapData.tickCarriedBy(player, atlas);
                mapData.getHoldingPlayer(player);
            }
            ((ClientLevel)level).overrideMapData(mapId, mapData);

        });
    }

}
