/**
 * This class was forked from:
 * https://github.com/AntiqueAtlasTeam/AntiqueAtlas/blob/37038a399ecac1d58bcc7164ef3d309e8636a2cb/src/main/java
 *      /hunternif/mc/impl/atlas/mixin/MixinCartographyTableAbstractContainerMenu.java
 * Under the GPL-3 license.
 */
package pebjebs.mapatlases.mixin;

import net.minecraft.world.inventory.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;


@Mixin(CartographyTableMenu.class)
public abstract class CartographyTableMenuMixin extends AbstractContainerMenu {

    @Shadow @Final private ResultContainer resultContainer;

    @Shadow @Final private ContainerLevelAccess access;

    protected CartographyTableMenuMixin(@Nullable MenuType<?> arg, int i) {
        super(arg, i);
    }
/*//TODO: PORT

    @Inject(method = "setupResultSlot", at = @At("HEAD"), cancellable = true)
    void mapAtlasUpdateResult(ItemStack atlas, ItemStack bottomItem, ItemStack oldResult, CallbackInfo info) {
        if (atlas.getItem() == MapAtlasesMod.MAP_ATLAS && bottomItem.getItem() == MapAtlasesMod.MAP_ATLAS) {
            final int[] allMapIds = Stream.of(Arrays.stream(MapAtlasesAccessUtils.getMapIdsFromItemStack(atlas)),
                    Arrays.stream(MapAtlasesAccessUtils.getMapIdsFromItemStack(bottomItem)))
                    .flatMapToInt(x -> x)
                    .distinct()
                    .toArray();
            this.access.execute((world, blockPos) -> {
                int[] filteredMapIds = filterIntArrayForUniqueMaps(world, allMapIds);
                ItemStack result = new ItemStack(MapAtlasesMod.MAP_ATLAS.get());
                CompoundTag mergedNbt = new CompoundTag();
                int halfEmptyCount = (int) Math.ceil((MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(atlas)
                        + MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(bottomItem)) / 2.0);
                mergedNbt.putInt(MapAtlasItem.EMPTY_MAP_NBT, halfEmptyCount);
                mergedNbt.putIntArray(MapAtlasItem.MAP_LIST_NBT, filteredMapIds);
                result.setTag(mergedNbt);
                result.grow(1);
                this.resultContainer.setItem(CartographyTableAbstractContainerMenu.RESULT_SLOT_INDEX, result);
            });

            this.sendContentUpdates();

            info.cancel();
        } else if (atlas.getItem() == MapAtlasesMod.MAP_ATLAS && (bottomItem.getItem() == Items.MAP
                || (MapAtlasesClientConfig.acceptPaperForEmptyMaps.get() && bottomItem.getItem() == Items.PAPER))) {
            ItemStack result = atlas.copy();
            CompoundTag nbt = result.getTag() != null ? result.getTag() : new CompoundTag();
            int amountToAdd = MapAtlasesAccessUtils.getMapCountToAdd(atlas, bottomItem);
            nbt.putInt(MapAtlasItem.EMPTY_MAP_NBT, nbt.getInt(MapAtlasItem.EMPTY_MAP_NBT) + amountToAdd);
            result.setNbt(nbt);
            this.resultInventory.setStack(CartographyTableAbstractContainerMenu.RESULT_SLOT_INDEX, result);

            this.sendContentUpdates();

            info.cancel();
        } else if (atlas.getItem() == MapAtlasesMod.MAP_ATLAS && bottomItem.getItem() == Items.FILLED_MAP) {
            ItemStack result = atlas.copy();
            if (bottomItem.getTag() == null || !bottomItem.hasNbt() || !bottomItem.getTag().contains("map"))
                return;
            int mapId = bottomItem.getTag().getInt("map");
            CompoundTag compound = result.getTag();
            if (compound == null) return;
            int[] existentMapIdArr = compound.getIntArray(MapAtlasItem.MAP_LIST_NBT);
            List<Integer> existentMapIds =
                    Arrays.stream(existentMapIdArr).boxed().distinct().collect(Collectors.toList());
            if (!existentMapIds.contains(mapId)) {
                existentMapIds.add(mapId);
            }
            this.context.run((world, blockPos) -> {
                int[] filteredMapIds =
                        filterIntArrayForUniqueMaps(world, existentMapIds.stream().mapToInt(s -> s).toArray());

                compound.putIntArray(MapAtlasItem.MAP_LIST_NBT, filteredMapIds);
                result.setNbt(compound);

                this.resultInventory.setStack(CartographyTableAbstractContainerMenu.RESULT_SLOT_INDEX, result);
            });

            this.sendContentUpdates();

            info.cancel();
        } else if (atlas.getItem() == Items.BOOK && bottomItem.getItem() == Items.FILLED_MAP) {
            ItemStack result = new ItemStack(MapAtlasesMod.MAP_ATLAS);
            if (bottomItem.getTag() == null || !bottomItem.hasNbt() || !bottomItem.getTag().contains("map"))
                return;
            int mapId = bottomItem.getTag().getInt("map");
            CompoundTag compound = new CompoundTag();
            compound.putIntArray(MapAtlasItem.MAP_LIST_NBT, new int[]{mapId});
            if (MapAtlasesMod.CONFIG != null && MapAtlasesClientConfig.pityActivationMapCount.get() > 0) {
                compound.putInt(MapAtlasItem.EMPTY_MAP_NBT, MapAtlasesClientConfig.pityActivationMapCount.get());
            }
            result.setNbt(compound);
            this.resultInventory.setStack(CartographyTableAbstractContainerMenu.RESULT_SLOT_INDEX, result);

            this.sendContentUpdates();

            info.cancel();
        }
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    void mapAtlasTransferSlot(Player player, int index, CallbackInfoReturnable<ItemStack> info) {
        if (index >= 0 && index <= 2) return;

        Slot slot = this.slots.get(index);

        if (slot.hasStack()) {
            ItemStack stack = slot.getStack();

            if (stack.getItem() != MapAtlasesMod.MAP_ATLAS) return;

            boolean result = this.insertItem(stack, 0, 2, false);

            if (!result) {
                info.setReturnValue(ItemStack.EMPTY);
            }
        }
    }

    // Filters for both duplicate map id (e.g. "map_25") and duplicate X+Z+Dimension
    private int[] filterIntArrayForUniqueMaps(Level world, int[] toFilter) {
        Map<String, Pair<Integer, MapItemSavedData>> uniqueXZMapIds =
                Arrays.stream(toFilter)
                        .mapToObj(mId -> new Pair<>(mId, world.getMapData("map_" + mId)))
                        .filter(m -> m.getRight() != null)
                        .collect(Collectors.toMap(
                                m -> m.getRight().centerX + ":" + m.getRight().centerZ
                                        + ":"  + m.getRight().dimension,
                                m -> m,
                                (m1, m2) -> m1));
        return uniqueXZMapIds.values().stream().mapToInt(Pair::getLeft).toArray();
    }
*/
}