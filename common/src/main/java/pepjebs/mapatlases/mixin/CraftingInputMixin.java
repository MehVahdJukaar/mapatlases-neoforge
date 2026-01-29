package pepjebs.mapatlases.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.CraftingInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import pepjebs.mapatlases.utils.ICraftingInputWithContext;

@Mixin(CraftingInput.class)
public class CraftingInputMixin implements ICraftingInputWithContext {

    @Unique
    private AbstractContainerMenu mapAtlases$menu;

    @Override
    public void mapAtlases$setMenu(AbstractContainerMenu menu) {
        this.mapAtlases$menu = menu;
    }

    @Override
    public AbstractContainerMenu mapAtlases$getMenu() {
        return this.mapAtlases$menu;
    }

}
