package pepjebs.mapatlases.integration.platform;

import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import net.mehvahdjukaar.supplementaries.common.utils.SlotReference;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import pepjebs.mapatlases.MapAtlasesMod;

import java.util.List;

public class TrinketsCompatImpl {
    public static ItemStack getAtlasInTrinket(Player player) {
        TrinketComponent trinket = TrinketsApi.getTrinketComponent(player).orElse(null);
        if (trinket != null) {
            var found =
                    trinket.getEquipped(MapAtlasesMod.MAP_ATLAS.get());
            if (!found.isEmpty()) return found.getFirst().getB();
        }
        return ItemStack.EMPTY;
    }
}
