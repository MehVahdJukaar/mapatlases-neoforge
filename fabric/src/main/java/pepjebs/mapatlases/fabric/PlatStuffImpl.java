package pepjebs.mapatlases.fabric;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PlatStuffImpl {
    public static boolean isShear(ItemStack bottomItem) {
        return bottomItem.is(Items.SHEARS);
    }

    public static boolean isBoss(EntityType<?> type) {
        return type == EntityType.WARDEN || type == EntityType.ENDER_DRAGON || type ==EntityType.ELDER_GUARDIAN || type == EntityType.WITHER;
    }
}
