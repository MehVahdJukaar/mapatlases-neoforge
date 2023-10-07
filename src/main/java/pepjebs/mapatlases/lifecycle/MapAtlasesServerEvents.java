package pepjebs.mapatlases.lifecycle;

import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.loot.functions.ExplorationMapFunction;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.capabilities.MapCollectionCap;
import pepjebs.mapatlases.capabilities.MapKey;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.SupplementariesCompat;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.networking.MapAtlasesNetowrking;
import pepjebs.mapatlases.networking.S2CSetMapDataPacket;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.Slice;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class MapAtlasesServerEvents {

    // Used to prevent Map creation spam consuming all Empty Maps on auto-create
    private static final ReentrantLock mutex = new ReentrantLock();

    // Holds the current MapItemSavedData ID for each player
    @Deprecated(forRemoval = true)
    private static final WeakHashMap<Player, String> playerToActiveMapId = new WeakHashMap<>();
    private static final WeakHashMap<Player, HashMap<MapItemSavedData, MapUpdateTicket>> queues = new WeakHashMap<>();

    private static class MapUpdateTicket {
        private static final Comparator<MapUpdateTicket> COMPARATOR = Comparator.comparingDouble(MapUpdateTicket::getPriority);

        private final MapItemSavedData data;
        private int waitTime = 20; //set to 0 when this is updated. if not incremented each tick. we start with lowest for newly added entries
        private double lastDistance = 1000000;
        private double currentPriority; //bigger the better
    }

    @SubscribeEvent
    public static void mapAtlasPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            mapAtlasPlayerJoinImpl(serverPlayer);
        }
    }

    public static void mapAtlasPlayerJoinImpl(ServerPlayer player) {
        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
        if (atlas.isEmpty()) return;
        //we need to send all data for all dimensions as they are not sent automatically
        var mapInfos = MapAtlasItem.getMaps(atlas, player.level).getAll();
        for (var info : mapInfos) {
            String mapId = info.getFirst();
            MapItemSavedData state = info.getSecond();
            state.tickCarriedBy(player, atlas);
            state.getHoldingPlayer(player);

            MapAtlasesNetowrking.sendToClientPlayer(player, new S2CSetMapDataPacket(mapId, state, true));

        }
    }

    @SubscribeEvent
    public static void mapAtlasesPlayerTick(TickEvent.PlayerTickEvent event) {
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
            Level level = player.level;
            MapCollectionCap maps = MapAtlasItem.getMaps(atlas, level);
            Slice slice = MapAtlasItem.getSelectedSlice(atlas, level.dimension());

            // sets new center map

            MapKey activeKey = MapKey.at(maps.getScale(), player, slice);

            int playX = player.blockPosition().getX();
            int playZ = player.blockPosition().getZ();
            byte scale = maps.getScale();
            int scaleWidth = (1 << scale) * 128;
            Set<Vec2> discoveringEdges = getPlayerDiscoveringMapEdges(
                    activeKey.mapX(),
                    activeKey.mapZ(),
                    scaleWidth,
                    playX,
                    playZ,
                    slice.getDiscoveryReach()
            );

            // Update Map states & colors
            //these also include active map
            List<Pair<String, MapItemSavedData>> nearbyExistentMaps =
                    maps.filterSection(level.dimension(), slice, e -> discoveringEdges.stream()
                            .anyMatch(edge -> edge.x == e.x
                                    && edge.y == e.z));

            Pair<String, MapItemSavedData> activeInfo = maps.select(activeKey);
            if (activeInfo == null) {
                // no map. we try creating a new one for this dimension
                maybeCreateNewMapEntry(player, atlas, Mth.floor(player.getX()),
                        Mth.floor(player.getZ()));
            }else nearbyExistentMaps.add(activeInfo);

            //TODO: old code called this for all maps. Isnt it enough to just call for the visible ones?
            //this also update banners and decorations so wen dont want to update stuff we cant see
            for (var mapInfo : nearbyExistentMaps) {
                updateMapDataForPlayer(mapInfo, player, atlas);
            }

            // updateColors is *easily* the most expensive function in the entire server tick
            // As a result, we will only ever call updateColors twice per tick (same as vanilla's limit)
            if (!nearbyExistentMaps.isEmpty()) {
                var selected = nearbyExistentMaps.get(server.getTickCount() % nearbyExistentMaps.size());
                updateMapColorsForPlayer(selected.getSecond(), player);
                //TODO: update active one more frequently
            }

            // Create new Map entries
            if (!MapAtlasesConfig.enableEmptyMapEntryAndFill.get() ||
                    MapAtlasItem.isLocked(atlas)) return;

            //TODO : this isnt accurate and can be improved
            if (isPlayerTooFarAway(activeKey, player, scaleWidth)) {
                maybeCreateNewMapEntry(player, atlas, maps ,slice, Mth.floor(player.getX()),
                        Mth.floor(player.getZ()));
            }
            //remove existing maps and tries to fill in remaining nones
            discoveringEdges.removeIf(e -> nearbyExistentMaps.stream().anyMatch(
                    d -> d.getSecond().x == e.x && d.getSecond().z == e.y));
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
            range =  (SupplementariesCompat.getSliceReach() / i);
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

    private static MapItemSavedData getMapToUpdate(List<Pair<String, MapItemSavedData>> nearbyExistentMaps, ServerPlayer player) {
        var m = queues.computeIfAbsent(player, a -> new HashMap<>());
        Set<MapItemSavedData> existing = new HashSet<>();
        for (var v : nearbyExistentMaps) {
            var d = v.getSecond();
            existing.add(d);
            m.computeIfAbsent(d, a -> new MapUpdateTicket(d));
        }
        int px = player.getBlockX();
        int pz = player.getBlockZ();
        var it = m.entrySet().iterator();
        while (it.hasNext()) {
            var t = it.next();
            if (!existing.contains(t.getKey())) {
                it.remove();
            } else t.getValue().updatePriority(px, pz);
        }
        MapUpdateTicket selected = m.values().stream().max(MapUpdateTicket.COMPARATOR).orElseThrow();
        selected.waitTime = 0;
        return selected.data;
    }


    public static boolean isPlayerTooFarAway(
            MapKey key,
            Player player, int width
    ) {
        return Mth.square(key.mapX() - player.getX()) + Mth.square(key.mapZ() - player.getZ()) > width * width;
    }

    private static void updateMapDataForPlayer(
            Pair<String, MapItemSavedData> mapInfo,
            ServerPlayer player,
            ItemStack atlas
    ) {
        mapInfo.getSecond().tickCarriedBy(player, atlas);
        relayMapItemSavedDataSyncToPlayerClient(mapInfo, player);
    }

    private static void updateMapColorsForPlayer(
            MapItemSavedData state,
            ServerPlayer player) {
        ((MapItem) Items.FILLED_MAP).update(player.level, player, state);
    }

    public static void relayMapItemSavedDataSyncToPlayerClient(
            Pair<String, MapItemSavedData> mapInfo,
            ServerPlayer player
    ) {
        int mapId = MapAtlasesAccessUtils.getMapIntFromString(mapInfo.getFirst());
        //TODO make better

        Packet<?> p = null;
        int tries = 0;
        while (p == null && tries < 1) {
            //we sent at vanilla rate. when new is created we sent packet immediately (not here)
            //WHY??
            p = mapInfo.getSecond().getUpdatePacket(mapId, player);
            tries++;
        }
        if (p != null) {
            //TODO: maybe use isComplex  update packet and inventory tick
            player.connection.send(p);
        }
    }

    private static String relayActiveMapIdToPlayerClient(
            Pair<String, MapItemSavedData> activeInfo,
            ServerPlayer player
    ) {
        String changedMapItemSavedData = null;
        String cachedMapId = playerToActiveMapId.get(player);
        if (activeInfo != null) {
            boolean addingPlayer = cachedMapId == null;
            // Players that pick up an atlas will need their MapItemSavedDatas initialized
            if (addingPlayer) {
                mapAtlasPlayerJoinImpl(player);
            }
            String currentMapId = activeInfo.getFirst();
            if (addingPlayer || !currentMapId.equals(cachedMapId)) {
                changedMapItemSavedData = cachedMapId;
                playerToActiveMapId.put(player, currentMapId);
                //   MapAtlasesNetowrking.sendToClientPlayer(player, new S2CSetActiveMapPacket(currentMapId));
            }
        } else if (cachedMapId != null) {
            playerToActiveMapId.put(player, null);

            // MapAtlasesNetowrking.sendToClientPlayer(player, new S2CSetActiveMapPacket("null"));
        }
        return changedMapItemSavedData;
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
        Level level = player.level;
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
            if (height != null && !maps.getHeightTree(player.level.dimension(), slice.type()).contains(height)) {
                int error = 1;
            }

            byte scale = maps.getScale();

            //TODO: create custom ones

            ItemStack newMap = slice.createNewMap(destX, destZ, scale, player.level());
            Integer mapId = MapItem.getMapId(newMap);

            if (mapId != null) {
                var newData = MapAtlasesAccessUtils.findMapFromId(level,mapId);
                // for custom map data to be sent immediately... crappy and hacky. TODO: change custom map data impl
                if (newData != null) {
                    MapAtlasesAccessUtils.updateMapDataAndSync(newData.getSecond(), mapId, player, newMap);
                }
                addedMap = maps.add(mapId, level);
            }
            mutex.unlock();
        }

        if (addedMap) {
            // Play the sound
            player.level.playSound(null, player.blockPosition(),
                    MapAtlasesMod.ATLAS_CREATE_MAP_SOUND_EVENT.get(),
                    SoundSource.PLAYERS, (float) (double) MapAtlasesClientConfig.soundScalar.get(), 1.0F);
        }
    }

    private static Set<Vec2> getPlayerDiscoveringMapEdges(
            int xCenter,
            int zCenter,
            int width,
            int xPlayer,
            int zPlayer,
           int reach) {


        int halfWidth = width / 2;
        Set<Vec2> results = new HashSet<>();
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
                    results.add(new Vec2(qI, qJ));
                }
            }
        }
        return results;
    }
}