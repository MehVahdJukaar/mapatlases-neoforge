package pepjebs.mapatlases.utils;

import net.minecraft.world.item.ItemStack;

public interface AtlasCartographyTable {

    int mapatlases$getSelectedMapIndex();

    void mapatlases$setSelectedMapIndex(int index);

    void mapatlases$removeSelectedMap(ItemStack atlas);
}
