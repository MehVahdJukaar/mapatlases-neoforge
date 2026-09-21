package pepjebs.mapatlases.item.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.SupplementariesCompat;

public class AntiqueAtlasRecipe extends CustomRecipe {

    public AntiqueAtlasRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput inv, Level level) {
        if (!MapAtlasesMod.SUPPLEMENTARIES) return false;
        ItemStack atlas = ItemStack.EMPTY;
        ItemStack ink = ItemStack.EMPTY;
        // ensure 1 and one only atlas
        for (int j = 0; j < inv.size(); ++j) {
            ItemStack itemstack = inv.getItem(j);
            if (itemstack.is(MapAtlasesMod.MAP_ATLAS.get())) {
                if (!atlas.isEmpty()) return false;
                if (SupplementariesCompat.hasAntiqueInk(itemstack)) return false;
                atlas = itemstack;
            } else if (SupplementariesCompat.isAntiqueInk(itemstack)) {
                if (!ink.isEmpty()) return false;
                ink = itemstack;
            } else if (!itemstack.isEmpty()) return false;
        }
        return !atlas.isEmpty() && !ink.isEmpty();
    }

    // this runs on every grid change, so the maps themselves are only swapped in MapAtlasItem.onCraftedBy
    @Override
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider registries) {
        for (int j = 0; j < inv.size(); ++j) {
            ItemStack itemstack = inv.getItem(j);
            if (itemstack.is(MapAtlasesMod.MAP_ATLAS.get())) {
                ItemStack newAtlas = itemstack.copyWithCount(1);
                SupplementariesCompat.setAntiqueInk(newAtlas);
                return newAtlas;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MapAtlasesMod.MAP_ANTIQUE_RECIPE.get();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

}
