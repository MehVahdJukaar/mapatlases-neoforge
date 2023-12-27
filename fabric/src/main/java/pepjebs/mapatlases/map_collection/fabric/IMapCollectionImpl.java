package pepjebs.mapatlases.map_collection.fabric;

import dev.onyxstudios.cca.api.v3.item.ItemComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.map_collection.IMapCollection;
import pepjebs.mapatlases.map_collection.MapCollection;

import java.util.Optional;

// For fabric.
// Or use cardinal components.
// Less optimized as it deserializes the stuff every time but at least doesn't have syncing issues
public class IMapCollectionImpl extends ItemComponent {

    @Nullable
    private MapCollection instance = null;

    public IMapCollectionImpl(ItemStack stack) {
        super(stack);
    }

    public static IMapCollection get(ItemStack stack, Level level) {
        Optional<IMapCollectionImpl> resolve = CCStuff.MAP_COLLECTION_COMPONENT.maybeGet(stack);
        if (resolve.isEmpty()) {
            throw new AssertionError("Map Atlas cca was empty. How is this possible? Culprit itemstack " + stack);
        }
        IMapCollectionImpl cap = resolve.get();
        return cap.getOrCreateInstance(level);
    }

    public MapCollection getOrCreateInstance(Level level) {
        if (instance == null) {
            instance = new MapCollection() {
                @Override
                public boolean isInitialized() {
                    return super.isInitialized();
                }
            };
            instance.initialize(level);
        }
        return instance;
    }


}
