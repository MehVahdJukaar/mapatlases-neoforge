package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import pepjebs.mapatlases.client.MapAtlasesClient;
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
        super(pX, pY, BUTTON_W, BUTTON_H,  screen, MapAtlasesClient.SLICE_BOOKMARK_SPRITE, MapAtlasesClient.SLICE_BOOKMARK_SPRITE);
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
        return Tooltip.create(slice.height().isEmpty() ? Component.translatable("item.map_atlases.atlas.tooltip_slice_default") :
                Component.translatable("item.map_atlases.atlas.tooltip_slice", slice.height().get()));
    }

    public Slice getSlice() {
        return slice;
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (!active || !visible) return;
        PoseStack pose = pGuiGraphics.pose();
        pose.pushPose();

        pose.translate(0, 0, 2);
        RenderSystem.enableDepthTest();

        super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        ResourceLocation sprite = switch(slice.type()){
            case VANILLA, SLICED -> MapAtlasesClient.MAP_TYPE_VANILLA_SPRITE;
            case MAZE -> MapAtlasesClient.MAP_TYPE_MAZE_SPRITE;
            case ORE_MAZE -> MapAtlasesClient.MAP_TYPE_ORE_SPRITE;
            case MAGIC -> MapAtlasesClient.MAP_TYPE_MAGIC_SPRITE;
        };
        pGuiGraphics.blitSprite(sprite,
                this.getX() + 8, this.getY() + 2, 16, 16);

        if (hasMoreThan1Slice) {
            pose.translate(0, 0, 1);
            var h = slice.height();
            Component text = h.map(integer -> Component.literal(String.valueOf(integer)))
                    .orElseGet(() -> Component.translatable("message.map_atlases.atlas.slice_default"));
            pGuiGraphics.drawCenteredString(parentScreen.getMinecraft().font,
                    text, this.getX() + (compact ? 17 : 39), this.getY() + 7, -1);
        }

        pose.popPose();
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        parentScreen.cycleSliceType();
    }

    //@Override
    public void onClick(double mouseX, double mouseY, int button) {
        onClick(mouseX, mouseY);
    }

    public void setSlice(Slice slice) {
        this.slice = slice;
    }

    @Override
    protected boolean isValidClickButton(int pButton) {
        return hasMoreThan1Type;
    }
}
