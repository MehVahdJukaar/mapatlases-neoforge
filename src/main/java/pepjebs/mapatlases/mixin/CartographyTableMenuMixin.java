/**
 * This class was forked from:
 * https://github.com/AntiqueAtlasTeam/AntiqueAtlas/blob/37038a399ecac1d58bcc7164ef3d309e8636a2cb/src/main/java
 * /hunternif/mc/impl/atlas/mixin/MixinCartographyTableAbstractContainerMenu.java
 * Under the GPL-3 license.
 */
package pepjebs.mapatlases.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.capabilities.MapCollectionCap;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;


@Mixin(CartographyTableMenu.class)
public abstract class CartographyTableMenuMixin extends AbstractContainerMenu {

    @Shadow
    @Final
    private ResultContainer resultContainer;

    @Shadow
    @Final
    private ContainerLevelAccess access;

    protected CartographyTableMenuMixin(@Nullable MenuType<?> arg, int i) {
        super(arg, i);
    }


    @Inject(method = "setupResultSlot", at = @At("HEAD"), cancellable = true)
    void mapAtlasUpdateResult(ItemStack atlas, ItemStack bottomItem, ItemStack oldResult, CallbackInfo info) {
        if (!atlas.is(MapAtlasesMod.MAP_ATLAS.get())) return;
        // merge atlases
        if (bottomItem.is(MapAtlasesMod.MAP_ATLAS.get())) {
            this.access.execute((world, blockPos) -> {
                ItemStack result = atlas.copy();
                MapCollectionCap resultMaps = MapAtlasItem.getMaps(result, world);
                MapCollectionCap bottomMaps = MapAtlasItem.getMaps(bottomItem, world);
                if (resultMaps.getScale() != bottomMaps.getScale()) return;
                int[] idsToADd = bottomMaps.getAllIds();
                for (var i : idsToADd) {
                    resultMaps.add(i, world);
                }
                MapAtlasItem.setEmptyMaps(result, (int) Math.ceil((MapAtlasItem.getEmptyMaps(result) + MapAtlasItem.getEmptyMaps(bottomItem)) / 2f));

                result.grow(1);
                this.resultContainer.setItem(CartographyTableMenu.RESULT_SLOT, result);
                this.broadcastChanges();
                info.cancel();
            });

            //add map
        } else if (bottomItem.getItem() == Items.MAP
                || (MapAtlasesConfig.acceptPaperForEmptyMaps.get() && bottomItem.getItem() == Items.PAPER)) {
            this.access.execute((world, blockPos) -> {
                ItemStack result = atlas.copy();
                int amountToAdd = MapAtlasesAccessUtils.getMapCountToAdd(atlas, bottomItem, world);
                MapAtlasItem.increaseEmptyMaps(result, amountToAdd);
                this.resultContainer.setItem(CartographyTableMenu.RESULT_SLOT, result);
                this.broadcastChanges();
                info.cancel();
            });
        } else if (bottomItem.getItem() == Items.FILLED_MAP) {
            this.access.execute((world, blockPos) -> {

                ItemStack result = atlas.copy();
                Integer mapId = MapItem.getMapId(result);
                MapCollectionCap maps = MapAtlasItem.getMaps(atlas, world);
                if (maps.add(mapId, world)) {
                    this.resultContainer.setItem(CartographyTableMenu.RESULT_SLOT, result);
                    this.broadcastChanges();
                    info.cancel();
                }
            });
        }
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    void mapAtlasTransferSlot(Player player, int index, CallbackInfoReturnable<ItemStack> info) {
        if (index >= 0 && index <= 2) return;

        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();

            if (stack.getItem() != MapAtlasesMod.MAP_ATLAS) return;

            boolean result = this.moveItemStackTo(stack, 0, 2, false);

            if (!result) {
                info.setReturnValue(ItemStack.EMPTY);
            }
        }
    }


}