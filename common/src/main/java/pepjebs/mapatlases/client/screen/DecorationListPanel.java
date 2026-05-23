package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.utils.DecorationHolder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages the left-side decoration bookmark list with arrow-scroll when items overflow the book height.
 */
class DecorationListPanel {

    static final int SEPARATION = 17;
    private static final int ARROW_W = 8;
    private static final int ARROW_H = 5;

    private final AtlasOverviewScreen screen;
    // pX passed to DecorationBookmarkButton.of (button constructor shifts left by BUTTON_W internally)
    private final int decorX;
    private final int yStart;
    private final int maxVisible;

    private final Arrow arrowUp;
    private final Arrow arrowDown;
    private final Consumer<AbstractWidget> widgetAdder;
    private final Consumer<AbstractWidget> widgetRemover;

    private final List<DecorationHolder> allHolders = new ArrayList<>();
    private final List<DecorationBookmarkButton> visibleButtons = new ArrayList<>();
    private int scrollOffset = 0;

    DecorationListPanel(AtlasOverviewScreen screen,
                        int bookLeft, int bookTop, int bookHeight,
                        Consumer<AbstractWidget> widgetAdder,
                        Consumer<AbstractWidget> widgetRemover) {
        this.screen = screen;
        this.decorX = bookLeft + 10;
        this.yStart = bookTop + 15;
        this.maxVisible = Math.max(1, (bookHeight - 22) / SEPARATION);
        this.widgetAdder = widgetAdder;
        this.widgetRemover = widgetRemover;

        // Arrow X centered over the decoration buttons area (buttons are at decorX-24, width 24, center = decorX-12)
        int arrowX = decorX - 16;
        this.arrowUp = new Arrow(false, arrowX, yStart - 7, screen, this);
        this.arrowDown = new Arrow(true, arrowX, yStart + maxVisible * SEPARATION - 1, screen, this);

        widgetAdder.accept(arrowUp);
        widgetAdder.accept(arrowDown);
        arrowUp.visible = false;
        arrowUp.active = false;
        arrowDown.visible = false;
        arrowDown.active = false;
    }

    void rebuild(List<DecorationHolder> holders) {
        clearVisible();
        allHolders.clear();
        allHolders.addAll(holders);
        scrollOffset = 0;
        refreshVisible();
    }

    void scrollUp() {
        if (scrollOffset > 0) {
            scrollOffset--;
            refreshVisible();
        }
    }

    void scrollDown() {
        if (scrollOffset + maxVisible < allHolders.size()) {
            scrollOffset++;
            refreshVisible();
        }
    }

    List<DecorationBookmarkButton> getVisibleButtons() {
        return visibleButtons;
    }

    void updateVisible(int currentXCenter, int currentZCenter, float radius, boolean followingPlayer) {
        if (visibleButtons.isEmpty()) return;
        float minX = currentXCenter - radius;
        float maxX = currentXCenter + radius;
        float minZ = currentZCenter - radius;
        float maxZ = currentZCenter + radius;

        List<Pair<Double, DecorationBookmarkButton>> byDistance = followingPlayer ? new ArrayList<>() : null;
        for (var btn : visibleButtons) {
            double x = btn.getWorldX();
            double z = btn.getWorldZ();
            if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) btn.setSelected(true);
            if (byDistance != null) {
                byDistance.add(Pair.of(Mth.square(x - currentXCenter) + Mth.square(z - currentZCenter), btn));
            }
        }
        if (byDistance != null) {
            byDistance.sort(Comparator.comparingDouble(Pair::getFirst));
            int index = 0;
            for (var e : byDistance) {
                var btn = e.getSecond();
                btn.setY(yStart + index * SEPARATION);
                btn.setIndex(index);
                index++;
            }
        }
    }

    boolean canScrollUp() {
        return scrollOffset > 0;
    }

    boolean canScrollDown() {
        return scrollOffset + maxVisible < allHolders.size();
    }

    private void clearVisible() {
        for (var btn : visibleButtons) {
            widgetRemover.accept(btn);
        }
        visibleButtons.clear();
    }

    private void refreshVisible() {
        clearVisible();
        boolean needsScroll = allHolders.size() > maxVisible;
        arrowUp.visible = needsScroll;
        arrowUp.active = needsScroll;
        arrowDown.visible = needsScroll;
        arrowDown.active = needsScroll;

        int end = Math.min(scrollOffset + maxVisible, allHolders.size());
        List<DecorationBookmarkButton> batch = new ArrayList<>();
        for (int i = scrollOffset; i < end; i++) {
            int localIndex = i - scrollOffset;
            DecorationBookmarkButton btn = DecorationBookmarkButton.of(
                    decorX,
                    yStart + localIndex * SEPARATION,
                    allHolders.get(i),
                    screen);
            btn.setIndex(localIndex);
            batch.add(btn);
        }
        // Sort by batch group to minimise texture swaps during rendering
        batch.sort(Comparator.comparingInt(DecorationBookmarkButton::getBatchGroup));
        for (var btn : batch) {
            widgetAdder.accept(btn);
            visibleButtons.add(btn);
        }
    }

    // ── Scroll arrow widget ───────────────────────────────────────────────

    private static class Arrow extends BookmarkButton {

        private final DecorationListPanel panel;
        private final boolean down;
        private final ResourceLocation inactiveSprite;

        Arrow(boolean down, int x, int y, AtlasOverviewScreen screen, DecorationListPanel panel) {
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