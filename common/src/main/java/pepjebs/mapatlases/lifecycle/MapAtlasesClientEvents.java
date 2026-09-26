package pepjebs.mapatlases.lifecycle;

import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.SupplementariesClientCompat;
import pepjebs.mapatlases.integration.moonlight.ClientMarkers;
import pepjebs.mapatlases.integration.moonlight.EntityRadar;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.networking.C2S2COpenAtlasScreenPacket;
import pepjebs.mapatlases.networking.C2SSelectSlicePacket;
import pepjebs.mapatlases.utils.AtlasLookup;
import pepjebs.mapatlases.utils.Slice;

import java.util.Optional;

public class MapAtlasesClientEvents {

    public static void onClientTick(Minecraft client, ClientLevel level) {
        long gameTime = level.getGameTime();

        //offsets so these dont all run on the same tick
        if (MapAtlasesMod.SUPPLEMENTARIES && (gameTime + 27) % 40 == 0) {
            SupplementariesClientCompat.onClientTick(level);
        }
        if (client.screen == null && (gameTime + 5) % 40 == 0 && MapAtlasesClientConfig.automaticSlice.get()) {
            ItemStack atlas = MapAtlasesClient.getCurrentActiveAtlas();
            if (!atlas.isEmpty()) {
                MapCollection maps = MapAtlasItem.getMaps(atlas, level);
                Slice selected = MapAtlasItem.getSelectedSlice(atlas, level.dimension());
                Integer newHeight = maps.nearestReachableSliceHeight(client.player, selected.dimension(), selected.type());
                if (newHeight != null) {
                    maybeSyncNewSlice(atlas, selected, Slice.of(selected.type(), newHeight, selected.dimension()));
                }
            }
        }
        if ((gameTime + 7) % 40 == 0 && MapAtlasesClientConfig.entityRadar.get() && MapAtlasesConfig.entityRadar.get()) {
            EntityRadar.onClientTick(client.player);
        }
    }

    public static void onKeyPressed(int key, int code) {

        Minecraft client = Minecraft.getInstance();
        if (client.screen != null || client.level == null || client.player == null) return;
        if (MapAtlasesClient.OPEN_ATLAS_KEYBIND.matches(key, code)) {
            requestAtlasScreen(client.player, false);
        }
        if (MapAtlasesClient.PLACE_PIN_KEYBIND.matches(key, code) && MapAtlasesClientConfig.moonlightCompat.get()) {
            requestAtlasScreen(client.player, true);
        }

        ItemStack atlas = MapAtlasesClient.getCurrentActiveAtlas();
        if (!atlas.isEmpty()) {
            if (MapAtlasesClient.DECREASE_MINIMAP_ZOOM.matches(key, code)) {
                MapAtlasesClient.decreaseHoodZoom();
            }

            if (MapAtlasesClient.INCREASE_MINIMAP_ZOOM.matches(key, code)) {
                MapAtlasesClient.increaseHoodZoom();
            }

            boolean up = MapAtlasesClient.INCREASE_SLICE.matches(key, code);
            if (up || MapAtlasesClient.DECREASE_SLICE.matches(key, code)) {
                MapCollection maps = MapAtlasItem.getMaps(atlas, client.level);
                Slice selected = MapAtlasItem.getSelectedSlice(atlas, client.level.dimension());
                Slice next = maps.adjacentSlice(selected, up);
                if (next != null) maybeSyncNewSlice(atlas, selected, next);
            }
        }
    }

    private static void requestAtlasScreen(Player player, boolean pinOnly) {
        if (!AtlasLookup.getAtlasFromPlayerByConfig(player).isEmpty()) {
            // needed as we might not have all mas needed
            NetworkHelper.sendToServer(new C2S2COpenAtlasScreenPacket(Optional.empty(), pinOnly));
        }
    }

    private static void maybeSyncNewSlice(ItemStack atlas, Slice oldSlice, Slice newSlice) {
        if (!newSlice.equals(oldSlice)) {
            NetworkHelper.sendToServer(new C2SSelectSlicePacket(newSlice, Optional.empty()));
            //update the client immediately
            MapAtlasItem.setSelectedSlice(atlas, newSlice, MapAtlasesClient.getLevel());
        }
    }

    public static void onLoggedOut(RegistryAccess registryAccess) {
        ClientMarkers.saveClientMarkers(registryAccess);
        ClientMarkers.clearClientMarkers();
        EntityRadar.unloadLevel();
    }

}
