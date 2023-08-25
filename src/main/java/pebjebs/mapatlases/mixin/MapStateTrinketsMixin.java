package pebjebs.mapatlases.mixin;

import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = MapItemSavedData.class, priority = 1100)
public class MapStateTrinketsMixin {
/*
    @ModifyA(
            method ="tickCarriedBy",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;contains(Lnet/minecraft/world/item/ItemStack;)Z")
    )
    private boolean containsProxy(Inventory inventory, ItemStack stack) {
        return inventory.contains(stack) || (TrinketsApi.getTrinketComponent(inventory.player).isPresent()
                && TrinketsApi.getTrinketComponent(inventory.player).get()
                    .getEquipped(MapAtlasesMod.MAP_ATLAS).size() > 0);
    }*/
}
