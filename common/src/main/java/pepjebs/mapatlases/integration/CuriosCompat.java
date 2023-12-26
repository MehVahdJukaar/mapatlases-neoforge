package pepjebs.mapatlases.integration;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import pepjebs.mapatlases.MapAtlasesMod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.List;

public class CuriosCompat {

    public static ItemStack getAtlasInCurio(Player player) {
        List<SlotResult> found = CuriosApi.getCuriosHelper().findCurios(player, MapAtlasesMod.MAP_ATLAS.get());
        if (!found.isEmpty()) {
            return found.get(0).stack();
        }
        return ItemStack.EMPTY;
    }
}
