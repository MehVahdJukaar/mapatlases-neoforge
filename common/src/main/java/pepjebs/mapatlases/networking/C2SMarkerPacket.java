package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.moonlight.MoonlightCompat;
import pepjebs.mapatlases.mixin.MapItemSavedDataAccessor;
import pepjebs.mapatlases.utils.MapType;

import java.util.Optional;

public class C2SMarkerPacket implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, C2SMarkerPacket> TYPE = Message.makeType(
            MapAtlasesMod.res("place_marker"),
            C2SMarkerPacket::new
    );
    private final ColumnPos pos;
    private final MapId mapId;
    private final MapType mapType;
    private final String name;

    public C2SMarkerPacket(FriendlyByteBuf buf) {
        this.mapId = MapId.STREAM_CODEC.decode(buf);
        this.mapType = MapType.STREAM_CODEC.decode(buf);
        this.pos = fromLong(buf.readLong());

        this.name = buf.readOptional(FriendlyByteBuf::readUtf).orElse(null);
    }

    public ColumnPos fromLong(long combinedValue) {
        var x = (int) (combinedValue);
        var z = (int) (combinedValue >>> 32);
        return new ColumnPos(x, z);
    }

    public C2SMarkerPacket(MapId mapId, MapType mapType, ColumnPos pos, @Nullable String name) {
        this.pos = pos;
        this.mapId = mapId;
        this.mapType = mapType;
        this.name = name;
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        MapId.STREAM_CODEC.encode(buf, mapId);
        MapType.STREAM_CODEC.encode(buf, mapType);
        buf.writeLong(pos.toLong());
        buf.writeOptional(Optional.ofNullable(name), FriendlyByteBuf::writeUtf);
    }


    @Override
    public void handle(Context context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) return;

        Level level = player.level();
        MapItemSavedData data = mapType.getMapData(level, mapId);

        if (data instanceof MapItemSavedDataAccessor d) {

            double d0 = pos.x() + 0.5D;
            double d1 = pos.z() + 0.5D;
            String str = MapAtlasesConfig.pinMarkerId.get();
            if (!str.isEmpty()) {
                ResourceLocation id = ResourceLocation.parse(str);

                MutableComponent literal = name == null ? null : Component.literal(name);
                if (id.getNamespace().equals("minecraft")) {
                    var opt = BuiltInRegistries.MAP_DECORATION_TYPE.getHolder(id);
                    opt.ifPresent(type -> d.invokeAddDecoration(
                            type, level,
                            "pin_" + pos,
                            d0, d1, 180.0D, literal));
                } else {
                    if (MapAtlasesMod.MOONLIGHT) {
                        MoonlightCompat.addDecoration(level, data, new BlockPos(pos.x(), 0, pos.z()), id, literal);
                    }
                }
            }
        }

    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE.type();
    }
}
