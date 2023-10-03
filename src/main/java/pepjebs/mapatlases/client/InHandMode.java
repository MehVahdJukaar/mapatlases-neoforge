package pepjebs.mapatlases.client;

import net.minecraft.world.item.ItemStack;
import pepjebs.mapatlases.item.MapAtlasItem;

public enum InHandMode {
    ON, NOT_LOCKED, OFF;

    public boolean isOn(ItemStack stack) {
        return switch (this) {
            case OFF -> false;
            case ON -> true;
            case NOT_LOCKED -> !MapAtlasItem.isLocked(stack);
        };
    }
}
