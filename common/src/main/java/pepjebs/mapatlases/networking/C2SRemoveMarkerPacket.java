package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.moonlight.MoonlightCompat;

public class C2SRemoveMarkerPacket implements Message {

    private final int decoHash;
    private final String mapId;
    private final boolean isCustom;

    public C2SRemoveMarkerPacket(FriendlyByteBuf buf) {
        this.mapId = buf.readUtf();
        this.decoHash = buf.readVarInt();
        this.isCustom = buf.readBoolean();
    }

    public C2SRemoveMarkerPacket(String map, int decoId, boolean custom) {
        // Sending hash, hacky.
        // Have to because client doesn't know deco id
        this.decoHash = decoId;
        this.mapId = map;
        this.isCustom = custom;
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeUtf(mapId);
        buf.writeVarInt(decoHash);
        buf.writeBoolean(isCustom);

    }

    @Override
    public void handle(ChannelHandler.Context context) {
        if (!(context.getSender() instanceof ServerPlayer player)) return;

        Level level = player.level;
        MapItemSavedData data = level.getMapData(mapId);

        if (data != null) {
            if (!isCustom) {
                if(!removeBannerMarker(data, level, decoHash)){
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
            float f = (float) (mapBanner.getPos().getX() - (double) data.x) / i;
            float g = (float) (mapBanner.getPos().getZ() - (double) data.z) / i;
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
            MapDecoration mapDecoration = new MapDecoration(type, (byte) (b+1), (byte) (c+1), d, mapBanner.getName());

            if (mapDecoration.hashCode() == hash) {
                data.toggleBanner(level, mapBanner.getPos());
                return true;
            }
        }
        return false;
    }
}
