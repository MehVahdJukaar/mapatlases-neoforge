package pepjebs.mapatlases.networking;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.network.NetworkEvent;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.MoonlightCompat;
import pepjebs.mapatlases.mixin.MapItemSavedDataAccessor;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

public class C2SMarkerPacket {


    private final BlockPos pos;
    private final String mapId;

    public C2SMarkerPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.mapId = buf.readUtf();

    }

    public C2SMarkerPacket(BlockPos pos, String map) {
        this.pos = pos;
        this.mapId = map;

    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(mapId);
    }

    public void apply(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;
            Level level = player.level();
            MapItemSavedData data = level.getMapData(mapId);

            if (data instanceof MapItemSavedDataAccessor d) {

                double d0 = pos.getX() + 0.5D;
                double d1 = pos.getZ() + 0.5D;
                String str = MapAtlasesConfig.pinMarkerId.get();
                if(!str.isEmpty()) {
                    ResourceLocation id = new ResourceLocation(str);

                    if (id.getNamespace().equals("minecraft")) {
                        Optional<MapDecoration.Type> opt = Arrays.stream(MapDecoration.Type.values()).filter(t -> t.toString()
                                .toLowerCase().equals(id.getPath())).findFirst();
                        opt.ifPresent(type -> d.invokeAddDecoration(
                                type
                                , level,
                                "pin_" + pos,
                                d0, d1, 180.0D, null));
                    }else{
                        if(MapAtlasesMod.MOONLIGHT){
                            MoonlightCompat.addDecoration(data, pos, id);
                        }
                    }
                }
            }

        });
        context.get().setPacketHandled(true);
    }

}
