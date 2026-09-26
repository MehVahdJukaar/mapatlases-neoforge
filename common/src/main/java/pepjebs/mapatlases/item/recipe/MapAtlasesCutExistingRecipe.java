package pepjebs.mapatlases.item.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.EmptyMaps;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.utils.ICraftingInputWithContext;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.MapType;
import pepjebs.mapatlases.utils.Slice;

import java.util.List;

public class MapAtlasesCutExistingRecipe extends AbstractAtlasRecipe {

    public MapAtlasesCutExistingRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput inv, Level level) {
        ItemStack atlas = ItemStack.EMPTY;
        ItemStack shears = ItemStack.EMPTY;
        for (ItemStack i : inv.items()) {
            if (!i.isEmpty()) {
                if (i.is(MapAtlasesMod.MAP_ATLAS.get()) &&
                        (MapAtlasItem.getEmptyMaps(i).getTotalCount() > 0 || MapAtlasItem.getMaps(i, level).getCount() > 0)) {
                    if (!atlas.isEmpty()) return false;
                    atlas = i;
                } else if (i.is(Items.SHEARS) && i.getDamageValue() < i.getMaxDamage() - 1) {
                    if (!shears.isEmpty()) return false;
                    shears = i;
                } else return false;
            }
        }
        boolean b = !shears.isEmpty() && !atlas.isEmpty();
        if (b) rememberLevel(level);
        return b;
    }

    @Override
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider registries) {
        Level level = getLevel();
        ItemStack atlas = ItemStack.EMPTY;
        for (ItemStack i : inv.items()) {
            if (i.is(MapAtlasesMod.MAP_ATLAS.get())) {
                atlas = i;
                break;
            }
        }
        if (atlas.isEmpty() || level == null) return ItemStack.EMPTY;
        MapCollection maps = MapAtlasItem.getMaps(atlas, level);
        //not using count. we want actual maps
        Slice slice = MapAtlasItem.getSelectedSlice(atlas, level.dimension());
        //TODO: very ugly and wont work in many cases
        MapDataHolder toRemove = getMapToRemove(inv, maps, slice);
        if (toRemove != null) return toRemove.createExistingMapItem();
        MapType emptyToRemove = getEmptyMapToRemove(MapAtlasItem.getEmptyMaps(atlas), slice);
        if (emptyToRemove != null) {
            return emptyToRemove.getEmpty().getDefaultInstance();
        }
        //should never run
        return ItemStack.EMPTY;
    }

    @Nullable
    private static MapType getEmptyMapToRemove(EmptyMaps emptyMaps, Slice slice) {
        if (emptyMaps.getCount(slice) > 0) {
            return slice.emptyMapType();
        }
        for (var e : emptyMaps.getAll().entrySet()) {
            if (e.getValue() > 0) return e.getKey();
        }
        return null;
    }

    @Nullable
    private static MapDataHolder getMapToRemove(CraftingInput inv, MapCollection maps, Slice slice) {
        List<MapDataHolder> found = maps.getAllFound();
        if (found.isEmpty()) return null;
        Player crafter = null;
        if (inv instanceof ICraftingInputWithContext ct) {
            AbstractContainerMenu menu = ct.mapAtlases$getMenu();
            if (menu instanceof CraftingMenu cm) crafter = cm.player;
            else if (menu instanceof InventoryMenu im) crafter = im.owner;
        }
        if (crafter != null) {
            MapDataHolder closest = maps.getClosest(crafter, slice);
            if (closest != null) return closest;
        }
        return found.getFirst();
    }


    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput inv) {
        NonNullList<ItemStack> list = NonNullList.create();
        Level level = getLevel();
        for (ItemStack i : inv.items()) {
            ItemStack stack = i.copyWithCount(1);
            if (stack.is(Items.SHEARS)) {
                if (level instanceof ServerLevel sl) stack.hurtAndBreak(1, sl, null, s -> {}); //shrinks to empty on its own
            } else if (stack.is(MapAtlasesMod.MAP_ATLAS.get()) && level != null) {
                cutOneMap(inv, stack, level);
            }
            list.add(stack);
        }
        return list;
    }

    private static void cutOneMap(CraftingInput inv, ItemStack atlas, Level level) {
        MapCollection maps = MapAtlasItem.getMaps(atlas, level);
        Slice slice = MapAtlasItem.getSelectedSlice(atlas, level.dimension());
        MapDataHolder toRemove = getMapToRemove(inv, maps, slice);
        if (toRemove != null) {
            maps.removeAndAssigns(atlas, level, List.of(toRemove));
            maps = MapAtlasItem.getMaps(atlas, level);
            MapAtlasItem.setSelectedSlice(atlas, maps.closestAvailableSlice(slice.dimension(), slice), level);
            return;
        }
        EmptyMaps emptyMaps = MapAtlasItem.getEmptyMaps(atlas);
        MapType emptyToRemove = getEmptyMapToRemove(emptyMaps, slice);
        if (emptyToRemove != null) emptyMaps.addAndAssigns(atlas, emptyToRemove, -1);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width + height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MapAtlasesMod.MAP_ATLAS_CUT_RECIPE.get();
    }
}
