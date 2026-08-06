package pepjebs.mapatlases.lifecycle;

import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.config.UpdateFashion;
import pepjebs.mapatlases.integration.moonlight.EntityRadar;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.map_collection.MapGridKey;
import pepjebs.mapatlases.networking.S2CWorldHashPacket;
import pepjebs.mapatlases.utils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class MapAtlasesServerEvents {

    // Used to prevent Map creation spam consuming all Empty Maps on auto-create
    private static final ReentrantLock MUTEX = new ReentrantLock();

    private static final WeakHashMap<Player, UpdateScheduler> SCHEDULERS_PER_PLAYER = new WeakHashMap<>();
    private static final WeakHashMap<Player, MapDataHolder> LAST_CENTER_MAP_PER_PLAYER = new WeakHashMap<>();

    public static void onPlayerTick(ServerPlayer player) {
        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
        if (atlas.isEmpty()) return;
        if (MapAtlasItem.isLocked(atlas)) return;

        Level level = player.level();
        ResourceKey<Level> dimension = level.dimension();
        if (level.dimensionTypeRegistration().is(MapAtlasesMod.NON_TRACKED_DIMENSIONS)) {
            //don't do anything if player is in a non-tracked dimension
            return;
        }
        Slice slice = MapAtlasItem.getSelectedSlice(atlas, dimension);
        MapCollection maps = MapAtlasItem.getMaps(atlas, level);
        MapsNeighborhood neighborhood = MapsNeighborhood.around(player, maps.getScale(), slice);


        boolean createdNewMap = false;
        List<MapDataHolder> mapsInView = new ArrayList<>();
        //create missing maps
        boolean canFillEmpty = MapAtlasesConfig.enableEmptyMapEntryAndFill.get();
        for (var m : neighborhood.all()) {
            MapDataHolder info = maps.select(m);
            if (info == null && canFillEmpty) {
                //can alter map collection
                info = maybeCreateNewMapEntry(player, atlas, m);
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
            sendSlicesAboveAndBelow(player, atlas, maps, neighborhood.center());
        }

        if (mapsInView.isEmpty()) return;

        // Update Map states & colors
        // updateColors is *easily* the most expensive function in the entire server tick
        // As a result, we will only ever call updateColors twice per tick (same as vanilla's limit)
        UpdateScheduler scheduler = SCHEDULERS_PER_PLAYER.computeIfAbsent(player, p -> {
            if (MapAtlasesConfig.updateFashion.get() == UpdateFashion.ROUND_ROBIN) {
                return new RoundRobinUpdateScheduler();
            } else {
                return new WeightedUpdateScheduler();
            }
        });

        scheduler.performUpdate(player, mapsInView);

        for (MapDataHolder mapHolder : mapsInView) {
            MapAtlasesAccessUtils.tickHoldingPlayerAndSync(mapHolder, player, atlas, TriState.SET_TRUE);
            //if data has changed, a packet will be sent
        }
        // for far away maps so we remove player marker
        MapDataHolder lastData = LAST_CENTER_MAP_PER_PLAYER.get(player);
        if (lastData != null && !mapsInView.contains(lastData)) {
            MapAtlasesAccessUtils.tickHoldingPlayerAndSync(lastData, player, atlas, TriState.SET_FALSE);
        }
        LAST_CENTER_MAP_PER_PLAYER.put(player, maps.select(neighborhood.center()));

        if (createdNewMap) {
            // Play the sound
            player.level().playSound(null, player.blockPosition(),
                    MapAtlasesMod.ATLAS_CREATE_MAP_SOUND_EVENT.get(),
                    SoundSource.PLAYERS, 1, 1.0F);
        }
    }

    private static void sendSlicesAboveAndBelow(ServerPlayer player, ItemStack atlas,
                                                MapCollection maps, MapGridKey activeKey) {
        Slice slice = activeKey.slice;
        var dimension = activeKey.slice.dimension();
        var tree = maps.getHeightTree(dimension, slice.type());
        for (Integer hh : tree) {
            if (hh != slice.heightOrTop()) {
                var below = maps.select(activeKey.mapX, activeKey.mapZ, Slice.of(slice.type(), hh, dimension));
                if (below != null)
                    MapAtlasesAccessUtils.tickHoldingPlayerAndSync(below, player, atlas, TriState.SET_TRUE);
            }
        }
    }

    //TODO: optimize
    @Nullable
    private static MapDataHolder maybeCreateNewMapEntry(
            ServerPlayer player,
            ItemStack atlas,
            MapGridKey key
    ) {
        MapCollection maps = MapAtlasItem.getMaps(atlas, player.level());
        Level level = player.level();
        if (maps.getCount() == 0) {
            // If the Atlas is "inactive", give it a pity Empty Map count
            MapAtlasItem.getEmptyMaps(atlas).setAndAssign(atlas, MapType.VANILLA, MapAtlasesConfig.pityActivationMapCount.get());
        }

        Slice slice = key.slice;
        int destX = key.mapX;
        int destZ = key.mapZ;
        int emptyCount = MapAtlasItem.getEmptyMaps(atlas).get(slice);
        boolean bypassEmptyMaps = !MapAtlasesConfig.requireEmptyMapsToExpand.get();
        MapDataHolder newMapHolder = null;
        if (!MUTEX.isLocked() && (emptyCount > 0 || player.isCreative() || bypassEmptyMaps)) {
            MUTEX.lock();

            // Make the new map

            //validate height
            var height = slice.height();
            if (height.isPresent() && !maps.getHeightTree(player.level().dimension(), slice.type()).contains(height.get())) {
                MapAtlasesMod.LOGGER.error("Invalid height for slice: {} height: {}", slice, height.get());
            }

            byte scale = maps.getScale();

            ItemStack newMap = slice.createNewMap(destX, destZ, scale, player.level(), atlas);
            MapId newMapId = newMap.get(DataComponents.MAP_ID);

            if (newMapId != null) {
                MapDataHolder newData = MapDataHolder.find(newMapId, slice.type(), level);
                // for custom map data to be sent immediately... crappy and hacky. TODO: change custom map data impl
                if (newData != null) {
                    MapAtlasesAccessUtils.tickHoldingPlayerAndSync(newData, player, newMap, TriState.SET_TRUE);
                }
                boolean addedMap = maps.addAndAssigns(atlas, level, slice.type(), newMapId) != maps;


                if (addedMap) {
                    if (!player.isCreative() && !bypassEmptyMaps) {
                        //remove 1 map
                        MapAtlasItem.getEmptyMaps(atlas).addAndAssigns(atlas, slice, -1);
                    }
                    newMapHolder = newData;
                }
            }
            MUTEX.unlock();
        }
        return newMapHolder;
    }


    public static void onPlayerJoin(ServerPlayer player) {
        NetworkHelper.sendToClientPlayer(player, new S2CWorldHashPacket(player));
        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
        if (atlas.isEmpty()) return;

        Level level = player.level();
        ResourceKey<Level> dimension = level.dimension();
        MapCollection maps = MapAtlasItem.getMaps(atlas, level);

        Slice slice = MapAtlasItem.getSelectedSlice(atlas, dimension);
        // sets new center map
        MapGridKey activeKey = MapGridKey.atEntityPosition(maps.getScale(), slice, player);
        sendSlicesAboveAndBelow(player, atlas, maps, activeKey);

        //TODO: figure out why its not synced automatically
        if (PlatHelper.getPlatform().isFabric()) {
            for (var info : maps.getAllFound()) {
                // update all maps and sends them to player, if needed
                // MapAtlasesAccessUtils.updateMapDataAndSync(info, player, atlas, InteractionResult.PASS);
            }
        }
    }


    public static void onDimensionUnload() {
        EntityRadar.unloadLevel();
    }

}