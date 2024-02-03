package pepjebs.mapatlases.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapBanner;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.moonlight.MoonlightCompat;
import pepjebs.mapatlases.map_collection.MapKey;
import pepjebs.mapatlases.mixin.MapItemSavedDataAccessor;
import pepjebs.mapatlases.networking.MapAtlasesNetworking;
import pepjebs.mapatlases.networking.S2CDebugUpdateMapPacket;

import java.util.Iterator;
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
        return MapKey.at(data.scale, data.x, data.z, slice);
    }

    public void updateMap(ServerPlayer player) {
        if (canMultiThread(player.level)) {
            EXECUTORS.submit(() -> {
                //the only unsafe operation that this does is data.getHoldingPlayer
                //we need to redirect it.
                ((MapItem) type.filled).update(player.level, player, data);
            });
            //update markers on the main thread. has to be done because block entities cant be accessed off thread

            //calculate range
            updateMarkers(player, 128);

        } else {
            ((MapItem) type.filled).update(player.level, player, data);
        }
        if (MapAtlasesConfig.debugUpdate.get()) {
            MapAtlasesNetworking.CHANNEL.sendToClientPlayer(player, new S2CDebugUpdateMapPacket(stringId));
        }
    }

    private static boolean canMultiThread(Level level) {
        MapAtlasesConfig.UpdateType updateType = MapAtlasesConfig.mapUpdateMultithreaded.get();
        return switch (updateType) {
            case OFF -> false;
            case ALWAYS_ON -> true;
            case SINGLE_PLAYER_ONLY -> !level.getServer().isPublished();
        };
    }

    private void updateMarkers(Player player, int maxRange) {
        int step = data.getHoldingPlayer(player).step;
        int frenquency = MapAtlasesConfig.markersUpdatePeriod.get();
        if (step % frenquency == 0) {
            MapItemSavedDataAccessor accessor = (MapItemSavedDataAccessor) data;
            var markers = accessor.getBannerMarkers();
            Iterator<MapBanner> iterator = markers.values().iterator();

            Level level = player.level;
            while (iterator.hasNext()) {
                var banner = iterator.next();
                BlockPos pos = banner.getPos();
                //update all loaded in range
                if (pos.distToCenterSqr(player.position()) < (maxRange * maxRange)) {
                    if (level.isLoaded(pos)) {
                        MapBanner mapbanner1 = MapBanner.fromWorld(level, pos);
                        if (!banner.equals(mapbanner1)) {
                            iterator.remove();
                            accessor.invokeRemoveDecoration(banner.getId());
                        }
                    }
                }
            }
            if (MapAtlasesMod.MOONLIGHT) MoonlightCompat.updateMarkers(data, player, maxRange);

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
