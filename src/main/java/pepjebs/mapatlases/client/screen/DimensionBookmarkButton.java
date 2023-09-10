package pepjebs.mapatlases.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class DimensionBookmarkButton extends BookmarkButton {

    private final int dimY;

    protected DimensionBookmarkButton(int pX, int pY, ResourceKey<Level> dimension) {
        super(pX, pY, true);
        this.setTooltip(Tooltip.create(Component.literal(MapAtlasesAtlasOverviewScreen.getReadableName(dimension.location()))));
        if (dimension == Level.OVERWORLD) {
            dimY = 72;
        } else if (dimension == Level.NETHER) {
            dimY = 72 + 16;
        } else if (dimension == Level.END) {
            dimY = 72 + 16 * 2;
        } else {
            dimY = 72 + 16 * 3;
        }
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        pGuiGraphics.blit(MapAtlasesAtlasOverviewScreen.ATLAS_TEXTURE,
                this.getX()+4, this.getY()+2, MapAtlasesAtlasOverviewScreen.IMAGE_WIDTH,
                dimY,
                16, 16);
    }

    @Nullable
    @Override
    public Tooltip getTooltip() {
        return super.getTooltip();
    }
}
