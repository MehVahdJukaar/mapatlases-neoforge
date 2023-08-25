package pepjebs.mapatlases.recipe;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MapAtlasesCutExistingRecipe extends SpecialCraftingRecipe {

    public MapAtlasesCutExistingRecipe(Identifier id, CraftingRecipeCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(RecipeInputInventory inv, World world) {
        ItemStack atlas = ItemStack.EMPTY;
        ItemStack shears = ItemStack.EMPTY;
        int size = 0;
        for(int i = 0; i < inv.size(); i++) {
            if (!inv.getStack(i).isEmpty()) {
                size++;
                if (inv.getStack(i).getItem() == MapAtlasesMod.MAP_ATLAS) {
                    atlas = inv.getStack(i);
                } else if (inv.getStack(i).getItem() == Items.SHEARS) {
                    shears = inv.getStack(i);
                }
            }
        }
        return !atlas.isEmpty() && !shears.isEmpty() && shears.getDamage() < shears.getMaxDamage() - 1 && size == 2;
    }

    @Override
    public ItemStack craft(RecipeInputInventory inv, DynamicRegistryManager registryManager) {
        ItemStack atlas = ItemStack.EMPTY;
        for(int i = 0; i < inv.size(); i++) {
            if (!inv.getStack(i).isEmpty()) {
                if (inv.getStack(i).getItem() == MapAtlasesMod.MAP_ATLAS) {
                    atlas = inv.getStack(i);
                }
            }
        }
        if (atlas.getNbt() == null) return ItemStack.EMPTY;
        if (MapAtlasesAccessUtils.getMapCountFromItemStack(atlas) > 1) {
            List<Integer> mapIds = Arrays.stream(atlas.getNbt()
                    .getIntArray(MapAtlasItem.MAP_LIST_NBT)).boxed().collect(Collectors.toList());
            if (mapIds.size() > 0) {
                int lastId = mapIds.remove(mapIds.size() - 1);
                return MapAtlasesAccessUtils.createMapItemStackFromId(lastId);
            }
        }
        if (MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(atlas) > 0) {
            return new ItemStack(Items.MAP);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public DefaultedList<ItemStack> getRemainder(RecipeInputInventory inv) {
        DefaultedList<ItemStack> list = DefaultedList.of();
        for(int i = 0; i < inv.size(); i++) {
            ItemStack cur = inv.getStack(i).copy();
            if (cur.getItem() == Items.SHEARS) {
                cur.damage(1, Random.create(), null);
            } else if (cur.getItem() == MapAtlasesMod.MAP_ATLAS && cur.getNbt() != null) {
                boolean didRemoveFilled = false;
                if (MapAtlasesAccessUtils.getMapCountFromItemStack(cur) > 1) {
                    List<Integer> mapIds = Arrays.stream(cur.getNbt()
                            .getIntArray(MapAtlasItem.MAP_LIST_NBT)).boxed().collect(Collectors.toList());
                    if (mapIds.size() > 0) {
                        mapIds.remove(mapIds.size() - 1);
                        cur.getNbt().putIntArray(MapAtlasItem.MAP_LIST_NBT, mapIds);
                        didRemoveFilled = true;
                    }

                }
                if (MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(cur) > 0 && !didRemoveFilled) {
                    int multiplier = 1;
                    if (MapAtlasesMod.CONFIG != null) {
                        multiplier = MapAtlasesMod.CONFIG.mapEntryValueMultiplier;
                    }
                    int amountToSet = Math.max(cur.getNbt().getInt(MapAtlasItem.EMPTY_MAP_NBT) - multiplier, 0);
                    cur.getNbt().putInt(MapAtlasItem.EMPTY_MAP_NBT, amountToSet);
                }
            }
            list.add(cur);
        }
        return list;
    }

    @Override
    public boolean fits(int width, int height) {
        return width + height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MapAtlasesMod.MAP_ATLAS_CUT_RECIPE;
    }
}
