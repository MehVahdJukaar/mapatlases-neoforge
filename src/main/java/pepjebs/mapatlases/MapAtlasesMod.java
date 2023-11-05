package pepjebs.mapatlases;


import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pepjebs.mapatlases.capabilities.MapCollectionCap;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.MoonlightCompat;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.lifecycle.MapAtlasesServerEvents;
import pepjebs.mapatlases.networking.MapAtlasesNetworking;
import pepjebs.mapatlases.recipe.MapAtlasCreateRecipe;
import pepjebs.mapatlases.recipe.MapAtlasesAddRecipe;
import pepjebs.mapatlases.recipe.MapAtlasesCutExistingRecipe;

import java.util.function.Supplier;


@Mod(MapAtlasesMod.MOD_ID)
public class MapAtlasesMod {

    public static final String MOD_ID = "map_atlases";
    public static final Logger LOGGER = LogManager.getLogger("Map Atlases");

    public static final Supplier<MapAtlasItem> MAP_ATLAS;

    private static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final Supplier<RecipeSerializer<MapAtlasCreateRecipe>> MAP_ATLAS_CREATE_RECIPE;
    public static final Supplier<RecipeSerializer<MapAtlasesAddRecipe>> MAP_ATLAS_ADD_RECIPE;
    public static final Supplier<RecipeSerializer<MapAtlasesCutExistingRecipe>> MAP_ATLAS_CUT_RECIPE;

    public static final Supplier<SoundEvent> ATLAS_OPEN_SOUND_EVENT = regSound("atlas_open");
    public static final Supplier<SoundEvent> ATLAS_PAGE_TURN_SOUND_EVENT = regSound("atlas_page_turn");
    public static final Supplier<SoundEvent> ATLAS_CREATE_MAP_SOUND_EVENT = regSound("atlas_create_map");

    public static final TagKey<Item> STICKY_ITEMS = ItemTags.create(res("sticky_crafting_items"));

    public static final boolean CURIOS = ModList.get().isLoaded("curios");
    public static final boolean TRINKETS = ModList.get().isLoaded("trinkets");
    public static final boolean SUPPLEMENTARIES = ModList.get().isLoaded("supplementaries");
    public static final boolean MOONLIGHT = ModList.get().isLoaded("moonlight");
    public static final boolean TWILIGHTFOREST = false;// ModList.get().isLoaded("twilightforest");

    public MapAtlasesMod(){
        //lectern marker
        //sound
        //spyglass zoom
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(MapCollectionCap::register);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            MapAtlasesClient.init();
        }

        if(MOONLIGHT) MoonlightCompat.init();

        RECIPES.register(bus);
        MENU_TYPES.register(bus);
        ITEMS.register(bus);
        SOUND_EVENTS.register(bus);

        // Register config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MapAtlasesConfig.spec);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, MapAtlasesClientConfig.spec);

        // Register special recipes
        MAP_ATLAS_CREATE_RECIPE = RECIPES.register("crafting_atlas", MapAtlasCreateRecipe.Serializer::new);
        MAP_ATLAS_ADD_RECIPE = RECIPES.register("adding_atlas",
                () -> new SimpleRecipeSerializer<>(MapAtlasesAddRecipe::new));
        MAP_ATLAS_CUT_RECIPE = RECIPES.register("cutting_atlas",
                () -> new SimpleRecipeSerializer<>(MapAtlasesCutExistingRecipe::new));

        // Register items
        MAP_ATLAS = ITEMS.register("atlas", () -> new MapAtlasItem(new Item.Properties()
                .tab(CreativeModeTab.TAB_TOOLS).stacksTo(16)));

        // Register messages
        MapAtlasesNetworking.register();

        MinecraftForge.EVENT_BUS.register(MapAtlasesServerEvents.class);
    }


    private static Supplier<SoundEvent> regSound(String name) {
        return SOUND_EVENTS.register(name, () -> new SoundEvent(res(name)));
    }

    public static ResourceLocation res(String name) {
        return new ResourceLocation(MOD_ID, name);
    }

    public static boolean containsHack() {
        return hack;
    }

    public static void setMapInInentoryHack(boolean value) {
        hack = value;
    }

    private static boolean hack = false;


}
