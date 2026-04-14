/**
 * This class was forked from:
 * https://github.com/AntiqueAtlasTeam/AntiqueAtlas/blob/37038a399ecac1d58bcc7164ef3d309e8636a2cb/src/main/java
 * /hunternif/mc/impl/atlas/mixin/MixinCartographyTableAbstractContainerMenu.java
 * Under the GPL-3 license.
 */
package pepjebs.mapatlases.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.PlatStuff;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.IMapCollection;
import pepjebs.mapatlases.utils.AtlasCartographyTable;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.Slice;

import java.util.concurrent.atomic.AtomicReference;


@Mixin(CartographyTableMenu.class)
public abstract class CartographyTableMenuMixin extends AbstractContainerMenu implements AtlasCartographyTable {

    @Shadow
    @Final
    private ResultContainer resultContainer;

    @Shadow
    @Final
    private ContainerLevelAccess access;

    @Shadow
    public abstract void slotsChanged(Container pInventory);

    @Shadow
    @Final
    public Container container;

    @Unique
    private int mapatlases$selectedMapIndex;
    @Nullable
    @Unique
    private Slice mapatlases$selectedSlice;

    protected CartographyTableMenuMixin(@Nullable MenuType<?> arg, int i) {
        super(arg, i);
    }
    //TODO: TF maps cant go here


    @Inject(method = "setupResultSlot", at = @At("HEAD"), cancellable = true)
    void mapAtlas$UpdateResult(ItemStack topItem, ItemStack bottomItem, ItemStack oldResult, CallbackInfo info) {
        // Never allow stacks in either slot
        if (topItem.getCount() > 1 || bottomItem.getCount() > 1) {
            this.resultContainer.setItem(CartographyTableMenu.RESULT_SLOT, ItemStack.EMPTY);
            info.cancel();
            return;
        }
        if (!topItem.is(MapAtlasesMod.MAP_ATLAS.get())) return;
        // cut map
        if (PlatStuff.isShear(bottomItem)) {
            this.access.execute((world, blockPos) -> {
                var maps = MapAtlasItem.getMaps(topItem, world);
                if (maps.isEmpty()) return;
                if (mapatlases$selectedMapIndex > maps.getCount()) {
                    mapatlases$selectedMapIndex = 0;
                }
                MapDataHolder map = maps.getAll().get(mapatlases$selectedMapIndex);
                ItemStack result = map.createExistingMapItem();
                this.mapatlases$selectedSlice = map.slice;
                this.resultContainer.setItem(CartographyTableMenu.RESULT_SLOT, result);
                this.broadcastChanges();
                info.cancel();
            });
        }
        // merge atlases
        else if (bottomItem.is(MapAtlasesMod.MAP_ATLAS.get())) {
            this.access.execute((world, blockPos) -> {
                ItemStack result = topItem.copy();
                IMapCollection resultMaps = MapAtlasItem.getMaps(result, world);
                IMapCollection bottomMaps = MapAtlasItem.getMaps(bottomItem, world);
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

        }
        // add empty
        else if (bottomItem.getItem() == Items.MAP
                || (MapAtlasesConfig.acceptPaperForEmptyMaps.get() && bottomItem.getItem() == Items.PAPER)) {
            this.access.execute((world, blockPos) -> {
                ItemStack result = topItem.copy();
                int amountToAdd = MapAtlasesAccessUtils.getMapCountToAdd(topItem, bottomItem, world);
                MapAtlasItem.increaseEmptyMaps(result, amountToAdd);
                this.resultContainer.setItem(CartographyTableMenu.RESULT_SLOT, result);
                this.broadcastChanges();
                info.cancel();
            });
        }
        // add a filled map
        else if (bottomItem.getItem() == Items.FILLED_MAP) {
            this.access.execute((world, blockPos) -> {
                ItemStack result = topItem.copy();
                Integer mapId = MapAtlasesAccessUtils.getMapId(bottomItem);
                IMapCollection maps = MapAtlasItem.getMaps(result, world);
                if (mapId != null && maps.add(mapId, world)) {
                    this.resultContainer.setItem(CartographyTableMenu.RESULT_SLOT, result);
                    this.broadcastChanges();
                    info.cancel();
                }
            });
        }
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    void mapAtlas$TransferSlot(Player player, int index, CallbackInfoReturnable<ItemStack> info) {
        // Prevent iterating result slot when extracting a map from atlas+shears
        if (index == 2) {
            Slot resultSlot = this.slots.get(2);
            if (resultSlot.hasItem()) {
                ItemStack result = resultSlot.getItem().copy();
                resultSlot.onTake(player, resultSlot.getItem());
                player.getInventory().add(result);
            }
            info.setReturnValue(ItemStack.EMPTY);
            return;
        }
        if (index >= 0 && index <= 2) return;

        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return;

        ItemStack stack = slot.getItem();

        // Shears: only go in bottom slot (slot 1), and only if top slot has an atlas
        if (PlatStuff.isShear(stack)) {
            if (this.slots.get(0).hasItem() && this.slots.get(0).getItem().is(MapAtlasesMod.MAP_ATLAS.get())) {
                if (!this.moveItemStackTo(stack, 1, 2, false)) {
                    info.setReturnValue(ItemStack.EMPTY);
                }
            } else {
                info.setReturnValue(ItemStack.EMPTY);
            }
            return;
        }

        // Paper: bottom slot (slot 1) only if top slot has an atlas or filled map
        if (stack.getItem() == Items.PAPER) {
            if (this.slots.get(0).hasItem() && 
                (this.slots.get(0).getItem().is(MapAtlasesMod.MAP_ATLAS.get()) || 
                this.slots.get(0).getItem().getItem() == Items.FILLED_MAP)) {
                if (!this.moveItemStackTo(stack, 1, 2, false)) {
                    info.setReturnValue(ItemStack.EMPTY);
                }
            } else {
                info.setReturnValue(ItemStack.EMPTY);
            }
            return;
        }

        // Atlas: top slot first (slot 0), then bottom slot (slot 1) only if top slot has an atlas
        if (stack.getItem() == MapAtlasesMod.MAP_ATLAS.get()) {
            ItemStack single = stack.copyWithCount(1);
            if (!this.slots.get(0).hasItem()) {
                this.slots.get(0).set(single);
                stack.shrink(1);
            } else if (this.slots.get(0).getItem().is(MapAtlasesMod.MAP_ATLAS.get()) && !this.slots.get(1).hasItem()) {
                this.slots.get(1).set(single);
                stack.shrink(1);
            } else {
                info.setReturnValue(ItemStack.EMPTY);
            }
            info.setReturnValue(ItemStack.EMPTY);
            return;
        }

        // Filled map: top slot first (slot 0), then bottom slot (slot 1)
        if (stack.getItem() == Items.FILLED_MAP) {
            if (!this.slots.get(0).hasItem()) {
                this.moveItemStackTo(stack, 0, 1, false);
            } else if (!this.slots.get(1).hasItem()) {
                this.moveItemStackTo(stack, 1, 2, false);
            } else {
                info.setReturnValue(ItemStack.EMPTY);
            }
            info.setReturnValue(ItemStack.EMPTY);
            return;
        }

        // Everything else: vanilla behaviour, try top slot only
        if (!this.moveItemStackTo(stack, 0, 1, false)) {
            info.setReturnValue(ItemStack.EMPTY);
        }
    }

    @Override
    public void mapatlases$setSelectedMapIndex(int index) {
        mapatlases$selectedMapIndex = index;
    }

    @Override
    public int mapatlases$getSelectedMapIndex() {
        return mapatlases$selectedMapIndex;
    }

    @Nullable
    @Override
    public Slice mapatlases$getSelectedSlice() {
        return mapatlases$selectedSlice;
    }

    @Override
    public void mapatlases$removeSelectedMap(ItemStack atlas) {
        access.execute((level, pos) -> {
            var maps = MapAtlasItem.getMaps(atlas, level);
            MapDataHolder m = maps.getAll().get(mapatlases$selectedMapIndex);
            maps.remove(m);
        });
    }

    @Override
    public boolean clickMenuButton(Player pPlayer, int pId) {
        ItemStack atlas = this.slots.get(0).getItem();
        if (pId == 4 || pId == 5) {
            AtomicReference<Level> l = new AtomicReference<>();
            access.execute((level, pos) -> {
                l.set(level);
            });
            if (l.get() == null) {
                try {
                    MapAtlasesClient.getClientAccess().execute((level, pos) -> l.set(level));
                } catch (Exception ignored) {
                }
            }
            if (l.get() != null) {
                if (atlas.getItem() == MapAtlasesMod.MAP_ATLAS.get()) {
                    var maps = MapAtlasItem.getMaps(atlas, l.get());
                    mapatlases$selectedMapIndex = (mapatlases$selectedMapIndex
                            + (pId == 4 ? maps.getCount() - 1 : 1)) % maps.getCount();
                    try {
                        MapDataHolder map = maps.getAll().get(mapatlases$selectedMapIndex);
                        if (map != null) {
                            this.mapatlases$selectedSlice = map.slice;
                        } else {
                            this.mapatlases$selectedSlice = null;
                        }
                    } catch (Exception e) {
                        //aa ERROR
                        int a = 1;
                    }
                }
            }
            this.slotsChanged(this.container);
            return true;
        }
        return super.clickMenuButton(pPlayer, pId);
    }
}
