package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.candlelight.api.VirtualOverride;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
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
    // Tracks last map-centre used for sorting; MIN_VALUE forces a sort on the first markInView call.
    private int lastSortCx = Integer.MIN_VALUE, lastSortCz = Integer.MIN_VALUE;
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
        for (int i = from; i < to; i++) {
            int localIndex = i - from;
            DecorationBookmarkButton btn = DecorationBookmarkButton.of(
                    decorX,
                    yStart + localIndex * separation,
                    displayList.get(i),
                    screen);
            btn.setIndex(localIndex);
            widgetAdder.accept(btn);
            visibleButtons.add(btn);
        }
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Replace the full decoration list. The list will be sorted by map-centre distance
     * on the next {@link #markInView} call, which has the correct coordinates.
     */
    void rebuild(List<DecorationHolder> holders) {
        allHolders.clear();
        allHolders.addAll(holders);
        lastSortCx = Integer.MIN_VALUE; // force re-sort on next markInView
        scrollOffset = 0;
        refreshVisible();
    }

    /**
     * Recompute displayList (applying any active filter) then rebuild the visible widget window.
     */
    @Override
    protected void refreshVisible() {
        if (filterText.isEmpty()) {
            displayList = allHolders;
        } else {
            String filterLower = filterText.toLowerCase(Locale.ROOT);
            displayList = allHolders.stream()
                    .filter(h -> h.matchesFilter(filterLower))
                    .toList();
        }
        if (filter != null) filter.updateActiveState(totalCount() > maxVisible);
        super.refreshVisible();
    }

    /**
     * Called every render frame by MapWidget with the current map-centre coordinates.
     * <p>
     * Re-sorts {@code allHolders} by distance to the map centre whenever the centre
     * moves more than 16 blocks, then schedules a widget-window rebuild via
     * {@link #markRefreshPending()} (applied safely in the next {@code tick()} via
     * {@link #flush()}, outside the render loop).
     * Also marks visible buttons as selected if their decoration is in the current view.
     */
    void markInView(int cx, int cz, float radius) {
        // Re-sort if the map center has shifted significantly
        int dx = cx - lastSortCx, dz = cz - lastSortCz;
        if (dx * dx + dz * dz > 256) { // > 16 blocks
            allHolders.sort(Comparator.comparingDouble(h -> decorationDistSq(h, cx, cz)));
            lastSortCx = cx;
            lastSortCz = cz;
            markRefreshPending(); // deferred — applied in tick(), safe outside render loop
        }

        // Highlight buttons whose decoration lies within the visible map rectangle
        if (visibleButtons.isEmpty()) return;
        float minX = cx - radius, maxX = cx + radius;
        float minZ = cz - radius, maxZ = cz + radius;
        for (var btn : visibleButtons) {
            double x = btn.getWorldX(), z = btn.getWorldZ();
            if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) btn.setSelected(true);
        }
    }

    /** Apply a text filter; empty string clears it. */
    void applyFilter(String text) {
        filterText = text.toLowerCase(Locale.ROOT);
        scrollOffset = 0;
        refreshVisible();
    }

    void clearFilter() {
        applyFilter("");
    }

    boolean hasActiveFilter() {
        return !filterText.isEmpty();
    }


    /**
     * Squared world-space distance between a decoration and (px, pz).
     * Uses the exact decoration position for vanilla markers; falls back to map centre for custom ones.
     */
    private static double decorationDistSq(DecorationHolder h, double px, double pz) {
        var d = h.data().data;
        double wx = d.centerX, wz = d.centerZ;
        if (h.deco() instanceof MapDecoration md) {
            // getDecorationPos simplifies to -(1 << scale) * coord / 2
            int scale = 1 << d.scale;
            wx += scale * md.x() / 2.0;
            wz += scale * md.y() / 2.0;
        }
        return Mth.square(wx - px) + Mth.square(wz - pz);
    }

    // ── Filter button ──────────────────────────────────────────────────────

    private final class FilterButton extends AtlasButton {

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
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            super.mouseClicked(mouseX, mouseY, button);
            return false; //never focus this widget.
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (hasActiveFilter()) clearFilter();
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

        public void updateActiveState(boolean needsScroll) {
            if (hasActiveFilter()) return;
            this.active = needsScroll;
            this.visible = needsScroll;
        }
    }
}