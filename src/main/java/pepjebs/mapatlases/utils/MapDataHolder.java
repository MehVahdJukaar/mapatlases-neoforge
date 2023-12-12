package pepjebs.mapatlases.utils;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapBanner;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.capabilities.MapKey;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.moonlight.MoonlightCompat;
import pepjebs.mapatlases.mixin.MapItemSavedDataAccessor;
import pepjebs.mapatlases.networking.MapAtlasesNetworking;
import pepjebs.mapatlases.networking.S2CDebugUpdateMapPacket;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapDataHolder {
    public final int id;
    public final String stringId;
    public final MapItemSavedData data;

    // redundant info, cache basically as we use this for data structures
    public final Slice slice;
    public final MapType type;
    @Nullable
    public final Integer height;

    public MapDataHolder(String name, MapItemSavedData data) {
        this(MapAtlasesAccessUtils.findMapIntFromString(name), name, data);
    }

    private MapDataHolder(int id, String stringId, MapItemSavedData data) {
        this.id = id;
        this.stringId = stringId;
        this.data = data;
        this.type = MapType.fromKey(stringId, data);
        this.height = type.getHeight(data);
        this.slice = Slice.of(type, height, data.dimension);
    }

    @Nullable
    public static MapDataHolder findFromId(Level level, int id) {
        //try all known types
        for (var t : MapType.values()) {
            var d = t.getMapData(level, id);
            if (d != null) {
                return new MapDataHolder(id, d.getFirst(), d.getSecond());
            }
        }
        return null;
    }

    public MapKey makeKey() {
        return MapKey.at(data.scale, data.centerX, data.centerZ, slice);
    }

    public void updateMap(ServerPlayer player) {
        if (MapAtlasesConfig.mapUpdateMultithreaded.get()) {
            EXECUTORS.submit(() -> {
                ((MapItem) type.filled).update(player.level(), player, data);
            });
            //update markers on the main thread. has to be done because block entities cant be accessed off thread
            updateMarkers(player);

        } else {
            ((MapItem) type.filled).update(player.level(), player, data);
        }
        if (MapAtlasesConfig.debugUpdate.get()) {
            MapAtlasesNetworking.sendToClientPlayer(player, new S2CDebugUpdateMapPacket(stringId));
        }
    }

    private void updateMarkers(Player player) {
        int step = data.getHoldingPlayer(player).step;
        int frenquency = 5;
        if (step % frenquency == 0) {
            int i = step / frenquency;

            int j = 0;
            MapItemSavedDataAccessor accessor = (MapItemSavedDataAccessor) data;
            var markers = accessor.getBannerMarkers();
            int k = i;
            if (!markers.isEmpty()) k = k % markers.size();
            //get nth element
            for (var m : markers.entrySet()) {
                if (j++ == k) {
                    var banner = m.getValue();
                    MapBanner mapbanner1 = MapBanner.fromWorld(player.level(), banner.getPos());
                    if (!banner.equals(mapbanner1)) {
                        markers.remove(m.getKey());
                        accessor.invokeRemoveDecoration(banner.getId());
                    }
                    break;
                }
            }
            if (MapAtlasesMod.MOONLIGHT) MoonlightCompat.updateMarkers(data, player.level(), i);

        }
    }

    private static final ExecutorService EXECUTORS = Executors.newFixedThreadPool(6);


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MapDataHolder holder = (MapDataHolder) o;
        return Objects.equals(data, holder.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }
}
