package pepjebs.mapatlases;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

public class PlatStuff {
    @ExpectPlatform
    public static boolean isShear(ItemStack bottomItem) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isBoss(EntityType<?> type) {
        throw new AssertionError();
    }
}
