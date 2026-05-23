package pepjebs.mapatlases.client.screen;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.client.MapAtlasesClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

class DimensionListPanel extends BookmarkListPanel<DimensionBookmarkButton> {

    private static final int SEPARATION = 22;
    private final int buttonX;
    private final List<ResourceKey<Level>> allDimensions = new ArrayList<>();
    @Nullable
    private ResourceKey<Level> selectedDimension;

    DimensionListPanel(AtlasOverviewScreen screen,
                       int bookRight, int bookTop, int bookHeight,
                       Consumer<AbstractWidget> widgetAdder,
                       Consumer<AbstractWidget> widgetRemover) {
        super(screen, bookRight - 2, bookTop + 15,
                Math.max(1, (bookHeight - 50) / SEPARATION), SEPARATION,
                widgetAdder, widgetRemover);
        this.buttonX = bookRight - 10;
    }

    @Override
    protected int totalCount() {
        return allDimensions.size();
    }

    @Override
    protected void createVisibleWidgets(int from, int to) {
        for (int i = from; i < to; i++) {
            int localIndex = i - from;
            ResourceKey<Level> dim = allDimensions.get(i);
            DimensionBookmarkButton btn = new DimensionBookmarkButton(
                    buttonX,
                    yStart + localIndex * separation,
                    dim, screen);
            btn.setSelected(selectedDimension != null && dim.equals(selectedDimension));
            widgetAdder.accept(btn);
            visibleButtons.add(btn);
        }
    }

    void build(Collection<ResourceKey<Level>> dimensions) {
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
}
