package pepjebs.mapatlases.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.item.recipe.MapAtlasCreateRecipe;

@JeiPlugin
public class JEICompat implements IModPlugin {

    private static final Identifier ID = MapAtlasesMod.res("jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return ID;
    }

    //jei skips special recipes unless something claims them
    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getCraftingCategory().addExtension(MapAtlasCreateRecipe.class,
                r -> ((ShapelessCraftingRecipeDisplay) r.value().display().getFirst()).ingredients());
    }

}
