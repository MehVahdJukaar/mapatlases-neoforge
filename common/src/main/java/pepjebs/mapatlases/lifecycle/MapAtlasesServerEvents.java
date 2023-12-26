package pepjebs.mapatlases.lifecycle;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.SupplementariesCompat;
import pepjebs.mapatlases.integration.moonlight.EntityRadar;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.IMapCollection;
import pepjebs.mapatlases.map_collection.MapKey;
import pepjebs.mapatlases.networking.MapAtlasesNetworking;
import pepjebs.mapatlases.networking.S2CWorldHashPacket;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.MapType;
import pepjebs.mapatlases.utils.Slice;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class MapAtlasesServerEvents {

    // Used to prevent Map creation spam consuming all Empty Maps on auto-create
    private static final ReentrantLock mutex = new ReentrantLock();

    private static final WeakHashMap<Player, Tuple<Float, HashMap<String, MapUpdateTicket>>> updateQueue = new WeakHashMap<>();
    private static final WeakHashMap<Player, MapDataHolder> lastMapData = new WeakHashMap<>();

    //TODO: improve . lower updates when stationary
    private static class MapUpdateTicket {
        private static final Comparator<MapUpdateTicket> COMPARATOR = Comparator.comparingDouble(MapUpdateTicket::getPriority);

        private final MapDataHolder holder;
        private int waitTime = 20; //set to zero when this is updated.
        // if not incremented, each tick.
        // we start with lowest for newly added entries
        private double lastDistance = 1000000;
        private double currentPriority; //bigger the better
        private boolean hasBlankPixels = true;
        private int lastI = 0;
        private int lastJ = 0;
        private final float lowUpdateWeight;

        private MapUpdateTicket(MapDataHolder data) {
            this.holder = data;
            this.updateHasBlankPixels();
            if (data.type == MapType.VANILLA && data.slice.height() != null) {
                hasBlankPixels = false; //hack since these can have blank pixels when populated
                lowUpdateWeight = 0.6f;
            } else lowUpdateWeight = 0.15f;
        }

        public double getPriority() {
            return hasBlankPixels ? currentPriority : currentPriority * 0.15f;
        }

        public void updatePriority(int px, int pz) {
            this.waitTime++;
            double distSquared = Mth.lengthSquared(px - holder.data.centerX, pz - holder.data.centerZ);
            // Define weights for distance and waitTime
            double movingDistanceWeight = 10; // Adjust this based on your preference
            double staticDistanceWeight = 10000; // Adjust this based on your preference
            double waitTimeWeight = 1; // Adjust this based on your preference

            // Calculate the priority using a weighted sum
            double deltaDist = (lastDistance - distSquared); //for maps getting closer
            this.currentPriority = (movingDistanceWeight * deltaDist) + (waitTimeWeight * this.waitTime * this.waitTime) + (staticDistanceWeight * Mth.fastInvSqrt(distSquared));
            this.lastDistance = distSquared;
        }

        public void updateHasBlankPixels() {
            if (hasBlankPixels) {
                for (lastI = 0; lastI < 128; lastI++) {
                    int k = lastI % 2 == 0 ? lastI : 127 - lastI;
                    for (lastJ = 0; lastJ < 128; lastJ++) {
                        int m = lastJ % 2 == 0 ? lastJ : 127 - lastJ;
                        if (this.holder.data.colors[k + m * 128] == 0) {
                            //for slice maps...
                            return;
                        }
                    }
                }
                hasBlankPixels = false;
            }
        }

        public float getUpdateFrequencyWeight() {
            return hasBlankPixels ? 1 : lowUpdateWeight;
        }
    }

    public static void onPlayerTick(Player p) {
        ServerPlayer player = ((ServerPlayer) p);
        //not needed?
        //if (player.isRemoved() || player.isChangingDimension() || player.hasDisconnected()) continue;

        var server = player.server;
        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
        if (atlas.isEmpty()) return;

        Level level = player.level();
        ResourceKey<Level> dimension = level.dimension();
        IMapCollection maps = MapAtlasItem.getMaps(atlas, level);

        Slice slice = MapAtlasItem.getSelectedSlice(atlas, dimension);
        // sets new center map
        MapKey activeKey = MapKey.at(maps.getScale(), player, slice);

        //sync the slice below and above so we can update slice automatically
        if ((level.getGameTime() + 13) % 40 == 0) {
            sendSlicesAboveAndBelow(player, atlas, maps, activeKey);
        }

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
                maps.filterSection(slice, e -> discoveringEdges.stream()
                        .anyMatch(edge -> edge.x == e.centerX
                                && edge.y == e.centerZ));

        MapDataHolder activeInfo = maps.select(activeKey);
        if (activeInfo == null && !MapAtlasItem.isLocked(atlas)) {
            // no map. we try creating a new one for this dimension
            maybeCreateNewMapEntry(player, atlas, maps, slice, Mth.floor(player.getX()), Mth.floor(player.getZ()));
            activeInfo = maps.select(activeKey);
        }

        // adds center map
        if (activeInfo != null) nearbyExistentMaps.add(activeInfo);
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
                    if (selected != null) selected.updateMap(player);
                }
            }
        }


        //TODO: old code called this for all maps. Isnt it enough to just call for the visible ones?
        // this also update banners and decorations so wen dont want to update stuff we cant see
        for (var mapInfo : nearbyExistentMaps) {
            MapAtlasesAccessUtils.updateMapDataAndSync(mapInfo, player, atlas, InteractionResult.SUCCESS);
            //if data has changed, a packet will be sent
        }
        // for far away maps so we remove player marker
        MapDataHolder lastData = lastMapData.get(player);
        if (lastData != null && !nearbyExistentMaps.contains(lastData)) {
            MapAtlasesAccessUtils.updateMapDataAndSync(lastData, player, atlas, InteractionResult.FAIL);
        }
        lastMapData.put(player, activeInfo);

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

    private static void sendSlicesAboveAndBelow(ServerPlayer player, ItemStack atlas, IMapCollection maps, MapKey activeKey) {
        Slice slice = activeKey.slice();
        var dimension = activeKey.slice().dimension();
        var tree = maps.getHeightTree(dimension, slice.type());
        for (Integer hh : tree) {
            if (hh != slice.heightOrTop()) {
                var below = maps.select(activeKey.mapX(), activeKey.mapZ(), Slice.of(slice.type(), hh, dimension));
                if (below != null)
                    MapAtlasesAccessUtils.updateMapDataAndSync(below, player, atlas, InteractionResult.SUCCESS);
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

    @Nullable
    private static MapDataHolder getMapToUpdate(List<MapDataHolder> nearbyExistentMaps, ServerPlayer player) {
        var tup = updateQueue.computeIfAbsent(player, a -> new Tuple<>(0f, new HashMap<>()));
        var mapsToUpdate = tup.getB();
        Set<String> nearbyIds = new HashSet<>();
        for (var holder : nearbyExistentMaps) {
            nearbyIds.add(holder.stringId);
            mapsToUpdate.computeIfAbsent(holder.stringId, a -> new MapUpdateTicket(holder));
        }
        int px = player.getBlockX();
        int pz = player.getBlockZ();
        var iterator = mapsToUpdate.entrySet().iterator();
        //remove invalid tickets and update their priority
        float totalWeight = 0;
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (!nearbyIds.contains(entry.getKey())) {
                iterator.remove();
            } else {
                MapUpdateTicket ticket = entry.getValue();
                ticket.updatePriority(px, pz);
                totalWeight += ticket.getUpdateFrequencyWeight();
            }
        }
        float callsPerTick = totalWeight / 9; // default with nine empty maps around
        float counter = tup.getA() + callsPerTick;
        boolean shouldUpdate = false;
        if (counter >= 1) {
            shouldUpdate = true;
            counter -= 1;
        }
        tup.setA(counter);

        if (shouldUpdate) {
            MapUpdateTicket selected = mapsToUpdate.values().stream().max(MapUpdateTicket.COMPARATOR).orElseThrow();
            selected.waitTime = 0;
            selected.updateHasBlankPixels();
            return selected.holder;
        }
        return null;
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
            IMapCollection maps,
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

            ItemStack newMap = slice.createNewMap(destX, destZ, scale, player.level(), atlas);
            Integer mapId = MapItem.getMapId(newMap);

            if (mapId != null) {
                MapDataHolder newData = MapDataHolder.findFromId(level, mapId);
                // for custom map data to be sent immediately... crappy and hacky. TODO: change custom map data impl
                if (newData != null) {
                    MapAtlasesAccessUtils.updateMapDataAndSync(newData, player, newMap, InteractionResult.SUCCESS);
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


    public static void onPlayerJoin(ServerPlayer sp) {
        if (MapAtlasesMod.MOONLIGHT) {
            MapAtlasesNetworking.CHANNEL.sendToClientPlayer(sp, new S2CWorldHashPacket(sp));
        }
        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(sp);
        if (atlas.isEmpty()) return;

        Level level = sp.level();
        ResourceKey<Level> dimension = level.dimension();
        IMapCollection maps = MapAtlasItem.getMaps(atlas, level);

        Slice slice = MapAtlasItem.getSelectedSlice(atlas, dimension);
        // sets new center map
        MapKey activeKey = MapKey.at(maps.getScale(), sp, slice);
        sendSlicesAboveAndBelow(sp, atlas, maps, activeKey);
    }


    public static void onDimensionUnload() {
        if (MapAtlasesMod.MOONLIGHT) EntityRadar.unloadLevel();
    }

}