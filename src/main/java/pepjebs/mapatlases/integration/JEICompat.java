package pepjebs.mapatlases.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.forge.platform.PlatformHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.fml.ModList;
import pepjebs.mapatlases.MapAtlasesMod;

import java.util.List;

@JeiPlugin
public class JEICompat implements IModPlugin {

    private static final ResourceLocation ID = MapAtlasesMod.res("jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registry) {
        if (!ModList.get().isLoaded("roughly_enough_items")) {
            SpecialRecipeDisplays.registerCraftingRecipes(r -> registry.addRecipes(RecipeTypes.CRAFTING, r));
        }
    }


}