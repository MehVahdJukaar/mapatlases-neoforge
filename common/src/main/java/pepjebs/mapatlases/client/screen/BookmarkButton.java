package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import static pepjebs.mapatlases.client.MapAtlasesClient.ATLAS_BACKGROUND_TEXTURE;

public abstract class BookmarkButton extends AbstractWidget {

    protected final ResourceLocation sprite;
    protected final ResourceLocation selectedSprite;
    protected final AtlasOverviewScreen parentScreen;
    protected boolean selected = true;

    protected BookmarkButton(int pX, int pY, int width, int height, AtlasOverviewScreen screen,
                             ResourceLocation sprite, ResourceLocation selectedSprite) {
        super(pX, pY,
                width, height,
                Component.empty());
        this.parentScreen = screen;
        this.sprite = sprite;
        this.selectedSprite = selectedSprite;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean selected() {
        return this.selected;
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        RenderSystem.enableDepthTest();
        if (!visible || !active) return;
        pGuiGraphics.blitSprite(getSprite(),
                this.getX(), this.getY(),
                this.width, this.height);
        if (parentScreen.isEditingText()) isHovered = false; //cancel tooltip
    }

    public ResourceLocation getSprite() {
        return selected ? selectedSprite : sprite;
    }

    @Nullable
    @Override
    public Tooltip getTooltip() {
        if (!visible || !active) return null;
        return super.getTooltip();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {

    }

    public void setActive(boolean active) {
        this.active = active;
        this.visible = active;
        this.setTooltip(active ? createTooltip() : null);
    }

    public Tooltip createTooltip() {
        return getTooltip();
    }
}
