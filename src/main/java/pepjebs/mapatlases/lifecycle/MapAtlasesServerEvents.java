package pepjebs.mapatlases.lifecycle;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import org.joml.Vector2i;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.capabilities.MapCollectionCap;
import pepjebs.mapatlases.capabilities.MapKey;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.SupplementariesCompat;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.networking.MapAtlasesNetworking;
import pepjebs.mapatlases.networking.S2CWorldHashPacket;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.Slice;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class MapAtlasesServerEvents {

    // Used to prevent Map creation spam consuming all Empty Maps on auto-create
    private static final ReentrantLock mutex = new ReentrantLock();

    private static final WeakHashMap<Player, HashMap<String, MapUpdateTicket>> updateQueue = new WeakHashMap<>();

    //TODO: improve and make multithreaded
    private static class MapUpdateTicket {
        private static final Comparator<MapUpdateTicket> COMPARATOR = Comparator.comparingDouble(MapUpdateTicket::getPriority);

        private final MapDataHolder holder;
        private int waitTime = 20; //set to zero when this is updated.
        // if not incremented, each tick.
        // we start with lowest for newly added entries
        private double lastDistance = 1000000;
        private double currentPriority; //bigger the better

        private MapUpdateTicket(MapDataHolder data) {
            this.holder = data;
        }

        public double getPriority() {
            return currentPriority;
        }

        public void updatePriority(int px, int pz) {
            this.waitTime++;
            double distSquared = Mth.lengthSquared(px - holder.data.centerX, pz - holder.data.centerZ);
            // Define weights for distance and waitTime
            double distanceWeight = 20; // Adjust this based on your preference
            double waitTimeWeight = 1; // Adjust this based on your preference

            // Calculate the priority using a weighted sum
            double deltaDist = distanceWeight * (lastDistance - distSquared); //for maps getting closer
            this.currentPriority = deltaDist + (waitTimeWeight * this.waitTime * this.waitTime);
            this.lastDistance = distSquared;
        }
    }

    @SubscribeEvent
    public static void mapAtlasesPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.side == LogicalSide.CLIENT) {
            //caches client stuff
            MapAtlasesClient.cachePlayerState(event.player);
        } else {
            ServerPlayer player = ((ServerPlayer) event.player);
            //not needed?
            //if (player.isRemoved() || player.isChangingDimension() || player.hasDisconnected()) continue;

            var server = player.server;
            ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
            if (atlas.isEmpty()) return;
            Level level = player.level();
            MapCollectionCap maps = MapAtlasItem.getMaps(atlas, level);
            Slice slice = MapAtlasItem.getSelectedSlice(atlas, level.dimension());

            // sets new center map

            MapKey activeKey = MapKey.at(maps.getScale(), player, slice);

            int playX = player.blockPosition().getX();
            int playZ = player.blockPosition().getZ();
            byte scale = maps.getScale();
            int scaleWidth = (1 << scale) * 128;
            Set<Vector2i> discoveringEdges = getPlayerDiscoveringMapEdges(
                    activeKey.mapX(),
                    activeKey.mapZ(),
                    scaleWidth,
                    playX,
                    playZ,
                    slice.getDiscoveryReach()
            );

            // Update Map states & colors
            //these also include an active map
            List<MapDataHolder> nearbyExistentMaps =
                    maps.filterSection(level.dimension(), slice, e -> discoveringEdges.stream()
                            .anyMatch(edge -> edge.x == e.centerX
                                    && edge.y == e.centerZ));

            MapDataHolder activeInfo = maps.select(activeKey);
            if (activeInfo == null) {
                // no map. we try creating a new one for this dimension
                maybeCreateNewMapEntry(player, atlas, maps, slice, Mth.floor(player.getX()), Mth.floor(player.getZ()));
            }

            // updateColors is *easily* the most expensive function in the entire server tick
            // As a result, we will only ever call updateColors twice per tick (same as vanilla's limit)
            if (!nearbyExistentMaps.isEmpty()) {
                MapDataHolder selected;
                if (MapAtlasesConfig.roundRobinUpdate.get()) {
                    selected = nearbyExistentMaps.get(server.getTickCount() % nearbyExistentMaps.size());
                    selected.updateMap(player);

                } else {
                    for (int j = 0; j < MapAtlasesConfig.mapUpdatePerTick.get(); j++) {
                        selected = getMapToUpdate(nearbyExistentMaps, player);
                        selected.updateMap(player);
                    }
                }
            }
            // update center one too but not each tick
            if (activeInfo != null && isTimeToUpdate(activeInfo.data, player, slice, 5, 20)) {
                activeInfo.updateMap(player);
            }
            if (activeInfo != null) nearbyExistentMaps.add(activeInfo);

            //TODO: old code called this for all maps. Isnt it enough to just call for the visible ones?
            // this also update banners and decorations so wen dont want to update stuff we cant see
            for (var mapInfo : nearbyExistentMaps) {
                MapAtlasesAccessUtils.updateMapDataAndSync(mapInfo, player, atlas);
                //if data has changed, a packet will be sent
            }

            // Create new Map entries
            if (!MapAtlasesConfig.enableEmptyMapEntryAndFill.get() ||
                    MapAtlasItem.isLocked(atlas)) return;

            //TODO : this isnt accurate and can be improved
            if (isPlayerTooFarAway(activeKey, player, scaleWidth)) {
                maybeCreateNewMapEntry(player, atlas, maps, slice, Mth.floor(player.getX()),
                        Mth.floor(player.getZ()));
            }
            //remove existing maps and tries to fill in remaining nones
            discoveringEdges.removeIf(e -> nearbyExistentMaps.stream().anyMatch(
                    d -> d.data.centerX == e.x && d.data.centerZ == e.y));
            for (var edge : discoveringEdges) {
                maybeCreateNewMapEntry(player, atlas, maps, slice, edge.x, edge.y);
            }

        }
    }

    //checks if pixel of this map has been filled at this position with random offset
    private static boolean isTimeToUpdate(MapItemSavedData data, Player player,
                                          Slice slice, int min, int max) {
        int i = 1 << data.scale;
        int range;
        if (slice != null && MapAtlasesMod.SUPPLEMENTARIES) {
            range = (SupplementariesCompat.getSliceReach() / i);
        } else {
            range = 128 / i;
        }
        Level level = player.level();
        int rx = level.random.nextIntBetweenInclusive(-range, range);
        int rz = level.random.nextIntBetweenInclusive(-range, range);
        int x = (int) Mth.clamp((player.getX() + rx - data.centerX) / i + 64, 0, 127);
        int z = (int) Mth.clamp((player.getZ() + rz - data.centerZ) / i + 64, 0, 127);
        boolean filled = data.colors[x + z * 128] != 0;

        int interval = filled ? max : min;

        return level.getGameTime() % interval == 0;
    }

    private static MapDataHolder getMapToUpdate(List<MapDataHolder> nearbyExistentMaps, ServerPlayer player) {
        var m = updateQueue.computeIfAbsent(player, a -> new HashMap<>());
        Set<String> nearbyIds = new HashSet<>();
        for (var holder : nearbyExistentMaps) {
            nearbyIds.add(holder.stringId);
            m.computeIfAbsent(holder.stringId, a -> new MapUpdateTicket(holder));
        }
        int px = player.getBlockX();
        int pz = player.getBlockZ();
        var it = m.entrySet().iterator();
        while (it.hasNext()) {
            var t = it.next();
            if (!nearbyIds.contains(t.getKey())) {
                it.remove();
            } else t.getValue().updatePriority(px, pz);
        }
        MapUpdateTicket selected = m.values().stream().max(MapUpdateTicket.COMPARATOR).orElseThrow();
        selected.waitTime = 0;
        return selected.holder;
    }


    public static boolean isPlayerTooFarAway(
            MapKey key,
            Player player, int width
    ) {
        return Mth.square(key.mapX() - player.getX()) + Mth.square(key.mapZ() - player.getZ()) > width * width;
    }

    //TODO: optimize
    private static void maybeCreateNewMapEntry(
            ServerPlayer player,
            ItemStack atlas,
            MapCollectionCap maps,
            Slice slice,
            int destX,
            int destZ
    ) {
        Level level = player.level();
        if (atlas.getTag() == null) {
            // If the Atlas is "inactive", give it a pity Empty Map count
            MapAtlasItem.setEmptyMaps(atlas, MapAtlasesConfig.pityActivationMapCount.get());
        }

        int emptyCount = MapAtlasItem.getEmptyMaps(atlas);
        boolean bypassEmptyMaps = !MapAtlasesConfig.requireEmptyMapsToExpand.get();
        boolean addedMap = false;
        if (!mutex.isLocked() && (emptyCount > 0 || player.isCreative() || bypassEmptyMaps)) {
            mutex.lock();

            // Make the new map
            if (!player.isCreative() && !bypassEmptyMaps) {
                //remove 1 map
                MapAtlasItem.increaseEmptyMaps(atlas, -1);
            }
            //validate height
            Integer height = slice.height();
            if (height != null && !maps.getHeightTree(player.level().dimension(), slice.type()).contains(height)) {
                int error = 1;
            }

            byte scale = maps.getScale();

            //TODO: create custom ones

            ItemStack newMap = slice.createNewMap(destX, destZ, scale, player.level());
            Integer mapId = MapItem.getMapId(newMap);

            if (mapId != null) {
                MapDataHolder newData = MapDataHolder.findFromId(level, mapId);
                // for custom map data to be sent immediately... crappy and hacky. TODO: change custom map data impl
                if (newData != null) {
                    MapAtlasesAccessUtils.updateMapDataAndSync(newData, player, newMap);
                }
                addedMap = maps.add(mapId, level);
            }
            mutex.unlock();
        }

        if (addedMap) {
            // Play the sound
            player.level().playSound(null, player.blockPosition(),
                    MapAtlasesMod.ATLAS_CREATE_MAP_SOUND_EVENT.get(),
                    SoundSource.PLAYERS, (float) (double) MapAtlasesClientConfig.soundScalar.get(), 1.0F);
        }
    }

    private static Set<Vector2i> getPlayerDiscoveringMapEdges(
            int xCenter,
            int zCenter,
            int width,
            int xPlayer,
            int zPlayer,
            int reach) {


        int halfWidth = width / 2;
        Set<Vector2i> results = new HashSet<>();
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                if (i != 0 || j != 0) {
                    int qI = xCenter;
                    int qJ = zCenter;
                    if (i == -1 && xPlayer - reach <= xCenter - halfWidth) {
                        qI -= width;
                    } else if (i == 1 && xPlayer + reach >= xCenter + halfWidth) {
                        qI += width;
                    }
                    if (j == -1 && zPlayer - reach <= zCenter - halfWidth) {
                        qJ -= width;
                    } else if (j == 1 && zPlayer + reach >= zCenter + halfWidth) {
                        qJ += width;
                    }
                    // does not add duplicates this way
                    if (!(qI == xCenter && qJ == zCenter)) {
                        results.add(new Vector2i(qI, qJ));
                    }
                }
            }
        }
        return results;
    }


    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && MapAtlasesMod.MOONLIGHT) {
            MapAtlasesNetworking.sendToClientPlayer(sp, new S2CWorldHashPacket(sp));
        }
    }
}