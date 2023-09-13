package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class SliceBookmarkButton extends BookmarkButton {

    private static final int BUTTON_H = 21;
    private static final int BUTTON_W = 27;

    private final Integer slice;
    private final MapAtlasesAtlasOverviewScreen parentScreen;

    protected SliceBookmarkButton(int pX, int pY, @Nullable Integer slice, MapAtlasesAtlasOverviewScreen screen) {
        super(pX, pY, BUTTON_W, BUTTON_H, 0, MapAtlasesAtlasOverviewScreen.IMAGE_HEIGHT + 64);
        this.slice = slice;
        this.parentScreen = screen;
        this.setTooltip(Tooltip.create(slice == null ? Component.translatable("item.map_atlases.atlas.tooltip_slice_default") :
                Component.translatable("item.map_atlases.atlas.tooltip_slice", slice)));
        this.setSelected(false);
    }

    public Integer getSlice() {
        return slice;
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        PoseStack pose = pGuiGraphics.pose();
        pose.pushPose();

        pose.translate(0,0,2);
        RenderSystem.enableDepthTest();

        super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

            pose.translate(0,0,1);
        if (slice != null) {
            pGuiGraphics.drawString(parentScreen.getMinecraft().font,
                    String.valueOf(slice)
                    , this.getX()+ 30, this.getY()+7, -1);
        }


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
