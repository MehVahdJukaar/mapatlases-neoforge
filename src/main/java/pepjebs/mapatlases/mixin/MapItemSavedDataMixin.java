package pepjebs.mapatlases.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.CuriosCompat;

@Mixin(value = MapItemSavedData.class, priority = 1100)
public class MapItemSavedDataMixin {

    @WrapOperation(
            method = "tickCarriedBy",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;contains(Lnet/minecraft/world/item/ItemStack;)Z")
    )
    private boolean containsProxy(Inventory instance, ItemStack stack, Operation<Boolean> contains, @Local Player player) {
        InteractionResult interactionResult = MapAtlasesMod.containsHack();
        if (interactionResult == InteractionResult.FAIL) return false;
        else if (interactionResult.consumesAction()) return true;
        return contains.call(instance, stack) || (MapAtlasesMod.CURIOS && CuriosCompat.getAtlasInCurio(player) == stack);
    }
}
