package pepjebs.mapatlases.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.utils.Slice;

import static pepjebs.mapatlases.client.MapAtlasesClient.ATLAS_BACKGROUND_TEXTURE;

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
        this.setTooltip(createTooltip());
    }

    public void refreshState(boolean slice, boolean types) {
        hasMoreThan1Type = types;
        hasMoreThan1Slice = slice;
        this.setActive(slice || types);
    }

    @Override
    public Tooltip createTooltip() {
        return Tooltip.create(slice == null ? Component.translatable("item.map_atlases.atlas.tooltip_slice_default") :
                Component.translatable("item.map_atlases.atlas.tooltip_slice", slice.type().getName().getString()));
    }

    public Slice getSlice() {
        return slice;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!active || !visible) return;
        graphics.nextStratum();
        super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, ATLAS_BACKGROUND_TEXTURE,
                this.getX() + 8, this.getY() + 2, 51 + slice.type().ordinal() * 16,
                167 + 66,
                16, 16, 256, 256);

        if(hasMoreThan1Slice) {
            graphics.nextStratum();
            Integer h = slice.height();
            Component text = h != null ? Component.literal(String.valueOf(h)) :
                    Component.translatable("message.map_atlases.atlas.slice_default");
            graphics.centeredText(parentScreen.getMinecraft().font,
                    text, this.getX() + (compact ? 17 : 39), this.getY() + 7, -1);
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        parentScreen.cycleSliceType();
    }

    public void setSlice(Slice slice) {
        this.slice = slice;
    }

    @Override
    protected boolean isValidClickButton(MouseButtonInfo button) {
        return hasMoreThan1Type && button.button() == 0;
    }
}
