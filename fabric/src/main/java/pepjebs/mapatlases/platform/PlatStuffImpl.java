package pepjebs.mapatlases.platform;

import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class PlatStuffImpl {
    public static boolean isShear(ItemStack bottomItem) {
        return bottomItem.is(ConventionalItemTags.SHEAR_TOOLS);
    }

    public static boolean isBoss(EntityType<?> type) {
        return type == EntityType.WARDEN || type == EntityType.ENDER_DRAGON || type == EntityType.ELDER_GUARDIAN || type == EntityType.WITHER;
    }

    public static Pair<Boolean, Vec3> fireTeleportEvent(ServerPlayer player, double pX, double pY, double pZ) {
        return Pair.of(false, new Vec3(pX, pY, pZ));
    }
}
