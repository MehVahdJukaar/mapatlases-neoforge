package pepjebs.mapatlases.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import pepjebs.mapatlases.client.CompoundTooltip;

public class ShearButton extends BookmarkButton {
    protected ShearButton(int pX, int pY, AtlasOverviewScreen screen) {
        super(pX, pY, 16, 16, 47, 152, screen);
        Tooltip tooltip = Tooltip.create(Component.translatable("message.map_atlases.shear"));
        if (Minecraft.getInstance().options.advancedItemTooltips) {
            Tooltip t2 = Tooltip.create(Component.translatable("message.map_atlases.shear.info")
                    .withStyle(ChatFormatting.GRAY));
            tooltip = CompoundTooltip.create(tooltip, t2);
        }
        this.setTooltip(tooltip);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        parentScreen.toggleShearing();
    }

}
