/**
 * This class was forked from:
 * https://github.com/AntiqueAtlasTeam/AntiqueAtlas/blob/37038a399ecac1d58bcc7164ef3d309e8636a2cb/src/main/java
 * /hunternif/mc/impl/atlas/mixin/prod/MixinCartographyTableHandlerSlot.java
 * Under the GPL-3 license.
 */
package pepjebs.mapatlases.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.server.level.ServerPlayer;
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
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.PlatStuff;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.utils.AtlasCartographyTable;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

@Mixin(targets = "net.minecraft.world.inventory.CartographyTableMenu$3")
class MixinCartographyTableHandlerFirstSlot {

    @ModifyReturnValue(method = "mayPlace", at = @At("RETURN"))
    boolean mapAtlasCanInsert(boolean original, ItemStack stack) {
        return original || stack.is(MapAtlasesMod.MAP_ATLAS.get());

    }
}

@Mixin(targets = "net.minecraft.world.inventory.CartographyTableMenu$4")
class MixinCartographyTableAbstractContainerMenuSecondSlot {

    @ModifyReturnValue(method = "mayPlace", at = @At("RETURN"))
    boolean mapAtlasCanInsert(boolean original, ItemStack stack) {
        return original || stack.is(MapAtlasesMod.MAP_ATLAS.get()) ||
                MapAtlasesAccessUtils.isValidFilledMap(stack) ||
                PlatStuff.isShear(stack);
    }
}

@Mixin(targets = "net.minecraft.world.inventory.CartographyTableMenu$5")
class MixinCartographyTableAbstractContainerMenuSecondSlotMaps {

    @Shadow
    @Final
    CartographyTableMenu field_17303;

    @Inject(method = "onTake", at = @At("HEAD"))
    void mapAtlasOnTakeItem(Player player, ItemStack result, CallbackInfo info) {
        ItemStack atlas = field_17303.slots.get(0).getItem();
        Slot slotOne = field_17303.slots.get(1);
        if (atlas.is(MapAtlasesMod.MAP_ATLAS.get())) {
            ItemStack slotOneItem = slotOne.getItem();
            if (PlatStuff.isShear(slotOneItem)) {
                AtlasCartographyTable menu = (AtlasCartographyTable) this.field_17303;
                menu.mapatlases$removeSelectedMap(atlas);
                atlas.grow(1);
                slotOneItem.grow(1);
                if (player instanceof ServerPlayer sp) {
                    slotOneItem.hurtAndBreak(1, sp.serverLevel(), sp, b -> {
                    });
                }
                menu.mapatlases$setSelectedMapIndex(0);
            } else if (MapAtlasesAccessUtils.isValidEmptyMapIngredient(slotOneItem)) {
                var amountToTake = MapAtlasesAccessUtils.getMapCountToAdd(atlas, slotOneItem, player.level());
                // onTakeItem already calls takeStack(1) so we subtract that out
                if (amountToTake != null)
                    slotOne.remove(amountToTake.getSecond() - 1);
            } else if (MapAtlasesAccessUtils.isValidFilledMap(slotOneItem)) {
                slotOne.remove(1);
            }
        }
    }
}