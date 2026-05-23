package pepjebs.mapatlases.client.screen;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.util.Mth;
import pepjebs.mapatlases.utils.DecorationHolder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

class DecorationListPanel extends BookmarkListPanel<DecorationBookmarkButton> {

    static final int SEPARATION = 17;
    private final int decorX;
    private final List<DecorationHolder> allHolders = new ArrayList<>();

    DecorationListPanel(AtlasOverviewScreen screen,
                        int bookLeft, int bookTop, int bookHeight,
                        Consumer<AbstractWidget> widgetAdder,
                        Consumer<AbstractWidget> widgetRemover) {
        super(screen, bookLeft - 6, bookTop + 15,
                Math.max(1, (bookHeight - 22) / SEPARATION), SEPARATION,
                widgetAdder, widgetRemover);
        this.decorX = bookLeft + 10;
    }

    @Override
    protected int totalCount() {
        return allHolders.size();
    }

    @Override
    protected void createVisibleWidgets(int from, int to) {
        List<DecorationBookmarkButton> batch = new ArrayList<>();
        for (int i = from; i < to; i++) {
            int localIndex = i - from;
            DecorationBookmarkButton btn = DecorationBookmarkButton.of(
                    decorX,
                    yStart + localIndex * separation,
                    allHolders.get(i),
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

    void rebuild(List<DecorationHolder> holders) {
        allHolders.clear();
        allHolders.addAll(holders);
        scrollOffset = 0;
        refreshVisible();
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
}
