package pepjebs.mapatlases.lifecycle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.SupplementariesClientCompat;
import pepjebs.mapatlases.integration.moonlight.ClientMarkers;
import pepjebs.mapatlases.integration.moonlight.EntityRadar;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.IMapCollection;
import pepjebs.mapatlases.networking.C2S2COpenAtlasScreenPacket;
import pepjebs.mapatlases.networking.C2SSelectSlicePacket;
import pepjebs.mapatlases.networking.MapAtlasesNetworking;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.MapType;
import pepjebs.mapatlases.utils.Slice;

import java.util.TreeSet;

public class MapAtlasesClientEvents {

    public static void onClientTick(Minecraft client, ClientLevel level) {
        long gameTime = level.getGameTime();

        if (MapAtlasesMod.SUPPLEMENTARIES && (gameTime + 27) % 40 == 0) {
            SupplementariesClientCompat.onClientTick(level);
        }
        else if (client.screen == null && (gameTime + 5) % 40 == 0 && MapAtlasesClientConfig.automaticSlice.get()) {
            ItemStack atlas = MapAtlasesClient.getCurrentActiveAtlas();
            if (!atlas.isEmpty()) {
                IMapCollection maps = MapAtlasItem.getMaps(atlas, level);

                Slice s = MapAtlasItem.getSelectedSlice(atlas, level.dimension());
                maybeChangeSlice(client.player, level, maps, s, atlas);
            }
        }
        else if ((gameTime + 7) % 40 == 0 && MapAtlasesClientConfig.entityRadar.get() && MapAtlasesConfig.entityRadar.get()) {
            Player player = client.player;
            if (player != null) {
                EntityRadar.onClientTick(player);
            }
        }
    }

    public static void onKeyPressed(int key, int code) {
    }

    public static void onKeyPressed(KeyMapping keyMapping) {
        Minecraft client = Minecraft.getInstance();
        if (client.screen != null) return;

        if (keyMapping == MapAtlasesClient.OPEN_ATLAS_KEYBIND) {
            if (client.level == null || client.player == null) return;
            ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(client.player);
            if (atlas.getItem() instanceof MapAtlasItem) {
                MapAtlasesNetworking.CHANNEL.sendToServer(C2S2COpenAtlasScreenPacket.forActiveAtlas(false));
            }
        }

        if (keyMapping == MapAtlasesClient.PLACE_PIN_KEYBIND) {
            if (MapAtlasesClientConfig.moonlightCompat.get()) {
                if (client.level == null || client.player == null) return;
                ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(client.player);
                if (atlas.getItem() instanceof MapAtlasItem) {
                    MapAtlasesNetworking.CHANNEL.sendToServer(C2S2COpenAtlasScreenPacket.forActiveAtlas(true));
                }
            }
        }

        ItemStack atlas = MapAtlasesClient.getCurrentActiveAtlas();
        if (!atlas.isEmpty()) {
            if (keyMapping == MapAtlasesClient.DECREASE_MINIMAP_ZOOM) {
                MapAtlasesClient.decreaseHoodZoom();
            }

            if (keyMapping == MapAtlasesClient.INCREASE_MINIMAP_ZOOM) {
                MapAtlasesClient.increaseHoodZoom();
            }

            if (keyMapping == MapAtlasesClient.INCREASE_SLICE) {
                IMapCollection maps = MapAtlasItem.getMaps(atlas, client.level);
                ResourceKey<Level> dim = client.level.dimension();
                Slice selectedSlice = MapAtlasItem.getSelectedSlice(atlas, dim);
                int current = selectedSlice.heightOrTop();
                MapType type = selectedSlice.type();
                Integer newHeight = maps.getHeightTree(dim, type).ceiling(current + 1);
                maybeSyncNewSlice(atlas, selectedSlice, newHeight);
            }

            if (keyMapping == MapAtlasesClient.DECREASE_SLICE) {
                IMapCollection maps = MapAtlasItem.getMaps(atlas, client.level);
                ResourceKey<Level> dim = client.level.dimension();
                Slice selectedSlice = MapAtlasItem.getSelectedSlice(atlas, dim);
                int current = selectedSlice.heightOrTop();
                MapType type = selectedSlice.type();
                Integer newHeight = maps.getHeightTree(dim, type).floor(current - 1);
                maybeSyncNewSlice(atlas, selectedSlice, newHeight);
            }
        }
    }

    private static void maybeSyncNewSlice(ItemStack atlas, Slice oldSlice, Integer newHeight) {
        Slice newSlice = Slice.of(oldSlice.type(), newHeight, oldSlice.dimension());
        if (!newSlice.equals(oldSlice)) {
            MapAtlasesNetworking.CHANNEL.sendToServer(new C2SSelectSlicePacket(newSlice, null));
        }
        //update the client immediately
        MapAtlasItem.setSelectedSlice(atlas, newSlice);
    }

    public static void onLoggedOut() {
        MapAtlasesClient.clearPendingOpenScreen();
        ClientMarkers.unloadWorld();
    }

    //make this client sided
    private static void maybeChangeSlice(Player player, Level level, IMapCollection maps, Slice lastSlice, ItemStack atlas) {
        MapType type = lastSlice.type();
        ResourceKey<Level> dim = lastSlice.dimension();
        Integer newHeight = getClosestSlice(player, level, maps, dim, type);
        if (newHeight != null) {
            maybeSyncNewSlice(atlas, lastSlice, newHeight);
        }
    }


    // null when we dont change
    @Nullable
    public static Integer getClosestSlice(Player player, Level level, IMapCollection cap, ResourceKey<Level> dim, MapType type) {
        //check locked
        TreeSet<Integer> heightTree = cap.getHeightTree(dim, type);
        if (heightTree.size() == 1) return null;
        int y = player.getBlockY();

        int worldSurface = level.getHeight(Heightmap.Types.OCEAN_FLOOR, player.getBlockX(), player.getBlockZ());
        boolean isAboveHeightMap = y >= worldSurface;
        Integer ceiling = heightTree.ceiling(y);
        if (isAboveHeightMap) {
            return ceiling;
        }
        //if not aove check one below and above where we are
        else {
            Integer floor = heightTree.floor(y);

            int aboveDist = ceiling == null ? 0 : ceiling - y;
            int belowDist = floor == null ? 0 : y - floor;
            int max = Math.max(belowDist, aboveDist);
            boolean canGoUp = true;
            boolean canGoDown = true;
            BlockPos.MutableBlockPos pos = player.blockPosition().mutable();
            int startY = pos.getY();
            for (int j = 1; j <= max; j++) {
                //nothing found. we dont change
                if (!canGoUp && !canGoDown) {
                    return null;
                }
                if (j == aboveDist) {
                    return ceiling;
                }
                if (j == belowDist) {
                    return floor;
                }
                if (canGoUp) {
                    pos.setY(startY + j);
                    if (level.getBlockState(pos).getMapColor(level, pos) != MapColor.NONE) {
                        canGoUp = false;
                    }
                }
                if (canGoDown) {
                    pos.setY(startY - j);
                    if (level.getBlockState(pos).getMapColor(level, pos) != MapColor.NONE) {
                        canGoDown = false;
                    }
                }
            }
            return null;
        }
    }


}
