package pepjebs.mapatlases.client.screen;

import net.minecraft.client.gui.components.AbstractWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

abstract class BookmarkListPanel<B extends AbstractWidget> {

    protected final AtlasOverviewScreen screen;
    protected final int yStart;
    protected final int maxVisible;
    protected final int separation;
    protected final Consumer<AbstractWidget> widgetAdder;
    private final Consumer<AbstractWidget> widgetRemover;

    private final ScrollArrow arrowUp;
    private final ScrollArrow arrowDown;

    protected final List<B> visibleButtons = new ArrayList<>();
    protected int scrollOffset = 0;
    private boolean refreshPending = false;

    BookmarkListPanel(AtlasOverviewScreen screen,
                      int arrowX, int yStart, int maxVisible, int separation,
                      Consumer<AbstractWidget> widgetAdder,
                      Consumer<AbstractWidget> widgetRemover) {
        this.screen = screen;
        this.yStart = yStart;
        this.maxVisible = maxVisible;
        this.separation = separation;
        this.widgetAdder = widgetAdder;
        this.widgetRemover = widgetRemover;

        this.arrowUp = new ScrollArrow(false, arrowX, yStart - ArrowButton.HEIGHT - 2);
        this.arrowDown = new ScrollArrow(true, arrowX, yStart + maxVisible * separation - 1);
        widgetAdder.accept(arrowUp);
        widgetAdder.accept(arrowDown);
        arrowUp.setActiveAndVisible(false);
        arrowDown.setActiveAndVisible(false);
    }

    protected abstract int totalCount();

    protected abstract void createVisibleWidgets(int from, int to);

    void requestRefresh() {
        refreshPending = true;
    }

    void applyPendingRefresh() {
        if (!refreshPending) return;
        refreshPending = false;
        rebuildVisibleButtons();
    }

    boolean canScrollUp() {
        return scrollOffset > 0;
    }

    boolean canScrollDown() {
        return scrollOffset + maxVisible < totalCount();
    }

    protected void rebuildVisibleButtons() {
        for (var btn : visibleButtons) widgetRemover.accept(btn);
        visibleButtons.clear();
        boolean needsScroll = totalCount() > maxVisible;
        arrowUp.setActiveAndVisible(needsScroll);
        arrowDown.setActiveAndVisible(needsScroll);
        createVisibleWidgets(scrollOffset, Math.min(scrollOffset + maxVisible, totalCount()));
    }

    List<B> getVisibleButtons() {
        return visibleButtons;
    }

    private final class ScrollArrow extends ArrowButton {

        ScrollArrow(boolean down, int x, int y) {
            super(x, y, down, screen);
        }

        @Override
        protected boolean isAtLimit() {
            return down ? !canScrollDown() : !canScrollUp();
        }

        @Override
        protected void step() {
            scrollOffset += down ? 1 : -1;
            requestRefresh();
        }
    }
}
