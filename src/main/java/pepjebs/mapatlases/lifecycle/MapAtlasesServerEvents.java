package pepjebs.mapatlases.lifecycle;

import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.capabilities.MapCollectionCap;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.networking.MapAtlasesNetowrking;
import pepjebs.mapatlases.networking.S2CSetActiveMapPacket;
import pepjebs.mapatlases.networking.S2CSetMapDataPacket;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtilsOld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class MapAtlasesServerEvents {

    // Used to prevent Map creation spam consuming all Empty Maps on auto-create
    private static final ReentrantLock mutex = new ReentrantLock();

    // Holds the current MapItemSavedData ID for each player
    //maybe use weakhasmap with plauer
    private static final Map<String, String> playerToActiveMapId = new HashMap<>();


    @SubscribeEvent
    public static void mapAtlasPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            mapAtlasPlayerJoinImpl(serverPlayer);
        }
    }

    public static void mapAtlasPlayerJoinImpl(ServerPlayer player) {
        ItemStack atlas = MapAtlasesAccessUtilsOld.getAtlasFromPlayerByConfig(player);
        if (atlas.isEmpty()) return;
        //we need to send all data for all dimensions as they are not sent automatically
        var mapInfos = MapAtlasItem.getMaps(atlas, player.level()).getAll();
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
        if (event.side == LogicalSide.SERVER) {
            ServerPlayer player = ((ServerPlayer) event.player);
            var server = player.server;
            ItemStack atlas = MapAtlasesAccessUtilsOld.getAtlasFromPlayerByConfig(player);
            if (atlas.isEmpty()) return;
            MapCollectionCap maps = MapAtlasItem.getMaps(atlas, player.level());
            Integer slice = MapAtlasItem.getSelectedSlice(atlas);

            // sets new center map
            Pair<String, MapItemSavedData> activeInfo = getMapAtPositionOrClosest(player, maps, slice);
            maps.setActive(activeInfo);
            //TODO: improve
            if (activeInfo == null) return;

            MapItemSavedData activeState = activeInfo.getSecond();

            int playX = player.blockPosition().getX();
            int playZ = player.blockPosition().getZ();
            byte scale = activeState.scale;
            int scaleWidth = (1 << scale) * 128;
            ArrayList<Pair<Integer, Integer>> discoveringEdges = getPlayerDiscoveringMapEdges(
                    activeState.centerX,
                    activeState.centerZ,
                    scaleWidth,
                    playX,
                    playZ
            );

            // Update Map states & colors

            List<Pair<String, MapItemSavedData>> nearbyExistentMaps = maps.getAll().stream()
                    .filter(e -> discoveringEdges.stream()
                            .anyMatch(edge -> edge.getFirst() == e.getSecond().centerX
                                    && edge.getSecond() == e.getSecond().centerZ))
                    .toList();
            //TODO: old code called this for all maps. Isnt it enough to just call for the visible ones?
            //this also update banners and decorations so wen dont want to update stuff we cant see
            for (var mapInfo : nearbyExistentMaps) {
                updateMapDataForPlayer(mapInfo, player, atlas);
            }
            updateMapDataForPlayer(activeInfo, player, atlas);

            // updateColors is *easily* the most expensive function in the entire server tick
            // As a result, we will only ever call updateColors twice per tick (same as vanilla's limit)
            if (!nearbyExistentMaps.isEmpty()) {
                var selected = nearbyExistentMaps.get(server.getTickCount() % nearbyExistentMaps.size());
                updateMapColorsForPlayer(selected.getSecond(), player);
            }
            updateMapColorsForPlayer(activeState, player);


            // Create new Map entries
            if (!MapAtlasesConfig.enableEmptyMapEntryAndFill.get()) return;

            //TODO : this isnt accurate and can be improved
            if (isPlayerTooFarAway(activeState, player, scaleWidth)) {
                maybeCreateNewMapEntry(player, atlas, scale, Mth.floor(player.getX()),
                        Mth.floor(player.getZ()));
            }
            //remove existing maps and tries to fill in remaining nones
            discoveringEdges.removeIf(e -> nearbyExistentMaps.stream().anyMatch(
                    d -> d.getSecond().centerX == e.getFirst() && d.getSecond().centerZ == e.getSecond()));
            for (var edge : discoveringEdges) {
                maybeCreateNewMapEntry(player, atlas, scale, edge.getFirst(), edge.getSecond());
            }

        }
    }

    public static boolean isPlayerTooFarAway(
            MapItemSavedData mapState,
            Player player, int width
    ) {
        return Mth.square(mapState.centerX - player.getX()) + Mth.square(mapState.centerZ - player.getZ()) > width * width;
    }

    @SubscribeEvent
    public static void mapAtlasServerTick(TickEvent.ServerTickEvent event) {
        if (true) return;
        ArrayList<String> seenPlayers = new ArrayList<>();
        MinecraftServer server = event.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String playerName = player.getName().getString();
            seenPlayers.add(playerName);
            //why isnt all this in client tick?
            if (player.isRemoved() || player.isChangingDimension() || player.hasDisconnected()) continue;
            ItemStack atlas = MapAtlasesAccessUtilsOld.getAtlasFromPlayerByConfig(player);
            if (atlas.isEmpty()) continue;

            //gets closest data
            MapCollectionCap maps = MapAtlasItem.getMaps(atlas, player.level());
            Pair<String, MapItemSavedData> activeInfo = getMapAtPositionOrClosest(player, maps, MapAtlasItem.getSelectedSlice(atlas));

            // changedMapItemSavedData has non-null value if player has a new active Map ID
            String changedMapItemSavedData = relayActiveMapIdToPlayerClient(activeInfo, player);
            if (activeInfo == null) {
                maybeCreateNewMapEntry(player, atlas, (byte) 0, Mth.floor(player.getX()),
                        Mth.floor(player.getZ()));
                continue;
            }
            MapItemSavedData activeState = activeInfo.getSecond();

            int playX = player.blockPosition().getX();
            int playZ = player.blockPosition().getZ();
            byte scale = activeState.scale;
            int scaleWidth = (1 << scale) * 128;
            ArrayList<Pair<Integer, Integer>> discoveringEdges = getPlayerDiscoveringMapEdges(
                    activeState.centerX,
                    activeState.centerZ,
                    scaleWidth,
                    playX,
                    playZ
            );

            // Update Map states & colors
            // updateColors is *easily* the most expensive function in the entire server tick
            // As a result, we will only ever call updateColors twice per tick (same as vanilla's limit)
            Map<String, MapItemSavedData> nearbyExistentMaps = maps.getAll().stream()
                    .filter(e -> discoveringEdges.stream()
                            .anyMatch(edge -> edge.getFirst() == e.getSecond().centerX
                                    && edge.getSecond() == e.getSecond().centerZ))
                    .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
            for (var mapInfo : maps.getAll()) {
                updateMapDataForPlayer(mapInfo, player, atlas);
            }
            updateMapColorsForPlayer(activeState, player);
            if (!nearbyExistentMaps.isEmpty()) {
                updateMapColorsForPlayer(
                        (MapItemSavedData) nearbyExistentMaps.values().toArray()[server.getTickCount() % nearbyExistentMaps.size()],
                        player);
            }

            // Create new Map entries
            if (!MapAtlasesConfig.enableEmptyMapEntryAndFill.get()) continue;
            boolean isPlayerOutsideAllMapRegions = MapAtlasesAccessUtilsOld.distanceBetweenMapItemSavedDataAndPlayer(
                    activeState, player) > scaleWidth;
            if (isPlayerOutsideAllMapRegions) {
                maybeCreateNewMapEntry(player, atlas, scale, Mth.floor(player.getX()),
                        Mth.floor(player.getZ()));
            }
            discoveringEdges.removeIf(e -> nearbyExistentMaps.values().stream().anyMatch(
                    d -> d.centerX == e.getFirst() && d.centerZ == e.getSecond()));
            for (var p : discoveringEdges) {
                maybeCreateNewMapEntry(player, atlas, scale, p.getFirst(), p.getSecond());
            }
        }
        // Clean up disconnected players in server tick
        // since when using Disconnect event, the tick will sometimes
        // re-add the Player after they disconnect
        playerToActiveMapId.keySet().removeIf(playerName -> !seenPlayers.contains(playerName));
    }

    @Nullable
    private static Pair<String, MapItemSavedData> getMapAtPositionOrClosest(
            ServerPlayer player, MapCollectionCap maps, @Nullable Integer slice) {
        byte scale = maps.getScale();
        double px = player.getX();
        double pz = player.getZ();
        //map code
        int i = 128 * (1 << scale);
        int j = Mth.floor((px + 64.0D) / i);
        int k = Mth.floor((pz + 64.0D) / i);
        int mapCenterX = j * i + i / 2 - 64;
        int mapCenterZ = k * i + i / 2 - 64;
        var mapAt = maps.select(mapCenterX, mapCenterZ, player.level().dimension(), slice);
        if (mapAt != null) return mapAt;
        else return maps.getClosest(player, slice);
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
        ((MapItem) Items.FILLED_MAP).update(player.level(), player, state);
    }

    public static void relayMapItemSavedDataSyncToPlayerClient(
            Pair<String, MapItemSavedData> mapInfo,
            ServerPlayer player
    ) {
        int mapId = MapAtlasesAccessUtils.getMapIntFromString(mapInfo.getFirst());
        //TODO make better

        Packet<?> p = null;
        int tries = 0;
        while (p == null && tries < 10) {
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
        String playerName = player.getName().getString();
        String changedMapItemSavedData = null;
        String cachedMapId = playerToActiveMapId.get(playerName);
        if (activeInfo != null) {
            boolean addingPlayer = cachedMapId == null;
            // Players that pick up an atlas will need their MapItemSavedDatas initialized
            if (addingPlayer) {
                mapAtlasPlayerJoinImpl(player);
            }
            String currentMapId = activeInfo.getFirst();
            if (addingPlayer || !currentMapId.equals(cachedMapId)) {
                changedMapItemSavedData = cachedMapId;
                playerToActiveMapId.put(playerName, currentMapId);
                MapAtlasesNetowrking.sendToClientPlayer(player, new S2CSetActiveMapPacket(currentMapId));
            }
        } else if (cachedMapId != null) {
            playerToActiveMapId.put(playerName, null);

            MapAtlasesNetowrking.sendToClientPlayer(player, new S2CSetActiveMapPacket("null"));
        }
        return changedMapItemSavedData;
    }

    private static void maybeCreateNewMapEntry(
            ServerPlayer player,
            ItemStack atlas,
            byte scale,
            int destX,
            int destZ
    ) {
        Level level = player.level();
        if (atlas.getTag() == null) {
            // If the Atlas is "inactive", give it a pity Empty Map count
            MapAtlasItem.setEmptyMaps(atlas, MapAtlasesConfig.pityActivationMapCount.get());
        }
        MapCollectionCap maps = MapAtlasItem.getMaps(atlas, level);

        int emptyCount = MapAtlasItem.getEmptyMaps(atlas);
        boolean bypassEmptyMaps = !MapAtlasesConfig.requireEmptyMapsToExpand.get();
        if (!mutex.isLocked() && (emptyCount > 0 || player.isCreative() || bypassEmptyMaps)) {
            mutex.lock();

            // Make the new map
            if (!player.isCreative() && !bypassEmptyMaps) {
                //remove 1 map
                MapAtlasItem.increaseEmptyMaps(atlas, -1);
            }
            ItemStack newMap = MapItem.create(
                    player.level(),
                    destX,
                    destZ,
                    scale,
                    true,
                    false);

            Integer mapId = MapItem.getMapId(newMap);
            if (mapId != null) {
                maps.add(mapId, level);

                // Play the sound
                player.level().playSound(null, player.blockPosition(),
                        MapAtlasesMod.ATLAS_CREATE_MAP_SOUND_EVENT.get(),
                        SoundSource.PLAYERS, (float) (double) MapAtlasesClientConfig.soundScalar.get(), 1.0F);
            }
            mutex.unlock();
        }
    }

    private static ArrayList<Pair<Integer, Integer>> getPlayerDiscoveringMapEdges(
            int xCenter,
            int zCenter,
            int width,
            int xPlayer,
            int zPlayer) {
        int halfWidth = width / 2;
        ArrayList<Pair<Integer, Integer>> results = new ArrayList<>();
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                if (i != 0 || j != 0) {
                    int qI = xCenter;
                    int qJ = zCenter;
                    if (i == -1 && xPlayer - 128 < xCenter - halfWidth) {
                        qI -= width;
                    } else if (i == 1 && xPlayer + 128 > xCenter + halfWidth) {
                        qI += width;
                    }
                    if (j == -1 && zPlayer - 128 < zCenter - halfWidth) {
                        qJ -= width;
                    } else if (j == 1 && zPlayer + 128 > zCenter + halfWidth) {
                        qJ += width;
                    }
                    // Some lambda bullshit
                    int finalQI = qI;
                    int finalQJ = qJ;
                    if ((qI != xCenter || qJ != zCenter) && results.stream()
                            .noneMatch(p -> p.getFirst() == finalQI && p.getSecond() == finalQJ)) {
                        results.add(new Pair<>(qI, qJ));
                    }
                }
            }
        }
        return results;
    }
}