package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.utils.Slice;

import java.util.List;

public class SliceBookmarkButton extends BookmarkButton {

    private static final int BUTTON_H = 21;
    private static final int BUTTON_W = 27;

    protected final boolean compact = MapAtlasesClientConfig.worldMapCompactSliceIndicator.get();

    private Slice slice;
    private boolean hasMoreThan1Type = true;
    private boolean hasMoreThan1Slice = true;

    protected SliceBookmarkButton(int pX, int pY, Slice slice, AtlasOverviewScreen screen) {
        super(pX, pY, BUTTON_W, BUTTON_H, 0, 167 + 64, screen);
        this.slice = slice;
        this.selected = false;
        this.tooltip =(createTooltip());
    }

    public void refreshState(boolean slice, boolean types) {
        hasMoreThan1Type = types;
        hasMoreThan1Slice = slice;
        this.setActive(slice || types);
    }

    @Override
    public List<Component> createTooltip() {
        return List.of(slice == null ? Component.translatable("item.map_atlases.atlas.tooltip_slice_default") :
                Component.translatable("item.map_atlases.atlas.tooltip_slice", slice.getName().getString()));
    }

    public Slice getSlice() {
        return slice;
    }

    @Override
    public void renderButton(PoseStack pose, int pMouseX, int pMouseY, float pPartialTick) {
        if (!active || !visible) return;
        pose.pushPose();

        pose.translate(0, 0, 2);
        RenderSystem.enableDepthTest();

        super.renderButton(pose, pMouseX, pMouseY, pPartialTick);
        pGuiGraphics.blit(AtlasOverviewScreen.ATLAS_TEXTURE,
                this.getX() + 8, this.getY() + 2, 51 + slice.type().ordinal() * 16,
                167 + 66,
                16, 16);

        if(hasMoreThan1Slice) {
            pose.translate(0, 0, 1);
            Integer h = slice.height();
            Component text = h != null ? Component.literal(String.valueOf(h)) :
                    Component.translatable("message.map_atlases.atlas.slice_default");
            GuiComponent.drawCenteredString(pose, parentScreen.getMinecraft().font,
                text, this.x     + (compact ? 15 : 39), this.y + 7, -1);
        }

        pose.popPose();
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        parentScreen.cycleSliceType();
    }

    public void setSlice(Slice slice) {
        this.slice = slice;
    }

    @Override
    protected boolean isValidClickButton(int pButton) {
        return hasMoreThan1Type;
    }
}
