package pepjebs.mapatlases.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.CuriosCompat;

@Mixin(value = MapItemSavedData.class, priority = 1100)
public class MapDataMixin {

    @ModifyExpressionValue(
            method ="tickCarriedBy",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;contains(Lnet/minecraft/world/item/ItemStack;)Z")
    )
    private boolean containsProxy(boolean original, Player player, ItemStack stack) {
        return original || MapAtlasesMod.containsHack() || (MapAtlasesMod.CURIOS && CuriosCompat.getAtlasInCurio(player) == stack);
    }
}