package pepjebs.mapatlases.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ColumnPos;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.CompoundTooltip;
import pepjebs.mapatlases.integration.moonlight.ClientMarkers;
import pepjebs.mapatlases.networking.C2SMarkerPacket;
import pepjebs.mapatlases.networking.MapAtlasesNetworking;
import pepjebs.mapatlases.utils.MapDataHolder;

public class PinButton extends BookmarkButton {
    protected PinButton(int pX, int pY, AtlasOverviewScreen screen) {
        super(pX, pY, 16, 16, 30, 152, screen);
        Tooltip tooltip = Tooltip.create(Component.translatable("message.map_atlases.pin"));
        if(Minecraft.getInstance().options.advancedItemTooltips){
            Tooltip t2 = Tooltip.create(Component.translatable("message.map_atlases.pin.info")
                    .withStyle(ChatFormatting.GRAY));
            tooltip = CompoundTooltip.create(tooltip, t2);
        }
        this.setTooltip(tooltip);
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
