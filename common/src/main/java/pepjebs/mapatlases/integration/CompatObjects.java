package pepjebs.mapatlases.integration;

import net.mehvahdjukaar.moonlight.api.misc.OptRegSupplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;
public class CompatObjects {

    public static final OptRegSupplier<Item> TF_MAGIC_MAP = tf("magic_map");
    public static final OptRegSupplier<Item> TF_FILLED_MAGIC_MAP = tf("filled_magic_map");
    public static final OptRegSupplier<Item> TF_MAZE_MAP = tf("maze_map");
    public static final OptRegSupplier<Item> TF_FILLED_MAZE_MAP = tf("filled_maze_map");
    public static final OptRegSupplier<Item> TF_ORE_MAP = tf("ore_map");
    public static final OptRegSupplier<Item> TF_FILLED_ORE_MAP = tf("filled_ore_map");
    public static final OptRegSupplier<Item> SUPPLEMENTARIES_SLICE_MAP = supp("slice_map");

    private static OptRegSupplier<Item> tf(String id) {
        return modItem("twilightforest", id);
    }

    private static OptRegSupplier<Item> supp(String id) {
        return modItem("supplementaries", id);
    }

    private static OptRegSupplier<Item> modItem(String namespace, String id) {
        return OptRegSupplier.of(ResourceLocation.fromNamespaceAndPath(namespace, id), BuiltInRegistries.ITEM);
    }
}
