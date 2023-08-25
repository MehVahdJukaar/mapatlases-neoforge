/**
 * This class was forked from:
 * https://github.com/AntiqueAtlasTeam/AntiqueAtlas/blob/37038a399ecac1d58bcc7164ef3d309e8636a2cb/src/main/java
 *      /hunternif/mc/impl/atlas/mixin/prod/MixinCartographyTableHandlerSlot.java
 * Under the GPL-3 license.
 */
package pepjebs.mapatlases.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.CartographyTableScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

@Mixin(targets = "net.minecraft.screen.CartographyTableScreenHandler$3")
class MixinCartographyTableScreenHandlerFirstSlot {

    @Inject(method = "canInsert", at = @At("RETURN"), cancellable = true)
    void mapAtlasCanInsert(ItemStack stack, CallbackInfoReturnable<Boolean> info) {
        info.setReturnValue(stack.getItem() == MapAtlasesMod.MAP_ATLAS || stack.getItem() ==  Items.BOOK ||
                info.getReturnValueZ());
    }
}

@Mixin(targets = "net.minecraft.screen.CartographyTableScreenHandler$4")
class MixinCartographyTableScreenHandlerSecondSlot {

    @Inject(method = "canInsert", at = @At("RETURN"), cancellable = true)
    void mapAtlasCanInsert(ItemStack stack, CallbackInfoReturnable<Boolean> info) {
        info.setReturnValue(stack.getItem() == MapAtlasesMod.MAP_ATLAS || stack.getItem() ==  Items.FILLED_MAP ||
                info.getReturnValueZ());
    }
}

@Mixin(targets = "net.minecraft.screen.CartographyTableScreenHandler$5")
class MixinCartographyTableScreenHandlerSecondSlotMaps  {

    CartographyTableScreenHandler cartographyHandler;

    @Inject(method = "<init>", at = @At("TAIL"))
    void mapAtlasInit(CartographyTableScreenHandler handler, Inventory inventory, int index, int x, int y, ScreenHandlerContext context, CallbackInfo info) {
        cartographyHandler = handler;
    }

    @Inject(method = "onTakeItem", at = @At("HEAD"))
    void mapAtlasOnTakeItem(PlayerEntity player, ItemStack stack, CallbackInfo info) {
        ItemStack atlas = cartographyHandler.slots.get(0).getStack();
        Slot slotOne = cartographyHandler.slots.get(1);
        if (cartographyHandler.slots.get(0).getStack().getItem() == MapAtlasesMod.MAP_ATLAS
                && (slotOne.getStack().getItem() == Items.MAP
                || (MapAtlasesMod.CONFIG.acceptPaperForEmptyMaps && slotOne.getStack().getItem() == Items.PAPER))) {
            int amountToTake = MapAtlasesAccessUtils.getMapCountToAdd(atlas, slotOne.getStack());
            // onTakeItem already calls takeStack(1) so we subtract that out
            slotOne.takeStack(amountToTake - 1);
        } else if (cartographyHandler.slots.get(0).getStack().getItem() == MapAtlasesMod.MAP_ATLAS
                && slotOne.getStack().getItem() == Items.FILLED_MAP) {
            slotOne.takeStack(1);
        }
    }
}