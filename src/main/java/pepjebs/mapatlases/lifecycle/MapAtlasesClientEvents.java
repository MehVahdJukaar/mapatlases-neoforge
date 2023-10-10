package pepjebs.mapatlases.lifecycle;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.integration.ClientMarker;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.networking.C2SCOpenAtlasScreenPacket;
import pepjebs.mapatlases.networking.MapAtlasesNetworking;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

public class MapAtlasesClientEvents {

    @SubscribeEvent
    public static void onKeyPressed(InputEvent.Key event) {

        if (Minecraft.getInstance().screen != null || event.getAction() != InputConstants.PRESS) return;
        if (!MapAtlasesClient.OPEN_ATLAS_KEYBIND.isUnbound() &&
                event.getKey() == MapAtlasesClient.OPEN_ATLAS_KEYBIND.getKey().getValue()
        ) {
            Minecraft client = Minecraft.getInstance();
            if (client.level == null || client.player == null) return;
            ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(client.player);
            if (atlas.getItem() instanceof MapAtlasItem) {
                MapAtlasesNetworking.sendToServer(new C2SCOpenAtlasScreenPacket());
                MapAtlasesClient.openScreen(atlas);
            }
        }

        if (!MapAtlasesClient.getCurrentActiveAtlas().isEmpty()) {
            if (!MapAtlasesClient.DECREASE_MINIMAP_ZOOM.isUnbound() &&
                    event.getKey() == MapAtlasesClient.DECREASE_MINIMAP_ZOOM.getKey().getValue()
            ) {
                MapAtlasesClient.HUD.decreaseZoom();
            }
        }

        if (!MapAtlasesClient.getCurrentActiveAtlas().isEmpty()) {
            if (!MapAtlasesClient.INCREASE_MINIMAP_ZOOM.isUnbound() &&
                    event.getKey() == MapAtlasesClient.INCREASE_MINIMAP_ZOOM.getKey().getValue()
            ) {
                MapAtlasesClient.HUD.increaseZoom();
            }
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        if (MapAtlasesMod.MOONLIGHT) ClientMarker.saveClientMarkers();

    }

}
