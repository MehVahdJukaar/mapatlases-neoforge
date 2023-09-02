package pepjebs.mapatlases.lifecycle;

import com.mojang.datafixers.util.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.networking.MapAtlasesNetowrking;
import pepjebs.mapatlases.networking.S2CSetActiveMapPacket;
import pepjebs.mapatlases.networking.S2CSetMapDataPacket;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

public class MapAtlasesServerEvents {

    public static final ResourceLocation MAP_ATLAS_ACTIVE_STATE_CHANGE = MapAtlasesMod.res("active_state_change");

    // Used to prevent Map creation spam consuming all Empty Maps on auto-create
    private static final Semaphore mutex = new Semaphore(1);

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
        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
        if (atlas.isEmpty()) return;
        Map<String, MapItemSavedData> mapInfos = MapAtlasesAccessUtils.getAllMapInfoFromAtlas(player.level(), atlas);
        for (Map.Entry<String, MapItemSavedData> info : mapInfos.entrySet()) {
            String mapId = info.getKey();
            MapItemSavedData state = info.getValue();
            state.tickCarriedBy(player, atlas);
            state.getHoldingPlayer(player);

            MapAtlasesNetowrking.sendToClientPlayer(player, new S2CSetMapDataPacket(mapId, state, true));

        }
    }

    @SubscribeEvent
    public static void mapAtlasServerTick(TickEvent.ServerTickEvent event) {
        ArrayList<String> seenPlayers = new ArrayList<>();
        MinecraftServer server = event.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String playerName = player.getName().getString();
            seenPlayers.add(playerName);
            if (player.isRemoved() || player.isChangingDimension() || player.hasDisconnected()) continue;
            ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
            if (atlas.isEmpty()) continue;
            Map<String, MapItemSavedData> currentMapInfos =
                    MapAtlasesAccessUtils.getCurrentDimMapInfoFromAtlas(player.level(), atlas);
            Map.Entry<String, MapItemSavedData> activeInfo =
                    MapAtlasesAccessUtils.getActiveAtlasMapStateServer(currentMapInfos, player);


            // changedMapItemSavedData has non-null value if player has a new active Map ID
            String changedMapItemSavedData = relayActiveMapIdToPlayerClient(activeInfo, player);
            if (activeInfo == null) {
                maybeCreateNewMapEntry(player, atlas, 0, Mth.floor(player.getX()),
                        Mth.floor(player.getZ()));
                continue;
            }
            MapItemSavedData activeState = activeInfo.getValue();

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
            Map<String, MapItemSavedData> nearbyExistentMaps = currentMapInfos.entrySet().stream()
                    .filter(e -> discoveringEdges.stream()
                            .anyMatch(edge -> edge.getFirst() == e.getValue().centerX
                                    && edge.getSecond() == e.getValue().centerZ))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            for (var mapInfo : currentMapInfos.entrySet()) {
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
            boolean isPlayerOutsideAllMapRegions = MapAtlasesAccessUtils.distanceBetweenMapItemSavedDataAndPlayer(
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

    private static void updateMapDataForPlayer(
            Map.Entry<String, MapItemSavedData> mapInfo,
            ServerPlayer player,
            ItemStack atlas
    ) {
        mapInfo.getValue().tickCarriedBy(player, atlas);
        relayMapItemSavedDataSyncToPlayerClient(mapInfo, player);
    }

    private static void updateMapColorsForPlayer(
            MapItemSavedData state,
            ServerPlayer player) {
        ((MapItem) Items.FILLED_MAP).update(player.level(), player, state);
    }

    public static void relayMapItemSavedDataSyncToPlayerClient(
            Map.Entry<String, MapItemSavedData> mapInfo,
            ServerPlayer player
    ) {
        int mapId = MapAtlasesAccessUtils.getMapIntFromString(mapInfo.getKey());
        //TODO PORT

        Packet<?> p = null;
        int tries = 0;
        while (p == null && tries < 10) {
            //WHY??
            p = mapInfo.getValue().getUpdatePacket(mapId, player);
            tries++;
        }
        if (p != null) {
            //TODO: maybe use isComplex  update packet and inventory tick
            player.connection.send(p);
        }
    }

    private static String relayActiveMapIdToPlayerClient(
            Map.Entry<String, MapItemSavedData> activeInfo,
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
            String currentMapId = activeInfo.getKey();
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
            int scale,
            int destX,
            int destZ
    ) {
        List<Integer> mapIds = new ArrayList<>();
        if (atlas.getTag() != null) {
            mapIds = Arrays.stream(
                    atlas.getTag().getIntArray(MapAtlasItem.MAP_LIST_NBT)).boxed().collect(Collectors.toList());
        } else {
            // If the Atlas is "inactive", give it a pity Empty Map count
            CompoundTag defaultAtlasNbt = new CompoundTag();
            defaultAtlasNbt.putInt(MapAtlasItem.EMPTY_MAP_NBT,
                    MapAtlasesConfig.pityActivationMapCount.get());
            atlas.setTag(defaultAtlasNbt);
        }
        int emptyCount = MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(atlas);
        boolean bypassEmptyMaps = !MapAtlasesConfig.requireEmptyMapsToExpand.get();
        if (mutex.availablePermits() > 0
                && (emptyCount > 0 || player.isCreative() || bypassEmptyMaps)) {
            try {
                mutex.acquire();

                // Make the new map
                if (!player.isCreative() && !bypassEmptyMaps) {
                    atlas.getTag().putInt(
                            MapAtlasItem.EMPTY_MAP_NBT,
                            atlas.getTag().getInt(MapAtlasItem.EMPTY_MAP_NBT) - 1
                    );
                }
                ItemStack newMap = MapItem.create(
                        player.level(),
                        destX,
                        destZ,
                        (byte) scale,
                        true,
                        false);
                mapIds.add(MapItem.getMapId(newMap));
                atlas.getTag().putIntArray(MapAtlasItem.MAP_LIST_NBT, mapIds);

                // Play the sound
                player.level().playSound(null, player.blockPosition(),
                        MapAtlasesMod.ATLAS_CREATE_MAP_SOUND_EVENT.get(),
                        SoundSource.PLAYERS, (float) (double) MapAtlasesClientConfig.soundScalar.get(), 1.0F);
            } catch (InterruptedException e) {
                MapAtlasesMod.LOGGER.warn(e);
            } finally {
                mutex.release();
            }
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