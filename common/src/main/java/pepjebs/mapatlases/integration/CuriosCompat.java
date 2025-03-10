package pepjebs.mapatlases.integration;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import pepjebs.mapatlases.MapAtlasesMod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

public class CuriosCompat {

    public static ItemStack getAtlasInCurio(Player player) {
      return CuriosApi.getCuriosInventory(player)
                .flatMap(o -> o.findFirstCurio(MapAtlasesMod.MAP_ATLAS.get()))
              .map(SlotResult::stack).orElse(ItemStack.EMPTY);
    }
}
