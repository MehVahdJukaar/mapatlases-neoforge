package pepjebs.mapatlases.integration;

import net.minecraft.client.Minecraft;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.recipe.MapAtlasCreateRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SpecialRecipeDisplays {


    //TODO: add other 2
    private static List<CraftingRecipe> makeAtlasCreateRecipe() {

        var rec = Minecraft.getInstance().level.getRecipeManager().byKey(MapAtlasesMod.res("craft_atlas"));

        List<CraftingRecipe> recipes = new ArrayList<>();

        if (rec.isPresent() && rec.get() instanceof MapAtlasCreateRecipe create) {

            String group = "map_atlases.create_atlas";

            var ing = create.getIngredients();
            List<Ingredient> l = new ArrayList<>(ing);
            l.add(Ingredient.of(new ItemStack(Items.FILLED_MAP)));

            NonNullList<Ingredient> inputs = NonNullList.of(Ingredient.EMPTY, l.toArray(Ingredient[]::new));
            ResourceLocation id = new ResourceLocation(MapAtlasesMod.MOD_ID, "craft_atlas");
            ShapelessRecipe recipe = new ShapelessRecipe(id, group, CraftingBookCategory.MISC, new ItemStack(MapAtlasesMod.MAP_ATLAS.get()), inputs);
            recipes.add(recipe);
        }
        return recipes;
    }


    public static void registerCraftingRecipes(Consumer<List<CraftingRecipe>> registry) {
        for (var c : RecipeBookCategories.AGGREGATE_CATEGORIES.get(RecipeBookCategories.CRAFTING_SEARCH)) {
            registerRecipes(c, registry);
        }
    }

    public static void registerRecipes(RecipeBookCategories category, Consumer<List<CraftingRecipe>> registry) {

        if (category == RecipeBookCategories.CRAFTING_MISC) {
            registry.accept(makeAtlasCreateRecipe());
        }
    }
}
