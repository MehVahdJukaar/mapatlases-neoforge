package pepjebs.mapatlases;


import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.SupplementariesCompat;
import pepjebs.mapatlases.integration.moonlight.MoonlightCompat;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.networking.MapAtlasesNetworking;
import pepjebs.mapatlases.recipe.AntiqueAtlasRecipe;
import pepjebs.mapatlases.recipe.MapAtlasCreateRecipe;
import pepjebs.mapatlases.recipe.MapAtlasesAddRecipe;
import pepjebs.mapatlases.recipe.MapAtlasesCutExistingRecipe;
import pepjebs.mapatlases.utils.TriState;

import java.util.function.Supplier;
import java.util.stream.IntStream;


public class MapAtlasesMod {

    public static final String MOD_ID = "map_atlases";
    public static final Logger LOGGER = LogManager.getLogger("Map Atlases");

    public static final Supplier<MapAtlasItem> MAP_ATLAS;
    public static final Supplier<DataComponentType<int[]>> ATLAS_MAP_IDS;

    public static final Supplier<RecipeSerializer<MapAtlasCreateRecipe>> MAP_ATLAS_CREATE_RECIPE;
    public static final Supplier<RecipeSerializer<MapAtlasesAddRecipe>> MAP_ATLAS_ADD_RECIPE;
    public static final Supplier<RecipeSerializer<MapAtlasesCutExistingRecipe>> MAP_ATLAS_CUT_RECIPE;
    public static final Supplier<RecipeSerializer<AntiqueAtlasRecipe>> MAP_ANTIQUE_RECIPE;

    public static final Supplier<SoundEvent> ATLAS_OPEN_SOUND_EVENT = RegHelper.registerSound(res("atlas_open"));
    public static final Supplier<SoundEvent> ATLAS_PAGE_TURN_SOUND_EVENT = RegHelper.registerSound(res("atlas_page_turn"));
    public static final Supplier<SoundEvent> ATLAS_CREATE_MAP_SOUND_EVENT = RegHelper.registerSound(res("atlas_create_map"));

    public static final TagKey<Item> STICKY_ITEMS = TagKey.create(Registries.ITEM, res("sticky_crafting_items"));

    public static final boolean CURIOS = PlatHelper.isModLoaded("curios");
    public static final boolean TRINKETS = PlatHelper.isModLoaded("trinkets");
    public static final boolean SUPPLEMENTARIES = PlatHelper.isModLoaded("supplementaries");
    public static final boolean MOONLIGHT = PlatHelper.isModLoaded("moonlight");
    public static final boolean TWILIGHTFOREST = PlatHelper.isModLoaded("twilightforest");
    public static final boolean IMMEDIATELY_FAST = PlatHelper.isModLoaded("immediatelyfast");

    public static void init() {
        MapAtlasesNetworking.init();

        MapAtlasesConfig.init();
        if (PlatHelper.getPhysicalSide().isClient()) {
            MapAtlasesClientConfig.init();
            MapAtlasesClient.init();
        }
        RegHelper.addItemsToTabsRegistration(MapAtlasesMod::addItemsToTabs);

        //TODO
        //make map texture updates happen way less frequently. Delay upload maybe
        //lectern marker
        //sound
        //soap clear recipe
        //spyglass zoom in curio with keybind
        //auto waystone marker
        //interdimensional marker
        //antique in cart table


        if (MOONLIGHT) MoonlightCompat.init();
        if (SUPPLEMENTARIES) SupplementariesCompat.init();
    }

    static {
        // Register special recipes
        MAP_ATLAS_CREATE_RECIPE = RegHelper.registerRecipeSerializer(res("crafting_atlas"),
                () -> MapAtlasCreateRecipe.SERIALIZER);
        MAP_ATLAS_ADD_RECIPE = RegHelper.registerRecipeSerializer(res("adding_atlas"),
                () -> MapAtlasesAddRecipe.SERIALIZER);
        MAP_ATLAS_CUT_RECIPE = RegHelper.registerRecipeSerializer(res("cutting_atlas"),
                () -> MapAtlasesCutExistingRecipe.SERIALIZER);
        MAP_ANTIQUE_RECIPE = RegHelper.registerRecipeSerializer(res("antique_atlas"),
                () -> AntiqueAtlasRecipe.SERIALIZER);
        // Register items
        MAP_ATLAS = RegHelper.registerItem(res("atlas"),
                () -> new MapAtlasItem(new Item.Properties()
                        .setId(ResourceKey.create(Registries.ITEM, res("atlas")))
                        .stacksTo(16)));
        ATLAS_MAP_IDS = registerDataComponent(res("atlas_map_ids"),
                () -> DataComponentType.<int[]>builder()
                        .persistent(com.mojang.serialization.Codec.INT_STREAM.xmap(IntStream::toArray, java.util.Arrays::stream))
                        .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(
                                com.mojang.serialization.Codec.INT_STREAM.xmap(IntStream::toArray, java.util.Arrays::stream)))
                        .cacheEncoding()
                        .build());

    }


    public static void addItemsToTabs(RegHelper.ItemToTabEvent event) {
        event.addAfter(CreativeModeTabs.TOOLS_AND_UTILITIES, i -> i.is(Items.MAP), MAP_ATLAS.get());
    }

    public static Identifier res(String name) {
        return Identifier.fromNamespaceAndPath(MOD_ID, name);
    }

    private static <T> Supplier<DataComponentType<T>> registerDataComponent(Identifier id, Supplier<DataComponentType<T>> supplier) {
        DataComponentType<T> value = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id, supplier.get());
        return () -> value;
    }

    public static TriState containsHack() {
        return hack;
    }

    public static void setMapInInventoryHack(TriState value) {
        hack = value;
    }


    private static TriState hack = TriState.PASS;

    public static boolean rangeCheck(int distance, int range, int scale) {
        return distance <= (range + 1 + scale) * (range + 1 + scale);
    }


}
