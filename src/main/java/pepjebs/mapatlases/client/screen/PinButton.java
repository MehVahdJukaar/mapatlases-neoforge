package pepjebs.mapatlases.client.screen;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ColumnPos;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.moonlight.ClientMarkers;
import pepjebs.mapatlases.networking.C2SMarkerPacket;
import pepjebs.mapatlases.networking.MapAtlasesNetworking;
import pepjebs.mapatlases.utils.MapDataHolder;

public class PinButton extends BookmarkButton {
    protected PinButton(int pX, int pY, AtlasOverviewScreen screen) {
        super(pX, pY, 16, 16, 30, 152, screen);
        this.setTooltip(Tooltip.create(Component.translatable("message.map_atlases.pin")));
    }


    @Override
    public void onClick(double mouseX, double mouseY) {
        parentScreen.togglePlacingPin();
    }


    public static void placePin( MapDataHolder map, ColumnPos pos, String text, int index) {
        if (MapAtlasesMod.MOONLIGHT) {
            ClientMarkers.addMarker(map, pos, text, index);
        } else MapAtlasesNetworking.sendToServer(new C2SMarkerPacket(pos, map.stringId, text.isEmpty() ? null : text));
    }

}
