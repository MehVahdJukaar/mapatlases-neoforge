package pepjebs.mapatlases.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public abstract class BookmarkButton extends AbstractWidget {

    private final int xOff;
    private final int yOff;
    protected boolean selected = true;

    protected BookmarkButton(int pX, int pY, int width, int height, int xOff, int yOff) {
        super(pX, pY, width, height, Component.empty());
        this.xOff = xOff;
        this.yOff = yOff;

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
                this.getX(), this.getY(), xOff,
                yOff + (this.selected ? this.height : 0),
                this.width, this.height);

    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {

    }

}
