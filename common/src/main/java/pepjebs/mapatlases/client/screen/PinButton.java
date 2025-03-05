package pepjebs.mapatlases.client.screen;

import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ColumnPos;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.CompoundTooltip;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.integration.moonlight.ClientMarkers;
import pepjebs.mapatlases.networking.C2SMarkerPacket;
import pepjebs.mapatlases.utils.MapDataHolder;

public class PinButton extends BookmarkButton {

    protected PinButton(int pX, int pY, AtlasOverviewScreen screen) {
        super(pX, pY, 16, 16, screen,
                MapAtlasesClient.PIN_BUTTON_SPRITE, MapAtlasesClient.PIN_BUTTON_HOVERED_SPRITE);
        Tooltip tooltip = Tooltip.create(Component.translatable("message.map_atlases.pin"));
        if (Minecraft.getInstance().options.advancedItemTooltips) {
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

    @Override
    public ResourceLocation getSprite() {
        return isHovered ? selectedSprite : sprite;
    }

    public static void placePin(MapDataHolder map, ColumnPos pos, String text, int index) {
        if (MapAtlasesMod.MOONLIGHT) {
            ClientMarkers.addMarker(map, pos, text, index);
        } else
            NetworkHelper.sendToServer(new C2SMarkerPacket(map.id, map.type, pos, text.isEmpty() ? null : text));
    }

}
