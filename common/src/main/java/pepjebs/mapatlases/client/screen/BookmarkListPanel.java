package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.candlelight.api.VirtualOverride;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.resources.ResourceLocation;
import pepjebs.mapatlases.client.MapAtlasesClient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Base class for the left/right bookmark list panels in {@link AtlasOverviewScreen}.
 * Manages scroll state, arrow buttons, and widget lifecycle; subclasses supply
 * the item-specific creation logic.
 *
 * @param <B> concrete button type stored in the visible list
 */
abstract class BookmarkListPanel<B extends AbstractWidget> {

    private static final int ARROW_W = 8;
    private static final int ARROW_H = 5;
    private static final int ARROW_GAP = 2;

    protected final AtlasOverviewScreen screen;
    protected final int yStart;
    protected final int maxVisible;
    protected final int separation;
    protected final Consumer<AbstractWidget> widgetAdder;
    private final Consumer<AbstractWidget> widgetRemover;

    private final Arrow arrowUp;
    private final Arrow arrowDown;

    protected final List<B> visibleButtons = new ArrayList<>();
    protected int scrollOffset = 0;
    private boolean pendingRefresh = false;

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

        this.arrowUp = new Arrow(false, arrowX, yStart - ARROW_H - ARROW_GAP, screen, this);
        this.arrowDown = new Arrow(true, arrowX, yStart + maxVisible * separation - 1, screen, this);

        widgetAdder.accept(arrowUp);
        widgetAdder.accept(arrowDown);
        arrowUp.visible = false;
        arrowUp.active = false;
        arrowDown.visible = false;
        arrowDown.active = false;
    }

    // ── Subclass contract ─────────────────────────────────────────────────

    /**
     * Total number of items in the full (unclipped) list.
     */
    protected abstract int totalCount();

    /**
     * Create and register widgets for items {@code [from, to)}.
     * Use {@link #widgetAdder} to register each button and add it to {@link #visibleButtons}.
     */
    protected abstract void createVisibleWidgets(int from, int to);

    // ── Scroll ────────────────────────────────────────────────────────────

    void scrollUp() {
        if (canScrollUp()) {
            scrollOffset--;
            pendingRefresh = true;
        }
    }

    void scrollDown() {
        if (canScrollDown()) {
            scrollOffset++;
            pendingRefresh = true;
        }
    }

    /**
     * Apply any pending scroll refresh. Call this after the event-dispatch loop has finished.
     */
    void flush() {
        if (pendingRefresh) {
            pendingRefresh = false;
            refreshVisible();
        }
    }

    /**
     * Let a subclass signal that a refresh is needed (e.g. after re-sorting the data list).
     */
    protected void markRefreshPending() {
        pendingRefresh = true;
    }

    boolean canScrollUp() {
        return scrollOffset > 0;
    }

    boolean canScrollDown() {
        return scrollOffset + maxVisible < totalCount();
    }

    // ── Widget management ─────────────────────────────────────────────────

    protected void clearVisible() {
        for (var btn : visibleButtons) widgetRemover.accept(btn);
        visibleButtons.clear();
    }

    protected void refreshVisible() {
        if (screen.inMouseClick) {
            pendingRefresh = true;
            return;
        }
        clearVisible();
        boolean needsScroll = totalCount() > maxVisible;
        arrowUp.visible = needsScroll;
        arrowUp.active = needsScroll;
        arrowDown.visible = needsScroll;
        arrowDown.active = needsScroll;
        createVisibleWidgets(scrollOffset, Math.min(scrollOffset + maxVisible, totalCount()));
    }

    List<B> getVisibleButtons() {
        return visibleButtons;
    }

    // ── Shared scroll arrow ───────────────────────────────────────────────

    static final class Arrow extends AtlasButton {

        private final BookmarkListPanel<?> panel;
        private final boolean down;
        private final ResourceLocation inactiveSprite;

        Arrow(boolean down, int x, int y, AtlasOverviewScreen screen, BookmarkListPanel<?> panel) {
            super(x, y, ARROW_W, ARROW_H, screen,
                    down ? MapAtlasesClient.SLICE_DOWN_SPRITE : MapAtlasesClient.SLICE_UP_SPRITE,
                    down ? MapAtlasesClient.SLICE_DOWN_HOVERED_SPRITE : MapAtlasesClient.SLICE_UP_HOVERED_SPRITE);
            this.panel = panel;
            this.down = down;
            this.inactiveSprite = down ? MapAtlasesClient.SLICE_DOWN_INACTIVE_SPRITE
                    : MapAtlasesClient.SLICE_UP_INACTIVE_SPRITE;
            this.setSelected(false);
        }

        private boolean isAtLimit() {
            return down ? !panel.canScrollDown() : !panel.canScrollUp();
        }

        @Override
        public ResourceLocation getSprite() {
            return isAtLimit() ? inactiveSprite : super.getSprite();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
            if (!visible) return;
            PoseStack pose = g.pose();
            pose.pushPose();
            pose.translate(0, 0, 2);
            RenderSystem.enableDepthTest();
            g.blitSprite(getSprite(), getX(), getY(), width, height);
            this.setSelected(this.isHovered);
            pose.popPose();
        }

        @Override
        protected boolean clicked(double mouseX, double mouseY) {
            return !isAtLimit() && super.clicked(mouseX, mouseY);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (down) panel.scrollDown();
            else panel.scrollUp();
        }

        @VirtualOverride("neoforge")
        public void onClick(double mouseX, double mouseY, int button) {
            onClick(mouseX, mouseY);
        }

    }
}
