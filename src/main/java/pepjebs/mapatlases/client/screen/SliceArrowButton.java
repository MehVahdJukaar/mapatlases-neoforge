package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import org.jetbrains.annotations.Nullable;

public class SliceArrowButton extends BookmarkButton {

    private static final int BUTTON_H = 7;
    private static final int BUTTON_W = 12;

    private final boolean down;
    private final SliceBookmarkButton parent;

    protected SliceArrowButton(boolean down, SliceBookmarkButton button) {
        super(button.getX() + button.getWidth()+6, button.getY() -1 + (down ? button.getHeight() - BUTTON_H + 2 : 0),

                BUTTON_W, BUTTON_H, button.getWidth() + (down ? BUTTON_W : 0), MapAtlasesAtlasOverviewScreen.IMAGE_HEIGHT + 64);
        this.down = down;
        this.parent = button;
        this.setSelected(false);
    }


    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        PoseStack pose = pGuiGraphics.pose();
        pose.pushPose();

        if (selected()) {
            pose.translate(0, 0, 2);
        }
        super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.setSelected(this.isHovered);
        pose.popPose();
    }

    @Nullable
    @Override
    public Tooltip getTooltip() {
        return super.getTooltip();
    }


    @Override
    public void onClick(double mouseX, double mouseY, int button) {
    }

}
