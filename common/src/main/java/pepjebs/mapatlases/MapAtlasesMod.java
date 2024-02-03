package pepjebs.mapatlases;


import net.mehvahdjukaar.moonlight.api.platform.PlatformHelper;
import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.SupplementariesCompat;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.networking.MapAtlasesNetworking;
import pepjebs.mapatlases.recipe.AntiqueAtlasRecipe;
import pepjebs.mapatlases.recipe.MapAtlasCreateRecipe;
import pepjebs.mapatlases.recipe.MapAtlasesAddRecipe;
import pepjebs.mapatlases.recipe.MapAtlasesCutExistingRecipe;

import java.util.function.Supplier;


public class MapAtlasesMod {

    public static final String MOD_ID = "map_atlases";
    public static final Logger LOGGER = LogManager.getLogger("Map Atlases");

    public static final Supplier<MapAtlasItem> MAP_ATLAS;

    public static final Supplier<RecipeSerializer<MapAtlasCreateRecipe>> MAP_ATLAS_CREATE_RECIPE;
    public static final Supplier<RecipeSerializer<MapAtlasesAddRecipe>> MAP_ATLAS_ADD_RECIPE;
    public static final Supplier<RecipeSerializer<MapAtlasesCutExistingRecipe>> MAP_ATLAS_CUT_RECIPE;
    public static final Supplier<RecipeSerializer<AntiqueAtlasRecipe>> MAP_ANTIQUE_RECIPE;

    public static final Supplier<SoundEvent> ATLAS_OPEN_SOUND_EVENT = RegHelper.registerSound(res("atlas_open"));
    public static final Supplier<SoundEvent> ATLAS_PAGE_TURN_SOUND_EVENT = RegHelper.registerSound(res("atlas_page_turn"));
    public static final Supplier<SoundEvent> ATLAS_CREATE_MAP_SOUND_EVENT = RegHelper.registerSound(res("atlas_create_map"));

    public static final TagKey<Item> STICKY_ITEMS = TagKey.create(Registry.ITEM_REGISTRY, res("sticky_crafting_items"));

    public static final boolean CURIOS = PlatformHelper.isModLoaded("curios");
    public static final boolean TRINKETS = PlatformHelper.isModLoaded("trinkets");
    public static final boolean SUPPLEMENTARIES = PlatformHelper.isModLoaded("supplementaries");
    public static final boolean TWILIGHTFOREST = PlatformHelper.isModLoaded("twilightforest");
    public static final boolean IMMEDIATELY_FAST = PlatformHelper.isModLoaded("immediatelyfast");
    public static final boolean MOONLIGHT = PlatformHelper.isModLoaded("moonlight");

    public static void init() {
        MapAtlasesNetworking.init();

        MapAtlasesConfig.init();
        if (PlatformHelper.getEnv().isClient()) {
            MapAtlasesClientConfig.init();
            MapAtlasesClient.init();
        }
        //TODO
        //make map texture updates happen way less frequently. Delay upload maybe
        //lectern marker
        //sound
        //soap clear recipe
        //spyglass zoom in curio with keybind
        //auto waystone marker
        //interdimensional marker
        //antique in cart table


        if (SUPPLEMENTARIES) SupplementariesCompat.init();
    }

    static {
        // Register special recipes
        MAP_ATLAS_CREATE_RECIPE = RegHelper.registerRecipeSerializer(res("crafting_atlas"),
                MapAtlasCreateRecipe.Serializer::new);
        MAP_ATLAS_ADD_RECIPE = RegHelper.registerRecipeSerializer(res("adding_atlas"),
                () -> new SimpleRecipeSerializer<>(MapAtlasesAddRecipe::new));
        MAP_ATLAS_CUT_RECIPE = RegHelper.registerRecipeSerializer(res("cutting_atlas"),
                () -> new SimpleRecipeSerializer<>(MapAtlasesCutExistingRecipe::new));
        MAP_ANTIQUE_RECIPE = RegHelper.registerRecipeSerializer(res("antique_atlas"),
                () -> new SimpleRecipeSerializer<>(AntiqueAtlasRecipe::new));
        // Register items
        MAP_ATLAS = RegHelper.registerItem(res("atlas"),
                () -> new MapAtlasItem(new Item.Properties().stacksTo(16)
                        .tab(CreativeModeTab.TAB_TOOLS)));

    }


    public static ResourceLocation res(String name) {
        return new ResourceLocation(MOD_ID, name);
    }

    public static InteractionResult containsHack() {
        return hack;
    }

    public static void setMapInInventoryHack(InteractionResult value) {
        hack = value;
    }


    private static InteractionResult hack = InteractionResult.PASS;


}
