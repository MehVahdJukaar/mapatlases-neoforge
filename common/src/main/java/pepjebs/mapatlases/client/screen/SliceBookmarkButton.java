package pepjebs.mapatlases.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.utils.Slice;

public class SliceBookmarkButton extends AtlasButton {

    private static final int BUTTON_H = 21;
    private static final int BUTTON_W = 27;

    protected final boolean compact = MapAtlasesClientConfig.worldMapCompactSliceIndicator.get();

    private Slice slice;
    private boolean hasMultipleTypes = true;
    private boolean hasMultipleHeights = true;

    protected SliceBookmarkButton(int pX, int pY, Slice slice, AtlasOverviewScreen screen) {
        super(pX, pY, BUTTON_W, BUTTON_H, screen, MapAtlasesClient.SLICE_BOOKMARK_SPRITE, MapAtlasesClient.SLICE_BOOKMARK_SPRITE);
        this.slice = slice;
        this.setSelected(false);
        this.setTooltip(createTooltip());
    }

    public void setAvailableChoices(boolean multipleHeights, boolean multipleTypes) {
        hasMultipleTypes = multipleTypes;
        hasMultipleHeights = multipleHeights;
        this.setActiveAndVisible(multipleHeights || multipleTypes);
    }

    @Override
    public Tooltip createTooltip() {
        return Tooltip.create(slice.height().isEmpty() ? Component.translatable("item.map_atlases.atlas.tooltip_slice_default") :
                Component.translatable("item.map_atlases.atlas.tooltip_slice", slice.height().get()));
    }

    public Slice getSlice() {
        return slice;
    }

    public void setSlice(Slice slice) {
        this.slice = slice;
    }

    @Override
    protected float zOffset() {
        return 2;
    }

    @Override
    protected void renderContents(GuiGraphics graphics) {
        ResourceLocation typeSprite = switch (slice.type()) {
            case VANILLA, SLICED -> MapAtlasesClient.MAP_TYPE_VANILLA_SPRITE;
            case MAZE -> MapAtlasesClient.MAP_TYPE_MAZE_SPRITE;
            case ORE_MAZE -> MapAtlasesClient.MAP_TYPE_ORE_SPRITE;
            case MAGIC -> MapAtlasesClient.MAP_TYPE_MAGIC_SPRITE;
        };
        graphics.blitSprite(typeSprite, this.getX() + 8, this.getY() + 2, 16, 16);

        if (hasMultipleHeights) {
            graphics.pose().translate(0, 0, 1);
            Component text = slice.height().map(h -> (Component) Component.literal(String.valueOf(h)))
                    .orElseGet(() -> Component.translatable("message.map_atlases.atlas.slice_default"));
            graphics.drawCenteredString(parentScreen.getMinecraft().font,
                    text, this.getX() + (compact ? 17 : 39), this.getY() + 7, -1);
        }

        if (isHovered && parentScreen.isShearing()) {
            parentScreen.markCursorActionHasTarget();
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (parentScreen.isShearing()) parentScreen.shearSlice(slice);
        else parentScreen.cycleSliceType();
    }

    @Override
    protected boolean isValidClickButton(int pButton) {
        return hasMultipleTypes;
    }
}
