package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.candlelight.api.VirtualOverride;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import pepjebs.mapatlases.client.CompoundTooltip;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.utils.DecorationHolder;

import static pepjebs.mapatlases.client.MapAtlasesClient.DELETE_MARKER_SPRITE;
import static pepjebs.mapatlases.client.MapAtlasesClient.FOCUS_MARKER_SPRITE;

public class DecorationBookmarkButton extends AtlasButton {
    private static final int BUTTON_H = 14;
    private static final int BUTTON_W = 24;

    protected final DecorationHolder holder;
    protected int index = 0;
    protected boolean shifting = false;
    protected boolean control = false;

    public DecorationBookmarkButton(int pX, int pY, AtlasOverviewScreen parentScreen, DecorationHolder holder) {
        super(pX - BUTTON_W, pY, BUTTON_W, BUTTON_H, parentScreen,
                MapAtlasesClient.BOOKMARK_LEFT_SPRITE, MapAtlasesClient.BOOKMARK_LEFT_SELECTED_SPRITE);
        this.holder = holder;
        this.shifting = Screen.hasShiftDown();
        this.control = Screen.hasControlDown();
        this.setTooltip(createTooltip());
    }

    public static DecorationBookmarkButton of(int px, int py, DecorationHolder holder, AtlasOverviewScreen screen) {
        return new DecorationBookmarkButton(px, py, screen, holder);
    }

    @Override
    public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
        this.shifting = Screen.hasShiftDown();
        this.control = Screen.hasControlDown();
        this.setTooltip(this.createTooltip());
        return false;
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        this.shifting = Screen.hasShiftDown();
        this.control = Screen.hasControlDown();
        this.setTooltip(this.createTooltip());
        return false;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.setSelected(true);
        if (shifting && holder.canDeleteMarker()) {
            holder.deleteMarker();
            parentScreen.recalculateDecorationWidgets();
        } else if (control && holder.canFocusMarker()) {
            holder.focusMarker();
        } else {
            parentScreen.centerOnDecoration(this);
        }
    }

    @VirtualOverride("neoforge")
    public void onClick(double mouseX, double mouseY, int button) {
        this.setSelected(true);
        if (button == 1 && holder.canFocusMarker()) {
            holder.focusMarker();
        } else {
            onClick(mouseX, mouseY);
        }
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public double getWorldX() { return holder.getWorldX(); }
    public double getWorldZ() { return holder.getWorldZ(); }

    @Override
    protected void renderWidget(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        PoseStack matrices = graphics.pose();
        matrices.pushPose();
        matrices.translate(0, 0, 0.01 * this.index);
        super.renderWidget(graphics, pMouseX, pMouseY, pPartialTick);
        if (!parentScreen.isPlacingPin() && !parentScreen.isEditingText()) {
            if (this.control && holder.canFocusMarker()) {
                graphics.blitSprite(FOCUS_MARKER_SPRITE, getX(), getY(), 5, 5);
            } else if (this.shifting && holder.canDeleteMarker()) {
                graphics.blitSprite(DELETE_MARKER_SPRITE, getX(), getY(), 5, 5);
            }
        }
        holder.renderDecoration(graphics, getX() + width / 2f, getY() + height / 2f);
        matrices.popPose();
    }

    @Override
    public Tooltip createTooltip() {
        if (control && holder.canFocusMarker()) {
            return Tooltip.create(Component.translatable("tooltip.map_atlases.focus_marker"));
        }
        if (shifting && holder.canDeleteMarker()) {
            return Tooltip.create(Component.translatable("tooltip.map_atlases.delete_marker"));
        }
        Tooltip t = Tooltip.create(holder.getDecorationName());
        if (!MapAtlasesClientConfig.drawWorldMapCoords.get()) return t;
        Component coords = Component.literal("X: " + (int) holder.getWorldX() + ", Z: " + (int) holder.getWorldZ())
                .withStyle(ChatFormatting.GRAY);
        return CompoundTooltip.create(t, Tooltip.create(coords));
    }
}