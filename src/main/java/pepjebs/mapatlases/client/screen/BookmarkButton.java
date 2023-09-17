package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

public abstract class BookmarkButton extends AbstractWidget {

    private final int xOff;
    private final int yOff;
    protected boolean selected = true;
    protected AtlasOverviewScreen parentScreen;
    protected List<Component> tooltip;

    protected BookmarkButton(int pX, int pY, int width, int height, int xOff, int yOff, AtlasOverviewScreen parent) {
        super(pX, pY, width, height, Component.empty());
        this.xOff = xOff;
        this.yOff = yOff;
        this.parentScreen = parent;

    }


    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean selected() {
        return this.selected;
    }


    @Override
    public void renderButton(PoseStack pose, int pMouseX, int pMouseY, float pPartialTick) {
        RenderSystem.enableDepthTest();
        if (!visible || !active) return;
        RenderSystem.setShaderTexture(0, AtlasOverviewScreen.ATLAS_TEXTURE);
        this.blit(pose,
                this.x, this.y, xOff,
                yOff + (this.selected ? this.height : 0),
                this.width, this.height);

    }

    @Override
    public void renderToolTip(PoseStack pPoseStack, int pMouseX, int pMouseY) {
        super.renderToolTip(pPoseStack, pMouseX, pMouseY);
        if (!visible || !active) return;
        parentScreen.renderTooltip(pPoseStack, tooltip, Optional.empty(), pMouseX, pMouseY);

    }

    public void setActive(boolean active) {
        this.active = active;
        this.visible = active;
        this.tooltip = (active ? createTooltip() : null);
    }

    public List<Component> createTooltip() {
        return tooltip;
    }

    @Override
    public void updateNarration(NarrationElementOutput pNarrationElementOutput) {

    }
}
