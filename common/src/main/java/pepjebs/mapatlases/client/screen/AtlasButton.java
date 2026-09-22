package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;

public abstract class AtlasButton extends AbstractWidget {

    protected final ResourceLocation sprite;
    protected final ResourceLocation selectedSprite;
    protected final AtlasOverviewScreen parentScreen;
    private boolean selected = true;

    protected AtlasButton(int pX, int pY, int width, int height, AtlasOverviewScreen screen,
                          ResourceLocation sprite, ResourceLocation selectedSprite) {
        super(pX, pY, width, height, Component.empty());
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
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.enableDepthTest();
        if (!visible || !active) return;
        if (parentScreen.isEditingText()) isHovered = false;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, 0, zOffset());
        graphics.blitSprite(getSprite(), this.getX(), this.getY(), this.width, this.height);
        renderContents(graphics);
        pose.popPose();
    }

    protected float zOffset() {
        return 0;
    }

    protected void renderContents(GuiGraphics graphics) {
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

    @Nullable
    public Tooltip createTooltip() {
        return null;
    }

    protected static void playPageTurnSound(SoundManager soundManager) {
        soundManager.play(SimpleSoundInstance.forUI(MapAtlasesMod.ATLAS_PAGE_TURN_SOUND_EVENT.get(), 1.0F,
                (float) (double) MapAtlasesClientConfig.soundScalar.get()));
    }
}
