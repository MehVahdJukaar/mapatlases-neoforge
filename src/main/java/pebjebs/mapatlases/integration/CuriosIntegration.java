package pebjebs.mapatlases.integration;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import pebjebs.mapatlases.MapAtlasesMod;
import pebjebs.mapatlases.mixin.plugin.MapAtlasesMixinPlugin;

public class CuriosIntegration {

    @Nullable
    public static ItemStack getAtlasInCurio(Player player){
        return null;
        /*
    }
                    && MapAtlasesMixinPlugin.isTrinketsLoaded()
                            && TrinketsApi.getTrinketComponent(entity).isPresent()
                && TrinketsApi.getTrinketComponent(entity).get().getEquipped(MapAtlasesMod.MAP_ATLAS).size() > 0){
            itemStack = TrinketsApi.getTrinketComponent(entity)
                    .get().getEquipped(MapAtlasesMod.MAP_ATLAS).get(0).getRight();

         */
        }
}
