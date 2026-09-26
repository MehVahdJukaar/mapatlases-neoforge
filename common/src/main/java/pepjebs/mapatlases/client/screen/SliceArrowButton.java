package pepjebs.mapatlases.client.screen;

import net.minecraft.client.sounds.SoundManager;

import java.util.NavigableSet;

public class SliceArrowButton extends ArrowButton {

    private final SliceBookmarkButton button;
    private int limitHeight;

    protected SliceArrowButton(boolean down, SliceBookmarkButton button, AtlasOverviewScreen screen) {
        super(arrowX(button), arrowY(down, button), down, screen);
        this.button = button;
        this.limitHeight = down ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    }

    private static int arrowX(SliceBookmarkButton button) {
        return button.getX() + button.getWidth() + 6 + (button.compact ? -22 : 0);
    }

    private static int arrowY(boolean down, SliceBookmarkButton button) {
        int y = button.getY() - 1 + (down ? button.getHeight() - HEIGHT + 2 : 0);
        if (button.compact) y += (down ? 7 : -7);
        return y;
    }

    @Override
    protected boolean isAtLimit() {
        return button.getSlice().heightOrTop() == limitHeight;
    }

    @Override
    protected void step() {
        parentScreen.stepSlice(!down);
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        super.playDownSound(soundManager);
        playPageTurnSound(soundManager);
    }

    public void setLimitHeight(NavigableSet<Integer> heightTree) {
        if (heightTree.isEmpty()) return;
        limitHeight = down ? heightTree.first() : heightTree.last();
    }
}
