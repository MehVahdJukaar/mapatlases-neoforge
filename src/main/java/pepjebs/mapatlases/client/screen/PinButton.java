package pepjebs.mapatlases.client.screen;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public class PinButton extends BookmarkButton{
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
            ClientMarker.addMarker(map, pos, text, index);
        } else MapAtlasesNetworking.sendToServer(new C2SMarkerPacket(pos, map.stringId, text.isEmpty() ? null : text));
    }

}
