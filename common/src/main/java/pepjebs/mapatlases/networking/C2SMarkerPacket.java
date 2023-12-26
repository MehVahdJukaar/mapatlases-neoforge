package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.moonlight.MoonlightCompat;
import pepjebs.mapatlases.mixin.MapItemSavedDataAccessor;

import java.util.Arrays;
import java.util.Optional;

public class C2SMarkerPacket implements Message {

    private final ColumnPos pos;
    private final String mapId;
    private final String name;

    public C2SMarkerPacket(FriendlyByteBuf buf) {
        this.pos = fromLong(buf.readLong());
        this.mapId = buf.readUtf();
        this.name = buf.readOptional(FriendlyByteBuf::readUtf).orElse(null);
    }

    public ColumnPos fromLong(long combinedValue) {
        var x = (int) (combinedValue);
        var z = (int) (combinedValue >>> 32);
        return new ColumnPos(x,z);
    }

    public C2SMarkerPacket(ColumnPos pos, String map, @Nullable String name) {
        this.pos = pos;
        this.mapId = map;
        this.name = name;
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeUtf(mapId);
        buf.writeOptional(Optional.ofNullable(name), FriendlyByteBuf::writeUtf);
    }

    @Override
    public void handle(ChannelHandler.Context context) {
        if (!(context.getSender() instanceof ServerPlayer player)) return;

        Level level = player.level();
        MapItemSavedData data = level.getMapData(mapId);

        if (data instanceof MapItemSavedDataAccessor d) {

            double d0 = pos.x() + 0.5D;
            double d1 = pos.z() + 0.5D;
            String str = MapAtlasesConfig.pinMarkerId.get();
            if (!str.isEmpty()) {
                ResourceLocation id = new ResourceLocation(str);

                MutableComponent literal = name == null ? null : Component.literal(name);
                if (id.getNamespace().equals("minecraft")) {
                    Optional<MapDecoration.Type> opt = Arrays.stream(MapDecoration.Type.values()).filter(t -> t.toString()
                            .toLowerCase().equals(id.getPath())).findFirst();
                    opt.ifPresent(type -> d.invokeAddDecoration(
                            type
                            , level,
                            "pin_" + pos,
                            d0, d1, 180.0D, literal));
                } else {
                    if (MapAtlasesMod.MOONLIGHT) {
                        MoonlightCompat.addDecoration(data, new BlockPos(pos.x(), 0, pos.z()), id, literal);
                    }
                }
            }
        }

    }
}
