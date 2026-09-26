package pepjebs.mapatlases.client.screen;

import net.mehvahdjukaar.moonlight.api.resources.assets.LangBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;

public class DimensionBookmarkButton extends AtlasButton {

    private static final int BUTTON_H = 18;
    private static final int BUTTON_W = 24;

    private final ResourceKey<Level> dimension;
    private final ResourceLocation dimensionIconSprite;

    protected DimensionBookmarkButton(int pX, int pY, ResourceKey<Level> dimension, AtlasOverviewScreen screen) {
        super(pX, pY, BUTTON_W, BUTTON_H, screen,
                MapAtlasesClient.BOOKMARK_RIGHT_SPRITE, MapAtlasesClient.BOOKMARK_RIGHT_SELECTED_SPRITE);
        this.dimension = dimension;
        this.dimensionIconSprite = findDimensionIcon(dimension);
        this.setTooltip(createTooltip());
    }

    private static ResourceLocation findDimensionIcon(ResourceKey<Level> dimension) {
        var guiSprites = Minecraft.getInstance().getGuiSprites();
        ResourceLocation icon = MapAtlasesMod.res("dimensions/" + dimension.location().getPath());
        TextureAtlasSprite missing = guiSprites.getSprite(MapAtlasesMod.res("missing"));
        if (guiSprites.getSprite(icon) == missing) return MapAtlasesMod.res("dimensions/overworld");
        return icon;
    }

    @Override
    public Tooltip createTooltip() {
        return Tooltip.create(Component.literal(
                LangBuilder.getReadableName(dimension.location().getPath())));
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    @Override
    protected float zOffset() {
        return selected() ? 2 : 0;
    }

    @Override
    protected void renderContents(GuiGraphics graphics) {
        graphics.blitSprite(dimensionIconSprite, this.getX() + 4, this.getY(), 16, 16);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.setSelected(true);
        parentScreen.selectDimension(dimension);
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        playPageTurnSound(soundManager);
    }
}
