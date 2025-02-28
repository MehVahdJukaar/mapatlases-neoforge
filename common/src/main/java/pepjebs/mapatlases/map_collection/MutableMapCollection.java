package pepjebs.mapatlases.map_collection;

import net.minecraft.world.level.Level;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.util.Collection;

public class MutableMapCollection extends MapCollection {

    protected MutableMapCollection(Collection<Integer> integers, Level level) {
        for (int i : integers) {
            add(i, level);
        }
    }

    protected ImmutableMapCollection toImmutable() {
        return new ImmutableMapCollection(ids);
    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    public boolean remove(MapDataHolder map) {
        assertInitialized();
        boolean success = ids.remove(map.id);
        if (maps.remove(map.makeKey()) != null) {
            dimensionSlices.clear();
            for (var j : maps.keySet()) {
                addToDimensionMap(j);
            }
        }
        return success;
    }

    public boolean add(int intId, Level level) {
        return this.addInternal(intId, level);
    }
}
