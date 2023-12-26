package pepjebs.mapatlases.forge;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.Tags;

public class PlatStuffImpl {
    public static boolean isShear(ItemStack bottomItem) {
        return bottomItem.is(Tags.Items.SHEARS);
    }

    public static boolean isBoss(EntityType<?> type) {
        return         type.is( Tags.EntityTypes.BOSSES);
    }
}
