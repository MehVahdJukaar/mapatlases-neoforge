package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;

public class SliceBookmarkButton extends BookmarkButton {

    private static final int BUTTON_H = 21;
    private static final int BUTTON_W = 27;

    private final AtlasOverviewScreen parentScreen;
    protected final boolean compact =  MapAtlasesClientConfig.compactSliceIndicator.get();

    private Integer slice;

    protected SliceBookmarkButton(int pX, int pY, @Nullable Integer slice, AtlasOverviewScreen screen) {
        super(pX, pY, BUTTON_W, BUTTON_H, 0, AtlasOverviewScreen.IMAGE_HEIGHT + 64);
        this.slice = slice;
        this.parentScreen = screen;
        this.selected = false;
        this.setTooltip(createTooltip());
    }

    @Override
    public Tooltip createTooltip() {
        return Tooltip.create(slice == null ? Component.translatable("item.map_atlases.atlas.tooltip_slice_default") :
                Component.translatable("item.map_atlases.atlas.tooltip_slice", slice));
    }

    public Integer getSlice() {
        return slice;
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (!active || !visible) return;
        PoseStack pose = pGuiGraphics.pose();
        pose.pushPose();

        pose.translate(0, 0, 2);
        RenderSystem.enableDepthTest();

        super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

        pose.translate(0, 0, 1);
        Component text = slice != null ? Component.literal(String.valueOf(slice)) :
                Component.translatable("message.map_atlases.atlas.slice_default");
        pGuiGraphics.drawCenteredString(parentScreen.getMinecraft().font,
                text, this.getX() + (compact ? 15: 39), this.getY() + 7, -1);


        pose.popPose();
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
    }

    public void setSlice(Integer slice) {
        this.slice = slice;
    }

    @Override
    protected boolean isValidClickButton(int pButton) {
        return false; //cant be clicked
    }
}
