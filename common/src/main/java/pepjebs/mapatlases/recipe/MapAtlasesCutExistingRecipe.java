package pepjebs.mapatlases.recipe;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.utils.ICraftingInputWithContext;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.Slice;

import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class MapAtlasesCutExistingRecipe extends CustomRecipe {

    public static final Codec<MapAtlasesCutExistingRecipe> CODEC = null;

    private WeakReference<Level> levelRef = new WeakReference<>(null);

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
                        (MapAtlasItem.getEmptyMaps(i) > 0 || MapAtlasItem.getMaps(i, level).getCount() > 0)) {
                    if (!atlas.isEmpty()) return false;
                    atlas = i;
                } else if (i.is(Items.SHEARS) && i.getDamageValue() < i.getMaxDamage() - 1) {
                    if (!shears.isEmpty()) return false;
                    shears = i;
                } else return false;
            }
        }
        boolean b = !shears.isEmpty() && !atlas.isEmpty();
        if (b) {
            levelRef = new WeakReference<>(level);
        }
        return b;
    }

    @Override
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider registries) {
        ItemStack atlas = ItemStack.EMPTY;
        for (ItemStack i : inv.items()) {
            if (i.is(MapAtlasesMod.MAP_ATLAS.get())) {
                atlas = i;
                break;
            }
        }
        MapCollection maps = MapAtlasItem.getMaps(atlas, levelRef.get());
        //not using count. we want actual maps
        if (maps.getAll().size() > 1) {
            var slice = MapAtlasItem.getSelectedSlice(atlas, levelRef.get().dimension());
            //TODO: very ugly and wont work in many cases
            MapDataHolder toRemove = getMapToRemove(inv, maps, slice);
            return toRemove.createExistingMapItem();
        }
        if (MapAtlasItem.getEmptyMaps(atlas) > 0) {
            return new ItemStack(Items.MAP);
        }
        //should never run
        return ItemStack.EMPTY;
    }

    private static MapDataHolder getMapToRemove(CraftingInput inv, MapCollection maps, Slice slice) {
        if (inv instanceof ICraftingInputWithContext ct) {
            AbstractContainerMenu menu = ct.mapAtlases$getMenu();
            if (menu instanceof CraftingMenu cm) {
                MapDataHolder c = maps.getClosest(cm.player, slice);
                if (c != null) {
                    return c;
                }
            } else if (menu instanceof InventoryMenu im) {
                MapDataHolder c = maps.getClosest(im.owner, slice);
                if (c != null) {
                    return c;
                }
            }
        }
        return maps.getAll().stream().findAny().get();
    }


    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput inv) {
        NonNullList<ItemStack> list = NonNullList.create();
        for (ItemStack i : inv.items()) {
            ItemStack stack = i.copy();

            if (stack.getItem() == Items.SHEARS) {
                AtomicReference<Boolean> broken = new AtomicReference<>(false);
                stack.hurtAndBreak(1, (ServerLevel) levelRef.get(), null, s-> broken.set(true));
                if (broken.get()) {
                    stack = ItemStack.EMPTY;
                }
//TODO: test
            } else if (stack.is(MapAtlasesMod.MAP_ATLAS.get())) {
                boolean didRemoveFilled = false;
                MapCollection maps = MapAtlasItem.getMaps(stack, levelRef.get());
                if (!maps.isEmpty()) {
                    Slice slice = MapAtlasItem.getSelectedSlice(stack, levelRef.get().dimension());
                    MapDataHolder toRemove = getMapToRemove(inv, maps, slice);
                    maps = maps.removeAndAssigns(stack, levelRef.get(), toRemove.id, toRemove.type);
                    var tree = maps.getHeightTree(slice.dimension(), slice.type());
                    if (!tree.contains(slice.heightOrTop())) {
                        Optional<Integer> first = tree.stream().findFirst();
                        if (first.isPresent()) {
                            Integer newH = first.get();
                            MapAtlasItem.setSelectedSlice(stack, Slice.of(slice.type(),
                                    newH, slice.dimension()));
                        }
                    }
                    didRemoveFilled = true;
                }
                int emptyMaps = MapAtlasItem.getEmptyMaps(stack);
                if (emptyMaps > 0 && !didRemoveFilled) {
                    int multiplier = MapAtlasesConfig.mapEntryValueMultiplier.get();
                    int amountToSet = Math.max(emptyMaps - multiplier, 0);
                    MapAtlasItem.setEmptyMaps(stack, amountToSet);
                }
            }
            list.add(stack);
        }
        return list;
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
