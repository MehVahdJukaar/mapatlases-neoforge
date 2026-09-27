package pepjebs.mapatlases.platform;

import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;

public class PlatStuffImpl {
    public static boolean isShear(ItemStack bottomItem) {
        return bottomItem.is(Tags.Items.TOOLS_SHEAR);
    }

    public static boolean isBoss(EntityType<?> type) {
        return type.builtInRegistryHolder().is(Tags.EntityTypes.BOSSES);
    }

    public static Pair<Boolean, Vec3> fireTeleportEvent(ServerPlayer player, double pX, double pY, double pZ) {
        EntityTeleportEvent event = EventHooks.onEntityTeleportCommand(player, player.level(), pX, pY, pZ);
        return Pair.of(event.isCanceled(), new Vec3(event.getTargetX(), event.getTargetY(), event.getTargetZ()));
    }
}
