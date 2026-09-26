package pepjebs.mapatlases.utils;

import com.google.common.base.Preconditions;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapBanner;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.config.UpdateType;
import pepjebs.mapatlases.integration.moonlight.MoonlightCompat;
import pepjebs.mapatlases.map_collection.MapGridKey;
import pepjebs.mapatlases.mixin.MapItemSavedDataAccessor;
import pepjebs.mapatlases.networking.S2CDebugUpdateMapPacket;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AtlasMap {
    public final MapId id;
    public final MapItemSavedData data;

    // redundant info, cache basically as we use this for data structures
    public final Slice slice;
    public final MapType type;
    @Nullable
    public final Integer height;


    public AtlasMap(MapId id, MapType type, @NotNull MapItemSavedData data) {
        Preconditions.checkNotNull(data);
        this.id = id;
        this.data = data;
        this.type = type;
        this.height = type.getHeight(data);
        this.slice = Slice.of(type, height, data.dimension);
    }

    @Nullable
    public static AtlasMap find(MapId id, MapType type, Level level) {
        MapItemSavedData data = type.getMapData(level, id);
        if (data == null) return null;
        return new AtlasMap(id, type, data);
    }

    @Nullable
    public static AtlasMap fromFilledMapItem(Level level, ItemStack stack) {
        MapId id = MapType.filledMapId(stack);
        if (id == null) return null;
        return find(id, MapType.fromFilledMap(stack.getItem()), level);
    }

    public void tickCarriedByAndSync(ServerPlayer player, ItemStack atlas, TriState forceBeingCarried) {
        MapAtlasesMod.setMapInInventoryHack(forceBeingCarried);
        //hack. just to be sure so contains will fail
        data.tickCarriedBy(player, atlas);
        syncToClient(player);
        MapAtlasesMod.setMapInInventoryHack(TriState.PASS);
    }

    // will fail if tickCarriedBy isnt sent
    private void syncToClient(ServerPlayer player) {
        //ok so hear me out. we use this to send new map holder to the client when needed. thing is this packet isnt enough on its own
        // i need it for another mod so i'm using some code in moonlight which upgrades it to send center and dimension too (as well as custom colors)
        //TODO: maybe use isComplex  update packet and inventory tick
        Packet<?> p = data.getUpdatePacket(id, player);
        if (p != null) {
            player.connection.send(p);
        }
    }

    public MapGridKey makeKey() {
        return MapGridKey.at(data.scale, slice, data.centerX, data.centerZ);
    }

    public void updateMapColorsAndMarkers(ServerPlayer player) {
        if (canMultiThread(player.level())) {
            EXECUTORS.execute(() -> {
                try {
                    type.getFilled().update(player.level(), player, data);
                } catch (Exception e) {
                    MapAtlasesMod.LOGGER.error("Failed to update map {} off thread", id, e);
                }
            });
            //update markers on the main thread. has to be done because block entities cant be accessed off thread

            //calculate range
            updateMarkers(player, 128);

        } else {
            type.getFilled().update(player.level(), player, data);
        }
        if (MapAtlasesConfig.debugUpdate.get() || PlatHelper.isDev()) {
            NetworkHelper.sendToClientPlayer(player, new S2CDebugUpdateMapPacket(id, type));
        }
    }

    private static boolean canMultiThread(Level level) {
        UpdateType updateType = MapAtlasesConfig.mapUpdateMultithreaded.get();
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
            MoonlightCompat.updateMarkers(data, player, maxRange);

        }
    }

    private static final ExecutorService EXECUTORS = Executors.newFixedThreadPool(6, runnable -> {
        Thread thread = new Thread(runnable, "Map Atlases Map Updater");
        thread.setDaemon(true);
        return thread;
    });


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AtlasMap holder = (AtlasMap) o;
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
