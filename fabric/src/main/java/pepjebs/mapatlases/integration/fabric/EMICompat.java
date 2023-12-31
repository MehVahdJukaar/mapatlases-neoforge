package pepjebs.mapatlases.integration.fabric;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import pepjebs.mapatlases.integration.SpecialRecipeDisplays;

public class EMICompat implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        SpecialRecipeDisplays.registerCraftingRecipes(recipes -> recipes.stream().map(r ->
                new EmiCraftingRecipe(
                        r.getIngredients().stream().map(EmiIngredient::of).toList(),
                        EmiStack.of(r.getResultItem(null)),
                        r.getId(),
                        r instanceof ShapelessRecipe
                )).forEach(registry::addRecipe));
    }
}
