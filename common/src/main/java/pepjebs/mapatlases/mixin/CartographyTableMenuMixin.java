/**
 * This class was forked from:
 * https://github.com/AntiqueAtlasTeam/AntiqueAtlas/blob/37038a399ecac1d58bcc7164ef3d309e8636a2cb/src/main/java
 * /hunternif/mc/impl/atlas/mixin/MixinCartographyTableAbstractContainerMenu.java
 * Under the GPL-3 license.
 */
package pepjebs.mapatlases.mixin;

import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
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
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.PlatStuff;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.EmptyMaps;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.utils.AtlasCartographyTable;
import pepjebs.mapatlases.utils.AtlasMap;
import pepjebs.mapatlases.utils.MapType;
import pepjebs.mapatlases.utils.Slice;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


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
   private void mapAtlasUpdateResult(ItemStack topItem, ItemStack bottomItem, ItemStack oldResult, CallbackInfo info) {
        if (!topItem.is(MapAtlasesMod.MAP_ATLAS.get())) return;
        this.access.execute((world, blockPos) -> {
            ItemStack result = mapatlases$makeResult(topItem, bottomItem, world);
            //vanilla doesnt clear the slot for an atlas, so a stale result would stay takeable
            if (result == null) result = ItemStack.EMPTY;
            this.resultContainer.setItem(CartographyTableMenu.RESULT_SLOT, result);
            this.broadcastChanges();
            info.cancel();
        });
    }

    @Unique
    @Nullable
    private ItemStack mapatlases$makeResult(ItemStack atlas, ItemStack bottomItem, Level level) {
        if (PlatStuff.isShear(bottomItem)) return mapatlases$cutSelectedMap(atlas, level);
        if (bottomItem.is(MapAtlasesMod.MAP_ATLAS.get())) return mapatlases$mergeAtlases(atlas, bottomItem, level);
        MapType emptyType = MapType.acceptedEmptyMapType(bottomItem);
        if (emptyType != null) return mapatlases$addEmptyMaps(atlas, emptyType, MapAtlasItem.countEmptyMapsToAdd(atlas, bottomItem, level), level);
        if (bottomItem.is(Items.FILLED_MAP)) return mapatlases$addFilledMap(atlas, bottomItem, level);
        return null;
    }

    @Unique
    @Nullable
    private ItemStack mapatlases$cutSelectedMap(ItemStack atlas, Level level) {
        List<AtlasMap> found = mapatlases$getMapsInOrder(atlas, level);
        if (found.isEmpty()) return null;
        if (mapatlases$selectedMapIndex >= found.size()) mapatlases$selectedMapIndex = 0;
        AtlasMap map = found.get(mapatlases$selectedMapIndex);
        this.mapatlases$selectedSlice = map.slice;
        return map.createExistingMapItem();
    }

    @Unique
    @Nullable
    private ItemStack mapatlases$mergeAtlases(ItemStack atlas, ItemStack other, Level level) {
        ItemStack result = atlas.copyWithCount(1);
        MapCollection resultMaps = MapAtlasItem.getMaps(result, level);
        MapCollection otherMaps = MapAtlasItem.getMaps(other, level);
        //empty atlas has no scale yet so it can merge with anything
        if (!resultMaps.isEmpty() && !otherMaps.isEmpty()
                && resultMaps.getScale() != otherMaps.getScale()) return null;
        resultMaps.addAndAssign(result, level, otherMaps.getIds().getAll());

        // Both atlases leave the table, so split the pool rather than giving each the full sum.
        Map<MapType, Integer> pooled = new EnumMap<>(MapType.class);
        MapAtlasItem.getEmptyMaps(atlas).getAll().forEach((type, count) -> pooled.merge(type, count, Integer::sum));
        MapAtlasItem.getEmptyMaps(other).getAll().forEach((type, count) -> pooled.merge(type, count, Integer::sum));
        pooled.replaceAll((type, count) -> count / 2);
        pooled.values().removeIf(count -> count == 0);
        result.set(MapAtlasesMod.EMPTY_MAPS.get(), EmptyMaps.of(pooled));

        result.grow(1);
        return result;
    }

    @Unique
    private ItemStack mapatlases$addEmptyMaps(ItemStack atlas, MapType type, int amount, Level level) {
        //full atlas still takes the result slot, just leaves it empty
        if (amount <= 0) return ItemStack.EMPTY;
        ItemStack result = atlas.copyWithCount(1);
        MapAtlasItem.getEmptyMaps(result).addAndAssign(result, type, amount);
        return result;
    }

    @Unique
    @Nullable
    private ItemStack mapatlases$addFilledMap(ItemStack atlas, ItemStack map, Level level) {
        AtlasMap holder = AtlasMap.fromFilledMapItem(level, map);
        if (holder == null) return null;
        ItemStack result = atlas.copyWithCount(1);
        MapCollection maps = MapAtlasItem.getMaps(result, level);
        if (!maps.isEmpty() && maps.getScale() != holder.data.scale) return null;
        if (!maps.addAndAssign(result, level, holder.type, holder.id)) return null;
        return result;
    }

    @Unique
    private static List<AtlasMap> mapatlases$getMapsInOrder(ItemStack atlas, Level level) {
        List<AtlasMap> found = MapAtlasItem.getMaps(atlas, level).getAllFound();
        found.sort(Comparator.comparingInt((AtlasMap h) -> h.type.ordinal()).thenComparingInt(h -> h.id.id()));
        return found;
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
            List<AtlasMap> found = mapatlases$getMapsInOrder(atlas, level);
            if (mapatlases$selectedMapIndex >= found.size()) return;
            AtlasMap m = found.get(mapatlases$selectedMapIndex);
            MapAtlasItem.removeMaps(atlas, level, List.of(m));
        });
    }

    @Override
    public boolean clickMenuButton(Player pPlayer, int pId) {
        if (pId != 4 && pId != 5) return super.clickMenuButton(pPlayer, pId);
        ItemStack atlas = this.slots.get(0).getItem();
        Level level = mapatlases$getLevel();
        if (level != null && atlas.is(MapAtlasesMod.MAP_ATLAS.get())) {
            List<AtlasMap> found = mapatlases$getMapsInOrder(atlas, level);
            if (found.isEmpty()) {
                this.mapatlases$selectedSlice = null;
            } else {
                mapatlases$selectedMapIndex = Math.floorMod(mapatlases$selectedMapIndex + (pId == 4 ? -1 : 1), found.size());
                this.mapatlases$selectedSlice = found.get(mapatlases$selectedMapIndex).slice;
            }
        }
        this.slotsChanged(this.container);
        return true;
    }

    //access is NULL on the client...
    @Unique
    @Nullable
    private Level mapatlases$getLevel() {
        Level level = access.evaluate((l, p) -> l, null);
        if (level == null && PlatHelper.getPhysicalSide().isClient()){
            return MapAtlasesClient.getLevel();
        }
        return level;
    }
}
