package pepjebs.mapatlases.integration;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import pepjebs.mapatlases.MapAtlasesMod;

import java.util.List;

public class TrinketsCompat {

    public static ItemStack getAtlasInTrinket(Player player) {
        TrinketComponent trinket = TrinketsApi.getTrinketComponent(player).orElse(null);
        if (trinket != null) {
            List<Tuple<SlotReference, ItemStack>> found = trinket.getEquipped(MapAtlasesMod.MAP_ATLAS.get());
            if (!found.isEmpty()) return found.get(0).getB();
        }
        return ItemStack.EMPTY;
    }
}
