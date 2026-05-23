package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.mehvahdjukaar.candlelight.api.VirtualOverride;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.utils.DecorationHolder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

class DecorationListPanel extends BookmarkListPanel<DecorationBookmarkButton> {

    static final int SEPARATION = 17;
    private final int decorX;

    private final List<DecorationHolder> allHolders = new ArrayList<>();
    private List<DecorationHolder> displayList = List.of();
    private String filterText = "";
    @Nullable
    private FilterButton filter;

    DecorationListPanel(AtlasOverviewScreen screen,
                        int bookLeft, int bookTop, int bookHeight,
                        Consumer<AbstractWidget> widgetAdder,
                        Consumer<AbstractWidget> widgetRemover) {
        super(screen, bookLeft - 6, bookTop + 15,
                Math.max(1, (bookHeight - 22) / SEPARATION), SEPARATION,
                widgetAdder, widgetRemover);
        this.decorX = bookLeft + 10;

        // Filter button: bottom-left of the decoration panel
        if (MapAtlasesClientConfig.filterButton.get()) {
            this.filter = new FilterButton(bookLeft - 14, bookTop + bookHeight - 10);
            widgetAdder.accept(filter);
        }
    }

    // ── BookmarkListPanel contract ─────────────────────────────────────────

    @Override
    protected int totalCount() {
        return displayList.size();
    }

    @Override
    protected void createVisibleWidgets(int from, int to) {
        List<DecorationBookmarkButton> batch = new ArrayList<>();
        for (int i = from; i < to; i++) {
            int localIndex = i - from;
            DecorationBookmarkButton btn = DecorationBookmarkButton.of(
                    decorX,
                    yStart + localIndex * separation,
                    displayList.get(i),
                    screen);
            btn.setIndex(localIndex);
            batch.add(btn);
        }
        batch.sort(Comparator.comparingInt(DecorationBookmarkButton::getBatchGroup));
        for (var btn : batch) {
            widgetAdder.accept(btn);
            visibleButtons.add(btn);
        }
    }

    // ── Public API ─────────────────────────────────────────────────────────

    void rebuild(List<DecorationHolder> holders) {
        allHolders.clear();
        allHolders.addAll(holders);
        recomputeDisplayList();
        scrollOffset = 0;
        refreshVisible();
    }

    @Override
    protected void refreshVisible() {
        super.refreshVisible();

        boolean needsScroll = totalCount() > maxVisible;
        if (filter != null) this.filter.active = needsScroll;
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
                btn.setY(yStart + index * separation);
                btn.setIndex(index);
                index++;
            }
        }
    }

    /**
     * Apply a text filter; empty string clears the filter.
     */
    void applyFilter(String text) {
        filterText = text.toLowerCase(Locale.ROOT);
        scrollOffset = 0;
        recomputeDisplayList();
        refreshVisible();
    }

    void clearFilter() {
        applyFilter("");
    }

    boolean hasActiveFilter() {
        return !filterText.isEmpty();
    }

    // ── Internal ───────────────────────────────────────────────────────────

    private void recomputeDisplayList() {
        if (filterText.isEmpty()) {
            displayList = allHolders;
        } else {
            displayList = allHolders.stream()
                    .filter(h -> DecorationBookmarkButton.getSearchText(h).contains(filterText))
                    .toList();
        }
    }

    // ── Filter button ──────────────────────────────────────────────────────

    private final class FilterButton extends BookmarkButton {

        private final ResourceLocation activeSprite = MapAtlasesClient.FILTER_ACTIVE_SPRITE;

        FilterButton(int x, int y) {
            super(x, y, 8, 8, screen,
                    MapAtlasesClient.FILTER_SPRITE,
                    MapAtlasesClient.FILTER_HOVERED_SPRITE);
            setSelected(false);
        }

        @Override
        public ResourceLocation getSprite() {
            if (hasActiveFilter()) return activeSprite;
            return isHovered ? selectedSprite : sprite;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
            if (!visible) return;
            PoseStack pose = g.pose();
            pose.pushPose();
            pose.translate(0, 0, 2);
            RenderSystem.enableDepthTest();
            g.blitSprite(getSprite(), getX(), getY(), width, height);
            pose.popPose();
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (hasActiveFilter()) clearFilter();
            else screen.openFilterBox();
        }


        @VirtualOverride("neoforge")
        public void onClick(double mouseX, double mouseY, int button) {
            onClick(mouseX, mouseY);
        }
    }
}