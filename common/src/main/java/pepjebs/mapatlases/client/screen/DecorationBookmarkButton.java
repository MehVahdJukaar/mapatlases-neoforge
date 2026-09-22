package pepjebs.mapatlases.client.screen;

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
    protected final int index;
    protected boolean shifting;
    protected boolean control;

    public DecorationBookmarkButton(int pX, int pY, int index, DecorationHolder holder, AtlasOverviewScreen parentScreen) {
        super(pX - BUTTON_W, pY, BUTTON_W, BUTTON_H, parentScreen,
                MapAtlasesClient.BOOKMARK_LEFT_SPRITE, MapAtlasesClient.BOOKMARK_LEFT_SELECTED_SPRITE);
        this.holder = holder;
        this.index = index;
        updateModifierKeys();
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        updateModifierKeys();
        return false;
    }

    @Override
    public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
        updateModifierKeys();
        return false;
    }

    private void updateModifierKeys() {
        this.shifting = Screen.hasShiftDown();
        this.control = Screen.hasControlDown();
        this.setTooltip(this.createTooltip());
    }

    private boolean willDelete() {
        return shifting && holder.canDeleteMarker();
    }

    private boolean willFocus() {
        return control && holder.canFocusMarker();
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.setSelected(true);
        if (willDelete()) {
            holder.deleteMarker(parentScreen.lecternPos());
            parentScreen.recalculateDecorationWidgets();
        } else if (willFocus()) {
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

    public double getWorldX() { return holder.getWorldX(); }
    public double getWorldZ() { return holder.getWorldZ(); }

    @Override
    protected float zOffset() {
        return 0.01f * index;
    }

    @Override
    protected void renderContents(GuiGraphics graphics) {
        if (!parentScreen.isPlacingPin() && !parentScreen.isEditingText()) {
            if (willFocus()) {
                graphics.blitSprite(FOCUS_MARKER_SPRITE, getX(), getY(), 5, 5);
            } else if (willDelete()) {
                graphics.blitSprite(DELETE_MARKER_SPRITE, getX(), getY(), 5, 5);
            }
        }
        holder.renderDecoration(graphics, getX() + width / 2f, getY() + height / 2f);
    }

    @Override
    public Tooltip createTooltip() {
        if (willFocus()) return Tooltip.create(Component.translatable("tooltip.map_atlases.focus_marker"));
        if (willDelete()) return Tooltip.create(Component.translatable("tooltip.map_atlases.delete_marker"));
        Tooltip name = Tooltip.create(holder.getDecorationName());
        if (!MapAtlasesClientConfig.drawWorldMapCoords.get()) return name;
        Component coords = Component.literal("X: " + (int) holder.getWorldX() + ", Z: " + (int) holder.getWorldZ())
                .withStyle(ChatFormatting.GRAY);
        return CompoundTooltip.create(name, Tooltip.create(coords));
    }
}
