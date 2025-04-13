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
import pepjebs.mapatlases.map_collection.EmptyMaps;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.utils.AtlasCartographyTable;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.Slice;

import java.util.Map;
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


    @Inject(method = "setupResultSlot", at = @At("HEAD"), cancellable = true)
    void mapAtlasUpdateResult(ItemStack topItem, ItemStack bottomItem, ItemStack oldResult, CallbackInfo info) {
        if (!topItem.is(MapAtlasesMod.MAP_ATLAS.get())) return;
        // cut map
        if (PlatStuff.isShear(bottomItem)) {
            this.access.execute((world, blockPos) -> {
                var maps = MapAtlasItem.getMaps(topItem, world);
                if (maps.isEmpty()) return;
                if (mapatlases$selectedMapIndex > maps.getCount()) {
                    mapatlases$selectedMapIndex = 0;
                }
                MapDataHolder map = maps.getAllFound().get(mapatlases$selectedMapIndex);
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
                MapCollection resultMaps = MapAtlasItem.getMaps(result, world);
                MapCollection bottomMaps = MapAtlasItem.getMaps(bottomItem, world);
                if (resultMaps.getScale() != bottomMaps.getScale()) return;
                var idsToADd = bottomMaps.getIdsCopy();
                resultMaps.addAndAssigns(result, world, idsToADd);

                EmptyMaps emptyMaps = MapAtlasItem.getEmptyMaps(result);
                emptyMaps.addAndAssigns(result, MapAtlasItem.getEmptyMaps(bottomItem).getAll());

                result.grow(1);
                this.resultContainer.setItem(CartographyTableMenu.RESULT_SLOT, result);
                this.broadcastChanges();
                info.cancel();
            });

        }
        // add empty
        else if (MapAtlasesAccessUtils.isValidEmptyMapIngredient(bottomItem)) {
            this.access.execute((world, blockPos) -> {
                ItemStack result = topItem.copy();
                var amountToAdd = MapAtlasesAccessUtils.getMapCountToAdd(topItem, bottomItem, world);
                if (amountToAdd != null) {
                    MapAtlasItem.getEmptyMaps(result).addAndAssigns(result, Map.of(amountToAdd.getFirst(), amountToAdd.getSecond()));
                }
                this.resultContainer.setItem(CartographyTableMenu.RESULT_SLOT, result);
                this.broadcastChanges();
                info.cancel();
            });
        }
        // add a filled map
        else if (bottomItem.getItem() == Items.FILLED_MAP) {
            this.access.execute((world, blockPos) -> {
                ItemStack result = topItem.copy();
                MapDataHolder mapHolder = MapAtlasesAccessUtils.findMapFromItemStack(world, bottomItem);
                MapCollection maps = MapAtlasItem.getMaps(result, world);
                if (maps.getScale() != mapHolder.data.scale) return;
                if (mapHolder != null && maps.addAndAssigns(result, world, mapHolder.type, mapHolder.id) != maps) {
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

            if (PlatStuff.isShear(stack)) {
                if (!this.moveItemStackTo(stack, 1, 1, false)) {
                    info.setReturnValue(ItemStack.EMPTY);
                    return;
                }
            }
            if (stack.getItem() != MapAtlasesMod.MAP_ATLAS.get()) return;

            boolean result = this.moveItemStackTo(stack, 0, 2, false);

            if (!result) {
                info.setReturnValue(ItemStack.EMPTY);
            }
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
            MapCollection maps = MapAtlasItem.getMaps(atlas, level);
            MapDataHolder m = maps.getAllFound().get(mapatlases$selectedMapIndex);
            maps.removeAndAssigns(atlas, level, m.id, m.type);
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
                    int aa = 1;
                }
            }
            if (l.get() != null) {
                if (atlas.getItem() == MapAtlasesMod.MAP_ATLAS.get()) {
                    MapCollection maps = MapAtlasItem.getMaps(atlas, l.get());
                    mapatlases$selectedMapIndex = (mapatlases$selectedMapIndex
                            + (pId == 4 ? maps.getCount() - 1 : 1)) % maps.getCount();
                    try {
                        MapDataHolder map = maps.getAllFound().get(mapatlases$selectedMapIndex);
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