package pepjebs.mapatlases.integration.platform;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import pepjebs.mapatlases.MapAtlasesMod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Optional;

public class CuriosCompatImpl {

    public static ItemStack getAtlasInCurio(Player player) {
        Optional<ICuriosItemHandler> curiosInventory = CuriosApi.getCuriosInventory(player);
        if (!curiosInventory.isPresent())
            return ItemStack.EMPTY;

        Item atlas_item = MapAtlasesMod.MAP_ATLAS.get();
        for (ICurioStacksHandler stacksHandler : curiosInventory.get().getCurios().values()) {
            IDynamicStackHandler stacks = stacksHandler.getStacks();
            NonNullList<Boolean> activeStates = stacksHandler.getActiveStates();
            for (int i = 0; i < stacks.getSlots(); i++) {
                if (activeStates.size() > i && !activeStates.get(i)) {
                    continue;
                }
                ItemStack stack = stacks.getStackInSlot(i);
                if (stack.is(atlas_item)) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }
}
