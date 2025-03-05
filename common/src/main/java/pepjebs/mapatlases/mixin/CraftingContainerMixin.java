package pepjebs.mapatlases.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.crafting.CraftingInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pepjebs.mapatlases.utils.ICraftingInputWithContext;

@Mixin(CraftingContainer.class)
public interface CraftingContainerMixin {

    @ModifyReturnValue(method = "asPositionedCraftInput", at = @At("RETURN"))
     default CraftingInput.Positioned mapAtlases$addContext(CraftingInput.Positioned original) {
        if (((Object) this) instanceof TransientCraftingContainer tc) {
            ((ICraftingInputWithContext) original.input()).mapAtlases$setMenu(tc.menu);
        }
        return original;
    }
}
