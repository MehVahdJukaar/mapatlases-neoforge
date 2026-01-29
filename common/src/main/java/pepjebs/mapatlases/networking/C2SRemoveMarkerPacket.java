package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.moonlight.MoonlightCompat;
import pepjebs.mapatlases.utils.MapType;

public class C2SRemoveMarkerPacket implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, C2SRemoveMarkerPacket> TYPE = Message.makeType(
            MapAtlasesMod.res("remove_marker"),
            C2SRemoveMarkerPacket::new
    );

    private final int decoHash;
    private final MapId mapId;
    private final MapType mapType;
    private final boolean isCustom;

    public C2SRemoveMarkerPacket(FriendlyByteBuf buf) {
        this.mapId = MapId.STREAM_CODEC.decode(buf);
        this.mapType = MapType.STREAM_CODEC.decode(buf);
        this.decoHash = buf.readVarInt();
        this.isCustom = buf.readBoolean();
    }

    public C2SRemoveMarkerPacket(MapId mapId, MapType mapType,  int decoId, boolean custom) {
        // Sending hash, hacky.
        // Have to because client doesn't know deco id
        this.decoHash = decoId;
        this.mapId = mapId;
        this.mapType = mapType;
        this.isCustom = custom;
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        MapId.STREAM_CODEC.encode(buf, mapId);
        MapType.STREAM_CODEC.encode(buf, mapType);
        buf.writeVarInt(decoHash);
        buf.writeBoolean(isCustom);

    }

    @Override
    public void handle(Context context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) return;

        Level level = player.level();
        MapItemSavedData data = mapType.getMapData(level, mapId);

        if (data != null) {
            if (!isCustom) {
                if (!removeBannerMarker(data, level, decoHash)) {
                    MapAtlasesMod.LOGGER.warn("Tried to delete banner marker but none was found");
                }
            } else if (MapAtlasesMod.MOONLIGHT) {
                MoonlightCompat.removeCustomDecoration(data, decoHash);
            }
        }


    }
    //TODO only allow x on banners

    //Turbo jank code
    public static boolean removeBannerMarker(MapItemSavedData data, Level level, int hash) {
        for (var mapBanner : data.getBanners()) {
            var type = mapBanner.getDecoration();

            // recreates deco...
            float rotation = 180;
            int i = 1 << data.scale;
            float f = (float) (mapBanner.pos().getX() - (double) data.centerX) / i;
            float g = (float) (mapBanner.pos().getZ() - (double) data.centerZ) / i;
            byte b = (byte) ((int) ((f * 2.0F) + 0.5));
            byte c = (byte) ((int) ((g * 2.0F) + 0.5));

            byte d;
            if (f >= -63.0F && g >= -63.0F && f <= 63.0F && g <= 63.0F) {
                rotation += 8.0;
                d = (byte) ((int) (rotation * 16.0 / 360.0));
                if (data.dimension == Level.NETHER && level != null) {
                    int k = (int) (level.getLevelData().getDayTime() / 10L);
                    d = (byte) (k * k * 34187121 + k * 121 >> 15 & 15);
                }
            } else {
                d = 0;
                if (f <= -63.0F) {
                    b = -128;
                }

                if (g <= -63.0F) {
                    c = -128;
                }

                if (f >= 63.0F) {
                    b = 127;
                }

                if (g >= 63.0F) {
                    c = 127;
                }
            }
            MapDecoration mapDecoration = new MapDecoration(type, (byte) (b + 1), (byte) (c + 1), d, mapBanner.name());

            if (mapDecoration.hashCode() == hash) {
                data.toggleBanner(level, mapBanner.pos());
                return true;
            }
        }
        return false;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE.type();
    }
}
