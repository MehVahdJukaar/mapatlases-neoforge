package pepjebs.mapatlases.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import pepjebs.mapatlases.client.CompoundTooltip;
import pepjebs.mapatlases.client.MapAtlasesClient;

public class ShearButton extends BookmarkButton {

    protected ShearButton(int pX, int pY, AtlasOverviewScreen screen) {
        super(pX, pY, 16, 16,  screen,
                MapAtlasesClient.SHEAR_BUTTON_SPRITE, MapAtlasesClient.SHEAR_BUTTON_HOVERED_SPRITE);
        Tooltip tooltip = Tooltip.create(Component.translatable("message.map_atlases.shear"));
        if (Minecraft.getInstance().options.advancedItemTooltips) {
            Tooltip t2 = Tooltip.create(Component.translatable("message.map_atlases.shear.info")
                    .withStyle(ChatFormatting.GRAY));
            tooltip = CompoundTooltip.create(tooltip, t2);
        }
        this.setTooltip(tooltip);
    }

    @Override
    public ResourceLocation getSprite() {
        return isHovered ? selectedSprite : sprite;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        parentScreen.toggleShearing();
    }

}
