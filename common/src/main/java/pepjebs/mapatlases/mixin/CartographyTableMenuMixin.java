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
import org.jetbrains.annotations.NotNull;
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
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.EmptyMaps;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.utils.AtlasCartographyTable;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.MapType;
import pepjebs.mapatlases.utils.Slice;

import java.util.EnumMap;
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
    public abstract void slotsChanged(@NotNull Container pInventory);

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
                var found = maps.getAllFound();
                if (mapatlases$selectedMapIndex >= found.size()) {
                    mapatlases$selectedMapIndex = 0;
                }
                MapDataHolder map = found.get(mapatlases$selectedMapIndex);
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
                ItemStack result = topItem.copyWithCount(1);
                MapCollection resultMaps = MapAtlasItem.getMaps(result, world);
                MapCollection bottomMaps = MapAtlasItem.getMaps(bottomItem, world);
                // an empty atlas has no scale yet so it can merge with anything
                if (!resultMaps.isEmpty() && !bottomMaps.isEmpty()
                        && resultMaps.getScale() != bottomMaps.getScale()) return;
                var idsToAdd = bottomMaps.getIdsCopy();
                resultMaps.addAndAssigns(result, world, idsToAdd);

                // Both atlases leave the table, so split the pool rather than giving each the full sum.
                Map<MapType, Integer> pooled = new EnumMap<>(MapType.class);
                MapAtlasItem.getEmptyMaps(topItem).getAll().forEach((type, count) -> pooled.merge(type, count, Integer::sum));
                MapAtlasItem.getEmptyMaps(bottomItem).getAll().forEach((type, count) -> pooled.merge(type, count, Integer::sum));
                pooled.replaceAll((type, count) -> count / 2);
                pooled.values().removeIf(count -> count == 0);
                result.set(MapAtlasesMod.EMPTY_MAPS.get(), EmptyMaps.of(pooled));

                result.grow(1);
                this.resultContainer.setItem(CartographyTableMenu.RESULT_SLOT, result);
                this.broadcastChanges();
                info.cancel();
            });

        }
        // add empty
        else if (MapAtlasesAccessUtils.isValidEmptyMapIngredient(bottomItem)) {
            this.access.execute((world, blockPos) -> {
                var amountToAdd = MapAtlasesAccessUtils.getMapCountToAdd(topItem, bottomItem, world);
                boolean atlasIsFull = amountToAdd == null || amountToAdd.getSecond() <= 0;
                ItemStack result = ItemStack.EMPTY;
                if (!atlasIsFull) {
                    result = topItem.copyWithCount(1);
                    MapAtlasItem.getEmptyMaps(result).addAndAssigns(result, amountToAdd.getFirst(), amountToAdd.getSecond());
                }
                this.resultContainer.setItem(CartographyTableMenu.RESULT_SLOT, result);
                this.broadcastChanges();
                info.cancel();
            });
        }
        // add a filled map
        else if (bottomItem.getItem() == Items.FILLED_MAP) {
            this.access.execute((world, blockPos) -> {
                ItemStack result = topItem.copyWithCount(1);
                MapDataHolder mapHolder = MapAtlasesAccessUtils.findMapFromItemStack(world, bottomItem);
                if (mapHolder == null) return;
                MapCollection maps = MapAtlasItem.getMaps(result, world);
                if (!maps.isEmpty() && maps.getScale() != mapHolder.data.scale) return;
                if (maps.addAndAssigns(result, world, mapHolder.type, mapHolder.id) != maps) {
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
            maps.removeDataAndAssign(atlas, level, m);
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
                    var found = maps.getAllFound();
                    if (!found.isEmpty()) {
                        mapatlases$selectedMapIndex = Math.floorMod(
                                mapatlases$selectedMapIndex + (pId == 4 ? -1 : 1), found.size());
                        this.mapatlases$selectedSlice = found.get(mapatlases$selectedMapIndex).slice;
                    } else {
                        this.mapatlases$selectedSlice = null;
                    }
                }
            }
            this.slotsChanged(this.container);
            return true;
        }
        return super.clickMenuButton(pPlayer, pId);
    }
}