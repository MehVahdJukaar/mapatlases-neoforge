package pepjebs.mapatlases.integration;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.forge.REIPluginClient;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCraftingDisplay;

@REIPluginClient
public class REICompat implements REIClientPlugin {

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        SpecialRecipeDisplays.registerCraftingRecipes(l -> l.forEach(r -> registry.add(DefaultCraftingDisplay.of(r))));
    }
}
