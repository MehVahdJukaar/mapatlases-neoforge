package pepjebs.mapatlases.utils;

import com.google.common.base.Preconditions;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapBanner;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.moonlight.MoonlightCompat;
import pepjebs.mapatlases.map_collection.MapSearchKey;
import pepjebs.mapatlases.mixin.MapItemSavedDataAccessor;
import pepjebs.mapatlases.networking.S2CDebugUpdateMapPacket;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapDataHolder {
    public final MapId id;
    public final MapItemSavedData data;

    // redundant info, cache basically as we use this for data structures
    public final Slice slice;
    public final MapType type;
    @Nullable
    public final Integer height;


    public MapDataHolder(MapId id, MapType type, @NotNull MapItemSavedData data) {
        Preconditions.checkNotNull(data);
        this.id = id;
        this.data = data;
        this.type = type;
        this.height = type.getHeight(data);
        this.slice = Slice.of(type, height, data.dimension);
    }

    @Nullable
    public static MapDataHolder get(MapId id, MapType type, Level level) {
        MapItemSavedData data = type.getMapData(level, id);
        if (data == null) return null;
        return new MapDataHolder(id, type, data);
    }

    public MapSearchKey makeKey() {
        return MapSearchKey.at(data.scale, data.centerX, data.centerZ, slice);
    }

    public void updateMap(ServerPlayer player) {
        if (canMultiThread(player.level())) {
            EXECUTORS.submit(() -> {
                //the only unsafe operation that this does is data.getHoldingPlayer
                //we need to redirect it.
                type.getFilled().update(player.level(), player, data);
            });
            //update markers on the main thread. has to be done because block entities cant be accessed off thread

            //calculate range
            updateMarkers(player, 128);

        } else {
            type.getFilled().update(player.level(), player, data);
        }
        if (MapAtlasesConfig.debugUpdate.get()) {
            NetworkHelper.sendToClientPlayer(player, new S2CDebugUpdateMapPacket(id, type));
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

            Level level = player.level();
            while (iterator.hasNext()) {
                var banner = iterator.next();
                BlockPos pos = banner.pos();
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

    public ItemStack createExistingMapItem() {
        return type.createExistingMapItem(id, slice.height());
    }
}
