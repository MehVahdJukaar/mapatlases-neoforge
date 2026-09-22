package pepjebs.mapatlases.client.screen;

import net.mehvahdjukaar.candlelight.api.VirtualOverride;
import net.minecraft.resources.ResourceLocation;
import pepjebs.mapatlases.client.MapAtlasesClient;

abstract class ArrowButton extends AtlasButton {

    static final int WIDTH = 8;
    static final int HEIGHT = 5;

    protected final boolean down;
    private final ResourceLocation inactiveSprite;

    ArrowButton(int x, int y, boolean down, AtlasOverviewScreen screen) {
        super(x, y, WIDTH, HEIGHT, screen,
                down ? MapAtlasesClient.SLICE_DOWN_SPRITE : MapAtlasesClient.SLICE_UP_SPRITE,
                down ? MapAtlasesClient.SLICE_DOWN_HOVERED_SPRITE : MapAtlasesClient.SLICE_UP_HOVERED_SPRITE);
        this.down = down;
        this.inactiveSprite = down ? MapAtlasesClient.SLICE_DOWN_INACTIVE_SPRITE : MapAtlasesClient.SLICE_UP_INACTIVE_SPRITE;
    }

    protected abstract boolean isAtLimit();

    protected abstract void step();

    @Override
    public ResourceLocation getSprite() {
        if (isAtLimit()) return inactiveSprite;
        return isHovered ? selectedSprite : sprite;
    }

    @Override
    protected float zOffset() {
        return 2;
    }

    @Override
    protected boolean clicked(double mouseX, double mouseY) {
        return !isAtLimit() && super.clicked(mouseX, mouseY);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        step();
    }

    @VirtualOverride("neoforge")
    public void onClick(double mouseX, double mouseY, int button) {
        onClick(mouseX, mouseY);
    }
}
