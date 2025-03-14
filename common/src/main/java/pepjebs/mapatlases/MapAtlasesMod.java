package pepjebs.mapatlases;


import com.mojang.serialization.Codec;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.SupplementariesCompat;
import pepjebs.mapatlases.integration.moonlight.MoonlightCompat;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.EmptyMaps;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.networking.MapAtlasesNetworking;
import pepjebs.mapatlases.recipe.AntiqueAtlasRecipe;
import pepjebs.mapatlases.recipe.MapAtlasCreateRecipe;
import pepjebs.mapatlases.recipe.MapAtlasesAddRecipe;
import pepjebs.mapatlases.recipe.MapAtlasesCutExistingRecipe;
import pepjebs.mapatlases.map_collection.SelectedSlice;
import pepjebs.mapatlases.utils.TriState;

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
    
    public static final Supplier<DataComponentType<MapCollection>> MAP_COLLECTION = RegHelper.registerDataComponent(
            res("map_collection"), () -> DataComponentType.<MapCollection>builder()
                    .networkSynchronized(MapCollection.STREAM_CODEC)
                    .persistent(MapCollection.CODEC).build()
    );

    public static final Supplier<DataComponentType<Unit>> LOCKED = RegHelper.registerDataComponent(
            res("locked"), () -> DataComponentType.<Unit>builder()
                    .networkSynchronized(StreamCodec.unit(Unit.INSTANCE))
                    .persistent(Unit.CODEC).build()
    );

    public static final Supplier<DataComponentType<EmptyMaps>> EMPTY_MAPS = RegHelper.registerDataComponent(
            res("empty_maps"), () -> DataComponentType.<EmptyMaps>builder()
                    .networkSynchronized(EmptyMaps.STREAM_CODEC)
                    .persistent(EmptyMaps.CODEC).build()
    );

    public static final Supplier<DataComponentType<Integer>> HEIGHT = RegHelper.registerDataComponent(
            res("height"), () -> DataComponentType.<Integer>builder()
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .persistent(Codec.INT).build()
    );

    public static final Supplier<DataComponentType<SelectedSlice>> SELECTED_SLICE = RegHelper.registerDataComponent(
            res("selected_slice"), () -> DataComponentType.<SelectedSlice>builder()
                    .networkSynchronized(SelectedSlice.STREAM_CODEC)
                    .persistent(SelectedSlice.CODEC).build()
    );

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
                MapAtlasCreateRecipe.Serializer::new);
        MAP_ATLAS_ADD_RECIPE = RegHelper.registerRecipeSerializer(res("adding_atlas"),
                () -> new SimpleCraftingRecipeSerializer<>(MapAtlasesAddRecipe::new));
        MAP_ATLAS_CUT_RECIPE = RegHelper.registerRecipeSerializer(res("cutting_atlas"),
                () -> new SimpleCraftingRecipeSerializer<>(MapAtlasesCutExistingRecipe::new));
        MAP_ANTIQUE_RECIPE = RegHelper.registerRecipeSerializer(res("antique_atlas"),
                () -> new SimpleCraftingRecipeSerializer<>(AntiqueAtlasRecipe::new));
        // Register items
        MAP_ATLAS = RegHelper.registerItem(res("atlas"),
                () -> new MapAtlasItem(new Item.Properties()
                        .component(EMPTY_MAPS.get(), EmptyMaps.EMPTY)
                        .component(MAP_COLLECTION.get(), MapCollection.EMPTY)
                        .stacksTo(16)));

    }


    public static void addItemsToTabs(RegHelper.ItemToTabEvent event) {
        event.addAfter(CreativeModeTabs.TOOLS_AND_UTILITIES, i -> i.is(Items.MAP), MAP_ATLAS.get());
    }

    public static ResourceLocation res(String name) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
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
