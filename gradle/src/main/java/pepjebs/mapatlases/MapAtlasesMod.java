package pepjebs.mapatlases;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.item.*;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.item.DummyFilledMap;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.lifecycle.MapAtlasesServerLifecycleEvents;
import pepjebs.mapatlases.recipe.MapAtlasCreateRecipe;
import pepjebs.mapatlases.recipe.MapAtlasesAddRecipe;
import pepjebs.mapatlases.recipe.MapAtlasesCutExistingRecipe;
import pepjebs.mapatlases.screen.MapAtlasesAtlasOverviewScreenHandler;
import pepjebs.mapatlases.networking.MapAtlasesOpenGUIC2SPacket;

public class MapAtlasesMod implements ModInitializer {

    public static final String MOD_ID = "map_atlases";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static MapAtlasesConfig CONFIG = null;

    public static MapAtlasItem MAP_ATLAS;

    public static SpecialRecipeSerializer<MapAtlasCreateRecipe> MAP_ATLAS_CREATE_RECIPE;
    public static SpecialRecipeSerializer<MapAtlasesAddRecipe> MAP_ATLAS_ADD_RECIPE;
    public static SpecialRecipeSerializer<MapAtlasesCutExistingRecipe> MAP_ATLAS_CUT_RECIPE;

    public static ScreenHandlerType<MapAtlasesAtlasOverviewScreenHandler> ATLAS_OVERVIEW_HANDLER;

    private static final Identifier ATLAS_OPEN_SOUND_ID = new Identifier(MOD_ID, "atlas_open");
    public static SoundEvent ATLAS_OPEN_SOUND_EVENT = SoundEvent.of(ATLAS_OPEN_SOUND_ID);
    private static final Identifier ATLAS_PAGE_TURN_SOUND_ID = new Identifier(MOD_ID, "atlas_page_turn");
    public static SoundEvent ATLAS_PAGE_TURN_SOUND_EVENT = SoundEvent.of(ATLAS_PAGE_TURN_SOUND_ID);
    private static final Identifier ATLAS_CREATE_MAP_SOUND_ID = new Identifier(MOD_ID, "atlas_create_map");
    public static SoundEvent ATLAS_CREATE_MAP_SOUND_EVENT = SoundEvent.of(ATLAS_CREATE_MAP_SOUND_ID);
    public static final Identifier STICKY_ITEMS_ID = new Identifier(MapAtlasesMod.MOD_ID, "sticky_crafting_items");

    public static final String TRINKETS_MOD_ID = "trinkets";

    @Override
    public void onInitialize() {
        // Register config
        AutoConfig.register(MapAtlasesConfig.class, JanksonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(MapAtlasesConfig.class).getConfig();

        // Register special recipes
        MAP_ATLAS_CREATE_RECIPE = Registry.register(Registries.RECIPE_SERIALIZER,
                new Identifier(MOD_ID, "crafting_atlas"), new SpecialRecipeSerializer<>(MapAtlasCreateRecipe::new));
        MAP_ATLAS_ADD_RECIPE = Registry.register(Registries.RECIPE_SERIALIZER,
                new Identifier(MOD_ID, "adding_atlas"), new SpecialRecipeSerializer<>(MapAtlasesAddRecipe::new));
        MAP_ATLAS_CUT_RECIPE = Registry.register(Registries.RECIPE_SERIALIZER,
                new Identifier(MOD_ID, "cutting_atlas"), new SpecialRecipeSerializer<>(MapAtlasesCutExistingRecipe::new));

        // Register screen
        ATLAS_OVERVIEW_HANDLER = Registry.register(
                Registries.SCREEN_HANDLER,
                new Identifier(MOD_ID, "atlas_overview"),
                new ExtendedScreenHandlerType<>(MapAtlasesAtlasOverviewScreenHandler::new)
        );

        // Register sounds
        Registry.register(Registries.SOUND_EVENT, ATLAS_OPEN_SOUND_ID, ATLAS_OPEN_SOUND_EVENT);
        Registry.register(Registries.SOUND_EVENT, ATLAS_PAGE_TURN_SOUND_ID, ATLAS_PAGE_TURN_SOUND_EVENT);
        Registry.register(Registries.SOUND_EVENT, ATLAS_CREATE_MAP_SOUND_ID, ATLAS_CREATE_MAP_SOUND_EVENT);

        // Register items
        Registry.register(Registries.ITEM, new Identifier(MOD_ID,"atlas"),
                new MapAtlasItem(new Item.Settings().maxCount(16)));
        MAP_ATLAS = (MapAtlasItem) Registries.ITEM.get(new Identifier(MapAtlasesMod.MOD_ID, "atlas"));
        Registry.register(Registries.ITEM, new Identifier(MOD_ID,"dummy_filled_map"),
                new DummyFilledMap(new Item.Settings()));

        // Register events/callbacks
        ServerPlayConnectionEvents.JOIN.register(MapAtlasesServerLifecycleEvents::mapAtlasPlayerJoin);
        ServerPlayNetworking.registerGlobalReceiver(MapAtlasesOpenGUIC2SPacket.MAP_ATLAS_OPEN_GUI,
                MapAtlasesServerLifecycleEvents::openGuiEvent);
        ServerTickEvents.START_SERVER_TICK.register(MapAtlasesServerLifecycleEvents::mapAtlasServerTick);
    }
}
