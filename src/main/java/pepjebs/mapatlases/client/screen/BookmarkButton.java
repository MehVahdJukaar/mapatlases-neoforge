package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.List;

public abstract class BookmarkButton extends AbstractWidget {

    private final int xOff;
    private final int yOff;
    protected final AtlasOverviewScreen parentScreen;
    protected boolean selected = true;
    protected List<Component> tooltip = List.of();

    protected BookmarkButton(int pX, int pY, int width, int height, int xOff, int yOff, AtlasOverviewScreen screen) {
        super(pX, pY,
                width, height,
                Component.empty());
        this.xOff = xOff;
        this.yOff = yOff;
        this.parentScreen = screen;

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
    public void updateNarration(NarrationElementOutput pNarrationElementOutput) {

    }

    public void setActive(boolean active) {
        this.active = active;
        this.visible = active;
        this.tooltip = (active ? createTooltip() : null);
    }

    public List<Component> createTooltip() {
        return List.of();
    }
}
