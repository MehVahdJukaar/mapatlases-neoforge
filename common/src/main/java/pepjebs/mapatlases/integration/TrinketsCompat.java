package pepjebs.mapatlases.integration;

import net.mehvahdjukaar.candlelight.api.PlatformImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class TrinketsCompat {

    @PlatformImpl
    public static ItemStack getAtlasInTrinket(Player player) {
        throw new AssertionError();
    }
}
