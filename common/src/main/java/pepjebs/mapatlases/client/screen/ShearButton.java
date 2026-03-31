package pepjebs.mapatlases.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import pepjebs.mapatlases.client.CompoundTooltip;

public class ShearButton extends BookmarkButton {
    protected ShearButton(int pX, int pY, AtlasOverviewScreen screen) {
        super(pX, pY, 16, 16, 47, 152, screen);
        Tooltip tooltip = Tooltip.create(Component.translatable("message.map_atlases.shear"));
        if (Minecraft.getInstance().options.advancedItemTooltips) {
            tooltip = CompoundTooltip.create(
                    Component.translatable("message.map_atlases.shear"),
                    Component.translatable("message.map_atlases.shear.info").withStyle(ChatFormatting.GRAY));
        }
        this.setTooltip(tooltip);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        parentScreen.toggleCursorAction(CursorAction.SHEARING);
    }

}
