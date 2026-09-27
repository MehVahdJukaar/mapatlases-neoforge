package pepjebs.mapatlases.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.Tooltip;
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
    protected final int listIndex;
    protected boolean shiftDown;
    protected boolean controlDown;

    public DecorationBookmarkButton(int pX, int pY, int listIndex, DecorationHolder holder, AtlasOverviewScreen parentScreen) {
        super(pX - BUTTON_W, pY, BUTTON_W, BUTTON_H, parentScreen,
                MapAtlasesClient.BOOKMARK_LEFT_SPRITE, MapAtlasesClient.BOOKMARK_LEFT_SELECTED_SPRITE);
        this.holder = holder;
        this.listIndex = listIndex;
        updateModifierKeys();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        updateModifierKeys();
        return false;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        updateModifierKeys();
        return false;
    }

    private void updateModifierKeys() {
        Minecraft mc = Minecraft.getInstance();
        this.shiftDown = mc.hasShiftDown();
        this.controlDown = mc.hasControlDown();
        this.setTooltip(this.createTooltip());
    }

    private boolean willDelete() {
        return shiftDown && holder.canDeleteMarker();
    }

    private boolean willFocus() {
        return controlDown && holder.canFocusMarker();
    }

    @Override
    protected boolean isValidClickButton(MouseButtonInfo buttonInfo) {
        return buttonInfo.button() == 0 || buttonInfo.button() == 1;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        this.setSelected(true);
        if (event.button() == 1 && holder.canFocusMarker()) {
            holder.focusMarker();
        } else if (willDelete()) {
            holder.deleteMarker(parentScreen.lecternPos());
            parentScreen.recalculateDecorationWidgets();
        } else if (willFocus()) {
            holder.focusMarker();
        } else {
            parentScreen.centerOnDecoration(this);
        }
    }

    public double getWorldX() { return holder.getWorldX(); }
    public double getWorldZ() { return holder.getWorldZ(); }

    @Override
    protected void renderContents(GuiGraphicsExtractor graphics) {
        if (!parentScreen.isPlacingPin() && !parentScreen.isEditingText()) {
            if (willFocus()) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FOCUS_MARKER_SPRITE, getX(), getY(), 5, 5);
            } else if (willDelete()) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, DELETE_MARKER_SPRITE, getX(), getY(), 5, 5);
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
