package pepjebs.mapatlases.integration.platform;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class TrinketsCompatImpl {
    public static ItemStack getAtlasInTrinket(Player player) {
        //TODO: trinkets
        /*
        TrinketComponent trinket = TrinketsApi.getTrinketComponent(player).orElse(null);
        if (trinket != null) {
            var found =
                    trinket.getEquipped(MapAtlasesMod.MAP_ATLAS.get());
            if (!found.isEmpty()) return found.getFirst().getB();
        }
        */
        return ItemStack.EMPTY;
    }
}
