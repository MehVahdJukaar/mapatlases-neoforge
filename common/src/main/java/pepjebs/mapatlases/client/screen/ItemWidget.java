package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import static pepjebs.mapatlases.client.MapAtlasesClient.ATLAS_BACKGROUND_TEXTURE;

public class ItemWidget extends AbstractWidget {

    protected final AtlasOverviewScreen parentScreen;
    private final ItemStack item;

    protected ItemWidget(int pX, int pY, AtlasOverviewScreen screen,
                         ItemStack item) {
        super(pX, pY,
                16, 16,
                Component.empty());
        this.parentScreen = screen;
        this.item = item;
    }


    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        RenderSystem.disableDepthTest();
        if (!visible || !active) return;
        Minecraft mc = Minecraft.getInstance();
        pGuiGraphics.renderItem(mc.player, item, this.getX(),this.getY(),0);
        if (parentScreen.isEditingText()) isHovered = false; //cancel tooltip
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
