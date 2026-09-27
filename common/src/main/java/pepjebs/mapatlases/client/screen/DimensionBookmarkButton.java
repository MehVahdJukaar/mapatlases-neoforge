package pepjebs.mapatlases.client.screen;

import net.mehvahdjukaar.moonlight.api.resources.assets.LangBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.data.AtlasIds;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;

public class DimensionBookmarkButton extends AtlasButton {

    private static final int BUTTON_H = 18;
    private static final int BUTTON_W = 24;

    private final ResourceKey<Level> dimension;
    private final Identifier dimensionIconSprite;

    protected DimensionBookmarkButton(int pX, int pY, ResourceKey<Level> dimension, AtlasOverviewScreen screen) {
        super(pX, pY, BUTTON_W, BUTTON_H, screen,
                MapAtlasesClient.BOOKMARK_RIGHT_SPRITE, MapAtlasesClient.BOOKMARK_RIGHT_SELECTED_SPRITE);
        this.dimension = dimension;
        this.dimensionIconSprite = findDimensionIcon(dimension);
        this.setTooltip(createTooltip());
    }

    private static Identifier findDimensionIcon(ResourceKey<Level> dimension) {
        TextureAtlas guiSprites = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.GUI);
        Identifier icon = MapAtlasesMod.res("dimensions/" + dimension.identifier().getPath());
        if (guiSprites.getSprite(icon) == guiSprites.missingSprite()) return MapAtlasesMod.res("dimensions/overworld");
        return icon;
    }

    @Override
    public Tooltip createTooltip() {
        return Tooltip.create(Component.literal(
                LangBuilder.getReadableName(dimension.identifier().getPath())));
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    @Override
    protected void renderContents(GuiGraphicsExtractor graphics) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, dimensionIconSprite, this.getX() + 4, this.getY(), 16, 16);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        this.setSelected(true);
        parentScreen.selectDimension(dimension);
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        playPageTurnSound(soundManager);
    }
}
