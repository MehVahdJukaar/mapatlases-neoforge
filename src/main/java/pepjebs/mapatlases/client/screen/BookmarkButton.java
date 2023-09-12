package pepjebs.mapatlases.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public abstract class BookmarkButton extends AbstractWidget {

    private static final int BUTTON_H = 18;
    private static final int BUTTON_HL = 14;
    private static final int BUTTON_W = 24;

    private final int yOff;
    private boolean selected = true;

    protected BookmarkButton(int pX, int pY, boolean right) {
        super(pX + (right ? 0 : -BUTTON_W), pY, BUTTON_W, right ? BUTTON_H : BUTTON_HL, Component.empty());
        this.yOff = right ? 0 : BUTTON_H*2;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean selected() {
        return this.selected;
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        pGuiGraphics.blit(MapAtlasesAtlasOverviewScreen.ATLAS_TEXTURE,
                this.getX(), this.getY(), MapAtlasesAtlasOverviewScreen.IMAGE_WIDTH,
                yOff + (this.selected ? this.height : 0),
                this.width, this.height);

    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {

    }

}
