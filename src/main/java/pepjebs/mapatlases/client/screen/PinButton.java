package pepjebs.mapatlases.client.screen;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public class PinButton extends BookmarkButton{
    protected PinButton(int pX, int pY, AtlasOverviewScreen screen) {
        super(pX, pY, 16, 16, 30, 152, screen);
        this.setTooltip(Tooltip.create(Component.translatable("message.map_atlases.pin")));
    }


    @Override
    protected boolean clicked(double pMouseX, double pMouseY) {
        return super.clicked(pMouseX, pMouseY);
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        parentScreen.placingPin = !parentScreen.placingPin;
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }
}
