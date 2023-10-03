package pepjebs.mapatlases.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.capabilities.MapCollectionCap;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.Slice;

import java.lang.ref.WeakReference;

public class MapAtlasesCutExistingRecipe extends CustomRecipe {

    private WeakReference<Level> levelRef = new WeakReference<>(null);

    public MapAtlasesCutExistingRecipe(ResourceLocation id) {
        super(id);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        ItemStack atlas = ItemStack.EMPTY;
        ItemStack shears = ItemStack.EMPTY;
        for (int j = 0; j < inv.getContainerSize(); j++) {
            var i = inv.getItem(j);
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
    public ItemStack assemble(CraftingContainer inv) {
        ItemStack atlas = ItemStack.EMPTY;
        for (int j = 0; j < inv.getContainerSize(); j++) {
            var i = inv.getItem(j);
            if (i.is(MapAtlasesMod.MAP_ATLAS.get())) {
                atlas = i;
                break;
            }
        }
        MapCollectionCap maps = MapAtlasItem.getMaps(atlas, levelRef.get());
        if (maps.getCount() > 1) {
            var slice = MapAtlasItem.getSelectedSlice(atlas, levelRef.get().dimension());
            //TODO: very ugly and wont work in many cases
            String stringId = getMapToRemove(inv, maps, slice);
            int mapId = MapAtlasesAccessUtils.findMapIntFromString(stringId);
            return MapAtlasesAccessUtils.createMapItemStackFromId(mapId);
        }
        if (MapAtlasItem.getEmptyMaps(atlas) > 0) {
            return new ItemStack(Items.MAP);
        }
        //should never run
        return ItemStack.EMPTY;
    }

    private static String getMapToRemove(CraftingContainer inv, MapCollectionCap maps, Slice slice) {
        String stringId = "";// maps.getActive().getFirst();
        if (inv instanceof TransientCraftingContainer tc) {
            try {
                if (tc.menu instanceof CraftingMenu cm) {
                    MapDataHolder c = maps.getClosest(cm.player, slice);
                    if (c != null) {
                        stringId = c.getFirst();
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (stringId.isEmpty()) {
            stringId = maps.getAll().stream().findAny().get().getFirst();
        }
        return stringId;
    }


    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer inv) {
        NonNullList<ItemStack> list = NonNullList.create();
        for (int j = 0; j < inv.getContainerSize(); j++) {
            var i = inv.getItem(j);
            ItemStack stack = i.copy();

            if (stack.getItem() == Items.SHEARS) {
                stack.hurt(1, RandomSource.create(), null);
            } else if (stack.is(MapAtlasesMod.MAP_ATLAS.get())) {
                boolean didRemoveFilled = false;
                MapCollectionCap maps = MapAtlasItem.getMaps(stack, levelRef.get());
                if (!maps.isEmpty()) {
                    var slice = MapAtlasItem.getSelectedSlice(stack, levelRef.get().dimension());
                    maps.remove(getMapToRemove(inv, maps, slice));
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
