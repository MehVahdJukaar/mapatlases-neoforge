package pepjebs.mapatlases.client.screen;

import net.mehvahdjukaar.candlelight.api.VirtualOverride;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
    private static final int RESORT_DISTANCE_SQ = 16 * 16;

    private final int decorX;

    private final List<DecorationHolder> allHolders = new ArrayList<>();
    private List<DecorationHolder> displayList = List.of();
    private String filterText = "";
    private int lastSortCx = Integer.MIN_VALUE, lastSortCz = Integer.MIN_VALUE;
    private int lastViewCx, lastViewCz;
    private float lastViewRadius = -1;
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

        if (MapAtlasesClientConfig.filterButton.get()) {
            this.filter = new FilterButton(bookLeft - 14, bookTop + bookHeight - 10);
            widgetAdder.accept(filter);
        }
    }

    @Override
    protected int totalCount() {
        return displayList.size();
    }

    @Override
    protected void createVisibleWidgets(int from, int to) {
        for (int i = from; i < to; i++) {
            int localIndex = i - from;
            var btn = new DecorationBookmarkButton(decorX, yStart + localIndex * separation, localIndex,
                    displayList.get(i), screen);
            if (lastViewRadius >= 0) btn.setSelected(isInView(btn));
            widgetAdder.accept(btn);
            visibleButtons.add(btn);
        }
    }

    void rebuild(List<DecorationHolder> holders) {
        allHolders.clear();
        allHolders.addAll(holders);
        lastSortCx = Integer.MIN_VALUE;
        scrollOffset = 0;
        refreshVisible();
    }

    @Override
    protected void refreshVisible() {
        if (filterText.isEmpty()) {
            displayList = allHolders;
        } else {
            displayList = allHolders.stream()
                    .filter(h -> h.sortingString().contains(filterText))
                    .toList();
        }
        if (filter != null) filter.updateActiveState(totalCount() > maxVisible);
        super.refreshVisible();
    }

    void markInView(int cx, int cz, float radius) {
        lastViewCx = cx;
        lastViewCz = cz;
        lastViewRadius = radius;

        int dx = cx - lastSortCx, dz = cz - lastSortCz;
        if (dx * dx + dz * dz > RESORT_DISTANCE_SQ) {
            sortHolders(cx, cz);
            lastSortCx = cx;
            lastSortCz = cz;
            markRefreshPending();
        }

        for (var btn : visibleButtons) {
            btn.setSelected(isInView(btn));
        }
    }

    private boolean isInView(DecorationBookmarkButton btn) {
        double x = btn.getWorldX(), z = btn.getWorldZ();
        return x >= lastViewCx - lastViewRadius && x <= lastViewCx + lastViewRadius
                && z >= lastViewCz - lastViewRadius && z <= lastViewCz + lastViewRadius;
    }

    void applyFilter(String text) {
        filterText = text.toLowerCase(Locale.ROOT);
        scrollOffset = 0;
        refreshVisible();
    }

    boolean hasActiveFilter() {
        return !filterText.isEmpty();
    }

    private void sortHolders(int cx, int cz) {
        if (MapAtlasesClientConfig.sortBookmarks.get()) {
            allHolders.sort(Comparator.comparingDouble(h -> h.decorationDistSq(cx, cz)));
        } else {
            allHolders.sort(Comparator.comparing(DecorationHolder::sortingString));
        }
    }

    private final class FilterButton extends AtlasButton {

        FilterButton(int x, int y) {
            super(x, y, 8, 8, screen,
                    MapAtlasesClient.FILTER_SPRITE,
                    MapAtlasesClient.FILTER_HOVERED_SPRITE);
        }

        @Override
        public ResourceLocation getSprite() {
            if (hasActiveFilter()) return MapAtlasesClient.FILTER_ACTIVE_SPRITE;
            return isHovered ? selectedSprite : sprite;
        }

        @Override
        protected float zOffset() {
            return 2;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            super.mouseClicked(mouseX, mouseY, button);
            return false; //never focus this widget.
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (hasActiveFilter()) applyFilter("");
            else screen.setFilterBoxState(true);
        }

        @VirtualOverride("neoforge")
        public void onClick(double mouseX, double mouseY, int button) {
            onClick(mouseX, mouseY);
        }

        @Nullable
        @Override
        public Tooltip getTooltip() {
            if (!visible || !active) return null;
            if (hasActiveFilter()) return Tooltip.create(Component.literal("\"" + filterText + "\""));
            return Tooltip.create(Component.translatable("tooltip.map_atlases.filter"));
        }

        void updateActiveState(boolean needsScroll) {
            if (hasActiveFilter()) return;
            this.active = needsScroll;
            this.visible = needsScroll;
        }
    }
}
