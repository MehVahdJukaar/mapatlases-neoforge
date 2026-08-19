package pepjebs.mapatlases.integration;

import net.mehvahdjukaar.candlelight.api.PlatformImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class CuriosCompat {

    @PlatformImpl
    public static ItemStack getAtlasInCurio(Player player) {
        throw new AssertionError();
    }
}
