package pepjebs.mapatlases.map_collection.forge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.INBTSerializable;
import pepjebs.mapatlases.map_collection.IMapCollection;
import pepjebs.mapatlases.map_collection.MapCollection;

import java.util.Optional;

// The porpoise of this object is to save a datastructures with all available maps so we dont have to keep deserializing nbt
public class IMapCollectionImpl extends MapCollection implements  INBTSerializable<CompoundTag> {

    public static IMapCollection get(ItemStack stack, Level level) {
        Optional<IMapCollectionImpl> resolve = stack.getCapability(CapStuff.ATLAS_CAP_TOKEN, null).resolve();
        if (resolve.isEmpty()) {
            throw new AssertionError("Map Atlas capability was empty. How is this possible? Culprit itemstack " + stack);
        }
        IMapCollectionImpl cap = resolve.get();
        if (!cap.isInitialized()) cap.initialize(level);
        return cap;
    }

}
