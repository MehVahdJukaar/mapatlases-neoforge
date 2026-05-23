package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.client.MapAtlasesClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages the right-side dimension bookmark list with arrow-scroll when dimensions overflow the book height.
 */
class DimensionListPanel {

    private static final int SEPARATION = 22;
    private static final int ARROW_W = 8;
    private static final int ARROW_H = 5;

    private final AtlasOverviewScreen screen;
    // X passed directly to DimensionBookmarkButton (no internal offset unlike decoration buttons)
    private final int buttonX;
    private final int yStart;
    private final int maxVisible;

    private final Arrow arrowUp;
    private final Arrow arrowDown;
    private final Consumer<AbstractWidget> widgetAdder;
    private final Consumer<AbstractWidget> widgetRemover;

    private final List<ResourceKey<Level>> allDimensions = new ArrayList<>();
    private final List<DimensionBookmarkButton> visibleButtons = new ArrayList<>();
    private int scrollOffset = 0;
    @Nullable
    private ResourceKey<Level> selectedDimension;

    DimensionListPanel(AtlasOverviewScreen screen,
                       int bookRight, int bookTop, int bookHeight,
                       Consumer<AbstractWidget> widgetAdder,
                       Consumer<AbstractWidget> widgetRemover) {
        this.screen = screen;
        this.buttonX = bookRight - 10;
        this.yStart = bookTop + 15;
        // Leave room at the bottom for the slice bookmark widget (~36px)
        this.maxVisible = Math.max(1, (bookHeight - 50) / SEPARATION);
        this.widgetAdder = widgetAdder;
        this.widgetRemover = widgetRemover;

        // Center arrow over the dimension buttons (buttons at buttonX, width 24, center = buttonX+12)
        int arrowX = buttonX + 8;
        this.arrowUp = new Arrow(false, arrowX, yStart - 7, screen, this);
        this.arrowDown = new Arrow(true, arrowX, yStart + maxVisible * SEPARATION - 1, screen, this);

        widgetAdder.accept(arrowUp);
        widgetAdder.accept(arrowDown);
        arrowUp.visible = false;
        arrowUp.active = false;
        arrowDown.visible = false;
        arrowDown.active = false;
    }

    void build(Collection<ResourceKey<Level>> dimensions) {
        clearVisible();
        allDimensions.clear();
        allDimensions.addAll(dimensions.stream().sorted(Comparator.comparingInt(e -> {
            var s = e.location().toString();
            return MapAtlasesClient.DIMENSION_TEXTURE_ORDER.contains(s)
                    ? MapAtlasesClient.DIMENSION_TEXTURE_ORDER.indexOf(s) : 999;
        })).toList());
        scrollOffset = 0;
        refreshVisible();
    }

    void setSelectedDimension(ResourceKey<Level> dimension) {
        this.selectedDimension = dimension;
        for (var btn : visibleButtons) {
            btn.setSelected(btn.getDimension().equals(dimension));
        }
    }

    void scrollUp() {
        if (scrollOffset > 0) {
            scrollOffset--;
            refreshVisible();
        }
    }

    void scrollDown() {
        if (scrollOffset + maxVisible < allDimensions.size()) {
            scrollOffset++;
            refreshVisible();
        }
    }

    boolean canScrollUp() {
        return scrollOffset > 0;
    }

    boolean canScrollDown() {
        return scrollOffset + maxVisible < allDimensions.size();
    }

    private void clearVisible() {
        for (var btn : visibleButtons) {
            widgetRemover.accept(btn);
        }
        visibleButtons.clear();
    }

    private void refreshVisible() {
        clearVisible();
        boolean needsScroll = allDimensions.size() > maxVisible;
        arrowUp.visible = needsScroll;
        arrowUp.active = needsScroll;
        arrowDown.visible = needsScroll;
        arrowDown.active = needsScroll;

        int end = Math.min(scrollOffset + maxVisible, allDimensions.size());
        for (int i = scrollOffset; i < end; i++) {
            int localIndex = i - scrollOffset;
            ResourceKey<Level> dim = allDimensions.get(i);
            DimensionBookmarkButton btn = new DimensionBookmarkButton(
                    buttonX,
                    yStart + localIndex * SEPARATION,
                    dim, screen);
            btn.setSelected(selectedDimension != null && dim.equals(selectedDimension));
            widgetAdder.accept(btn);
            visibleButtons.add(btn);
        }
    }

    // ── Scroll arrow widget ───────────────────────────────────────────────

    private static class Arrow extends BookmarkButton {

        private final DimensionListPanel panel;
        private final boolean down;
        private final ResourceLocation inactiveSprite;

        Arrow(boolean down, int x, int y, AtlasOverviewScreen screen, DimensionListPanel panel) {
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

    }
}