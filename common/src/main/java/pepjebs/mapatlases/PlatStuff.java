package pepjebs.mapatlases;

import com.mojang.datafixers.util.Pair;
import net.mehvahdjukaar.candlelight.api.PlatformImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;

public class PlatStuff {
    @Contract
    @PlatformImpl
    public static boolean isShear(ItemStack bottomItem) {
        throw new AssertionError();
    }

    @PlatformImpl
    public static boolean isBoss(EntityType<?> type) {
        throw new AssertionError();
    }

    @PlatformImpl
    public static Pair<Boolean, Vec3> fireTeleportEvent(ServerPlayer player, double pX, double pY, double pZ) {
        throw new AssertionError();
    }
}
