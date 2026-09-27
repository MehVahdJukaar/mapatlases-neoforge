package pepjebs.mapatlases.client.screen;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;
import pepjebs.mapatlases.client.MapAtlasesClient;

abstract class ArrowButton extends AtlasButton {

    static final int WIDTH = 8;
    static final int HEIGHT = 5;

    protected final boolean down;
    private final Identifier inactiveSprite;

    ArrowButton(int x, int y, boolean down, AtlasOverviewScreen screen) {
        super(x, y, WIDTH, HEIGHT, screen,
                down ? MapAtlasesClient.SLICE_DOWN_SPRITE : MapAtlasesClient.SLICE_UP_SPRITE,
                down ? MapAtlasesClient.SLICE_DOWN_HOVERED_SPRITE : MapAtlasesClient.SLICE_UP_HOVERED_SPRITE);
        this.down = down;
        this.inactiveSprite = down ? MapAtlasesClient.SLICE_DOWN_INACTIVE_SPRITE : MapAtlasesClient.SLICE_UP_INACTIVE_SPRITE;
    }

    @Override
    protected boolean drawnOverBookEdge() {
        return true;
    }

    protected abstract boolean isAtLimit();

    protected abstract void step();

    @Override
    public Identifier getSprite() {
        if (isAtLimit()) return inactiveSprite;
        return isHovered ? highlightedSprite : sprite;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return !isAtLimit() && super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        step();
    }
}
