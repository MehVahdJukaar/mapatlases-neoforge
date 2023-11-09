package pepjebs.mapatlases.lifecycle;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.integration.moonlight.ClientMarkers;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.networking.C2S2COpenAtlasScreenPacket;
import pepjebs.mapatlases.networking.MapAtlasesNetworking;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

public class MapAtlasesClientEvents {

    @SubscribeEvent
    public static void onKeyPressed(InputEvent.Key event) {

        if (Minecraft.getInstance().screen != null || event.getAction() != InputConstants.PRESS) return;
        int key = event.getKey();
        int code = event.getScanCode();
        if (MapAtlasesClient.OPEN_ATLAS_KEYBIND.matches(key, code)) {
            Minecraft client = Minecraft.getInstance();
            if (client.level == null || client.player == null) return;
            ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(client.player);
            if (atlas.getItem() instanceof MapAtlasItem) {
                // needed as we might not have all mas needed
                MapAtlasesNetworking.sendToServer(new C2S2COpenAtlasScreenPacket());
            }
        }

        if (MapAtlasesClient.PLACE_PIN_KEYBIND.matches(key, code)) {
            if (MapAtlasesMod.MOONLIGHT && MapAtlasesClientConfig.moonlightCompat.get()) {
                Minecraft client = Minecraft.getInstance();
                if (client.level == null || client.player == null) return;
                ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(client.player);
                if (atlas.getItem() instanceof MapAtlasItem) {
                    MapAtlasesNetworking.sendToServer(new C2S2COpenAtlasScreenPacket(null, true));
                }
            }
        }

        ItemStack atlas = MapAtlasesClient.getCurrentActiveAtlas();
        if (!atlas.isEmpty()) {
            if (MapAtlasesClient.DECREASE_MINIMAP_ZOOM.matches(key, code)) {
                MapAtlasesClient.HUD.decreaseZoom();
            }

            if (MapAtlasesClient.INCREASE_MINIMAP_ZOOM.matches(key, code)) {
                MapAtlasesClient.HUD.increaseZoom();
            }

            if (MapAtlasesClient.INCREASE_SLICE.matches(key, code)) {
                //MapAtlasItem.setSelectedSlice();
            }
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        if (MapAtlasesMod.MOONLIGHT) ClientMarkers.saveClientMarkers();
    }


}
