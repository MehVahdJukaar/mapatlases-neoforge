/**
 * This class was forked from:
 * https://github.com/AntiqueAtlasTeam/AntiqueAtlas/blob/37038a399ecac1d58bcc7164ef3d309e8636a2cb/src/main/java
 *      /hunternif/mc/impl/atlas/mixin/prod/MixinCartographyTableHandlerSlot.java
 * Under the GPL-3 license.
 */
package pepjebs.mapatlases.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtilsOld;

@Mixin(targets = "net.minecraft.world.inventory.CartographyTableMenu$3")
class MixinCartographyTableHandlerFirstSlot {

    @Inject(method = "mayPlace", at = @At("RETURN"), cancellable = true)
    void mapAtlasCanInsert(ItemStack stack, CallbackInfoReturnable<Boolean> info) {
        info.setReturnValue(stack.is(MapAtlasesMod.MAP_ATLAS.get()) || stack.getItem() ==  Items.BOOK ||
                info.getReturnValueZ());

    }
}

@Mixin(targets = "net.minecraft.world.inventory.CartographyTableMenu$4")
class MixinCartographyTableAbstractContainerMenuSecondSlot {

    @Inject(method = "mayPlace", at = @At("RETURN"), cancellable = true)
    void mapAtlasCanInsert(ItemStack stack, CallbackInfoReturnable<Boolean> info) {
        info.setReturnValue(stack.is(MapAtlasesMod.MAP_ATLAS.get()) || stack.getItem() ==  Items.FILLED_MAP ||
                info.getReturnValueZ());
    }
}

@Mixin(targets = "net.minecraft.world.inventory.CartographyTableMenu$5")
class MixinCartographyTableAbstractContainerMenuSecondSlotMaps  {

    @Shadow @Final
    CartographyTableMenu this$0;

    @Inject(method = "onTake", at = @At("HEAD"))
    void mapAtlasOnTakeItem(Player player, ItemStack stack, CallbackInfo info) {
        ItemStack atlas = this$0.slots.get(0).getItem();
        Slot slotOne = this$0.slots.get(1);
        if (this$0.slots.get(0).getItem().is(MapAtlasesMod.MAP_ATLAS.get())
                && (slotOne.getItem().is( Items.MAP)
                || (MapAtlasesConfig.acceptPaperForEmptyMaps.get() && slotOne.getItem().is(Items.PAPER)))) {
            int amountToTake = MapAtlasesAccessUtilsOld.getMapCountToAdd(atlas, slotOne.getItem());
            // onTakeItem already calls takeStack(1) so we subtract that out
            slotOne.remove(amountToTake - 1);
        } else if (this$0.slots.get(0).getItem().is(MapAtlasesMod.MAP_ATLAS.get())
                && slotOne.getItem().is(Items.FILLED_MAP)) {
            slotOne.remove(1);
        }
    }
}