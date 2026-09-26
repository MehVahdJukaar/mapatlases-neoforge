package pepjebs.mapatlases.lifecycle;

import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.config.UpdateFashion;
import pepjebs.mapatlases.integration.moonlight.EntityRadar;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.map_collection.MapGridKey;
import pepjebs.mapatlases.map_collection.MapsNeighborhood;
import pepjebs.mapatlases.networking.S2CWorldHashPacket;
import pepjebs.mapatlases.utils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MapAtlasesServerEvents {

    // keyed by UUID, not by player instance: the map data these values point at lists the players holding it,
    // so a value can reach its own key and weak keys would never be collected
    private static final Map<UUID, UpdateScheduler> SCHEDULERS_PER_PLAYER = new HashMap<>();
    private static final Map<UUID, AtlasMap> LAST_CENTER_MAP_PER_PLAYER = new HashMap<>();

    public static void onPlayerTick(ServerPlayer player) {
        ItemStack atlas = AtlasLookup.getAtlasFromPlayerByConfig(player);
        if (atlas.isEmpty()) return;
        if (MapAtlasItem.isLocked(atlas)) return;

        Level level = player.level();
        if (level.dimensionTypeRegistration().is(MapAtlasesMod.NON_TRACKED_DIMENSIONS)) {
            //don't do anything if player is in a non-tracked dimension
            return;
        }
        Slice slice = MapAtlasItem.getSelectedSlice(atlas, level.dimension());
        MapCollection maps = MapAtlasItem.getMaps(atlas, level);
        MapsNeighborhood neighborhood = MapsNeighborhood.around(player, maps.getScale(), slice);


        boolean createdNewMap = false;
        List<AtlasMap> mapsInView = new ArrayList<>();
        //create missing maps
        boolean canFillEmpty = MapAtlasesConfig.enableEmptyMapEntryAndFill.get();
        for (var m : neighborhood.keys()) {
            AtlasMap info = maps.getMapAt(m);
            if (info == null && canFillEmpty) {
                //can alter map collection
                info = MapAtlasItem.createMapAt(player, atlas, m);
                if (info != null) {
                    //update maps reference
                    maps = MapAtlasItem.getMaps(atlas, level);
                    createdNewMap = true;
                }
            }
            if (info != null) mapsInView.add(info);

        }

        //sync the slice below and above so we can update slice automatically
        if ((level.getGameTime() + 13) % 40 == 0) {
            syncOtherHeightsAtSameCell(player, atlas, maps, neighborhood.center());
        }

        if (mapsInView.isEmpty()) return;

        UpdateScheduler scheduler = SCHEDULERS_PER_PLAYER.computeIfAbsent(player.getUUID(), p -> createScheduler());
        scheduler.performUpdate(player, mapsInView);

        for (AtlasMap mapHolder : mapsInView) {
            mapHolder.tickCarriedByAndSync(player, atlas, TriState.SET_TRUE);
            //if data has changed, a packet will be sent
        }
        // for far away maps so we remove player marker
        AtlasMap lastData = LAST_CENTER_MAP_PER_PLAYER.get(player.getUUID());
        if (lastData != null && !mapsInView.contains(lastData)) {
            lastData.tickCarriedByAndSync(player, atlas, TriState.SET_FALSE);
        }
        LAST_CENTER_MAP_PER_PLAYER.put(player.getUUID(), maps.getMapAt(neighborhood.center()));

        if (createdNewMap) {
            // Play the sound
            level.playSound(null, player.blockPosition(),
                    MapAtlasesMod.ATLAS_CREATE_MAP_SOUND_EVENT.get(),
                    SoundSource.PLAYERS, 1, 1.0F);
        }
    }

    private static UpdateScheduler createScheduler() {
        if (MapAtlasesConfig.updateFashion.get() == UpdateFashion.ROUND_ROBIN) {
            return new RoundRobinUpdateScheduler();
        }
        return new WeightedUpdateScheduler();
    }

    private static void syncOtherHeightsAtSameCell(ServerPlayer player, ItemStack atlas,
                                                   MapCollection maps, MapGridKey activeKey) {
        Slice slice = activeKey.slice;
        var dimension = slice.dimension();
        for (int h : maps.getHeightTree(dimension, slice.type())) {
            if (h == slice.heightOrTop()) continue;
            var other = maps.getMapAt(activeKey.mapX, activeKey.mapZ, Slice.of(slice.type(), h, dimension));
            if (other != null) other.tickCarriedByAndSync(player, atlas, TriState.SET_TRUE);
        }
    }

    public static void onPlayerJoin(ServerPlayer player) {
        NetworkHelper.sendToClientPlayer(player, new S2CWorldHashPacket(player));
        ItemStack atlas = AtlasLookup.getAtlasFromPlayerByConfig(player);
        if (atlas.isEmpty()) return;

        Level level = player.level();
        MapCollection maps = MapAtlasItem.getMaps(atlas, level);
        Slice slice = MapAtlasItem.getSelectedSlice(atlas, level.dimension());
        // sets new center map
        MapGridKey activeKey = MapGridKey.atEntityPosition(maps.getScale(), slice, player);
        syncOtherHeightsAtSameCell(player, atlas, maps, activeKey);
    }


    public static void onPlayerLogout(ServerPlayer player) {
        SCHEDULERS_PER_PLAYER.remove(player.getUUID());
        LAST_CENTER_MAP_PER_PLAYER.remove(player.getUUID());
    }


    public static void onDimensionUnload() {
        EntityRadar.unloadLevel();
        DummyEmptyChunk.clear();
    }

}