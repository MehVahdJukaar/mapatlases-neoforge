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
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.Tags;
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
import pepjebs.mapatlases.capabilities.MapCollectionCap;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.AtlasCartographyTable;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

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

    protected CartographyTableMenuMixin(@Nullable MenuType<?> arg, int i) {
        super(arg, i);
    }


    @Inject(method = "setupResultSlot", at = @At("HEAD"), cancellable = true)
    void mapAtlasUpdateResult(ItemStack topItem, ItemStack bottomItem, ItemStack oldResult, CallbackInfo info) {
        if (!topItem.is(MapAtlasesMod.MAP_ATLAS.get())) return;
        // cut map
        if (bottomItem.is(Tags.Items.SHEARS)) {
            this.access.execute((world, blockPos) -> {
                var maps = MapAtlasItem.getMaps(topItem, world);
                if (maps.isEmpty()) return;
                if (mapatlases$selectedMapIndex > maps.getCount()) {
                    mapatlases$selectedMapIndex = 0;
                }
                var map = maps.getAll().get(mapatlases$selectedMapIndex);
                ItemStack result = MapAtlasesAccessUtils.createMapItemStackFromId(
                        MapAtlasesAccessUtils.findMapIntFromString(map.getFirst())
                );

                this.resultContainer.setItem(CartographyTableMenu.RESULT_SLOT, result);
                this.broadcastChanges();
                info.cancel();
            });
        }
        // merge atlases
        else if (bottomItem.is(MapAtlasesMod.MAP_ATLAS.get())) {
            this.access.execute((world, blockPos) -> {
                ItemStack result = topItem.copy();
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
                Integer mapId = MapItem.getMapId(bottomItem);
                MapCollectionCap maps = MapAtlasItem.getMaps(result, world);
                if (mapId != null && maps.add(mapId, world)) {
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

            if (stack.is(Tags.Items.SHEARS)) {
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

    @Override
    public void mapatlases$removeSelectedMap(ItemStack atlas) {
        access.execute((level, pos) -> {
            var maps = MapAtlasItem.getMaps(atlas, level);
            var m = maps.getAll().get(mapatlases$selectedMapIndex);
            maps.remove(m.getFirst());
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
            if(l.get() == null){
                try{
                    MapAtlasesClient.getClientAccess().execute((level, pos) -> l.set(level));
                }catch (Exception ignored){};
            }
            if(l.get() != null) {
                if (atlas.getItem() == MapAtlasesMod.MAP_ATLAS.get()) {
                    var maps = MapAtlasItem.getMaps(atlas, l.get());
                    mapatlases$selectedMapIndex = (mapatlases$selectedMapIndex
                            + (pId == 4 ? maps.getCount() - 1 : 1)) % maps.getCount();
                }
            }
            this.slotsChanged(this.container);
            return true;
        }
        return super.clickMenuButton(pPlayer, pId);
    }
}