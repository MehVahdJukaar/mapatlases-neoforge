package pepjebs.mapatlases.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;

public abstract class AtlasButton extends AbstractWidget {

    protected final Identifier sprite;
    protected final Identifier highlightedSprite;
    protected final AtlasOverviewScreen parentScreen;
    private boolean selected = true;

    protected AtlasButton(int pX, int pY, int width, int height, AtlasOverviewScreen screen,
                          Identifier sprite, Identifier highlightedSprite) {
        super(pX, pY, width, height, Component.empty());
        this.parentScreen = screen;
        this.sprite = sprite;
        this.highlightedSprite = highlightedSprite;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean selected() {
        return this.selected;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible || !active) return;
        if (parentScreen.isEditingText()) isHovered = false;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, getSprite(), this.getX(), this.getY(), this.width, this.height);
        renderContents(graphics);
    }

    protected void renderContents(GuiGraphicsExtractor graphics) {
    }

    protected boolean drawnOverBookEdge() {
        return false;
    }

    public Identifier getSprite() {
        return selected ? highlightedSprite : sprite;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
    }

    public void setActiveAndVisible(boolean active) {
        this.active = active;
        this.visible = active;
        this.setTooltip(active ? createTooltip() : null);
    }

    @Nullable
    public Tooltip createTooltip() {
        return null;
    }

    protected static void playPageTurnSound(SoundManager soundManager) {
        soundManager.play(SimpleSoundInstance.forUI(MapAtlasesMod.ATLAS_PAGE_TURN_SOUND_EVENT.get(), 1.0F,
                (float) (double) MapAtlasesClientConfig.soundScalar.get()));
    }
}
