package pepjebs.mapatlases.config;

import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigType;
import net.mehvahdjukaar.moonlight.api.platform.configs.ModConfigHolder;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.utils.ActivationLocation;

import java.util.function.Supplier;

public class MapAtlasesConfig {

    static {
        ConfigBuilder builder = ConfigBuilder.create(MapAtlasesMod.MOD_ID, ConfigType.COMMON_SYNCED);


        builder.push("general");


        maxMapCount = builder
                .comment("The maximum number of Maps (Filled & Empty combined) allowed to be inside an Atlas.")
                .define("max_map_count", 512, 0, 1000000);

        acceptPaperForEmptyMaps = builder
                .comment("If enabled, you can increase the Empty Map count by inserting Paper")
                .define("accept_paper_for_empty_maps", false);

        requireEmptyMapsToExpand = builder
                .comment("If true, the Atlas is required to have spare Empty Maps stored to expand the Filled Map size")
                .define("require_empty_maps_to_expand", true);

        pityActivationMapCount = builder
                .comment("Controls how many free Empty Maps you get for 'activating' an Inactive Atlas")
                .define("pity_activation_map_count", 9, 0, 64);


        requireSliceMaps = !PlatHelper.isModLoaded("supplementaries") ? () -> false :
                builder.comment("If active, when Supplementaries is installed, the atlas will need to be filled with slice maps to be able to create new sliced maps")
                        .define("requires_slice_maps", false);

        enableEmptyMapEntryAndFill = builder
                .comment("If 'true', Atlases will be able to store Empty Maps and auto-fill them as you explore.")
                .define("enable_empty_map_entry_and_fill", true);

        activationLocation = builder
                .comment("Locations of where an atlas will be scanned for. By default only hotbar will be scanned")
                .define("activation_locations", ActivationLocation.HOTBAR_AND_HANDS);

        creativeTeleport = builder
                .comment("Allows players in creative to teleport using the atlas. Hold shift and press anywhere")
                .define("creative_teleport", true);

        pinMarkerId = builder.comment("Marker id associated with the red pin button on the atlas screen. Set to empty string to disable")
                .define("pin_marked_id", "map_atlases:pin");

        lightMap = builder.comment("Shows light color on maps. Needs Moonlight lib")
                .define("light_map", false);

        entityRadar = builder.comment("Show nearby mobs on minimap. Needs matching client config also set")
                .define("mob_radar", false);

        builder.pop();
        builder.push("update_logic");
        roundRobinUpdate = builder.comment("Update maps in simple round robin fashion instead of prioritizing the ones closer. Overrides configs below")
                .define("round_robin", false);
        mapUpdatePerTick = builder.comment("Max of maps to update each tick. Increase to make maps update faster")
                .define("map_updates_per_tick", 1, 0, 9);

        mapRange = builder.comment("Range multiplier of the map update. Logic affects all maps, atlas or not. Change to make the range smaller or bigger")
                .define("map_range_multiplier", 1, 0.0001, 10);

        mapUpdateMultithreaded = builder.comment("Makes map update on different threads, speeding up the process. Disable if it causes issues. Especially on servers. Try turning on for a big performance improvement regarding map atlas update")
                .define("multithreaded_update", UpdateType.SINGLE_PLAYER_ONLY);
        debugUpdate = builder.comment("Visually shows map updates")
                .define("debug_map_updates", false);
        markersUpdatePeriod = builder.comment("Every how many ticks should markers be updated")
                .define("markers_update_period", 10, 1, 200);

        builder.pop();


        SPEC = builder.build();
    }

    public static final Supplier<Boolean> debugUpdate;
    public static final Supplier<Integer> markersUpdatePeriod;
    public static final Supplier<UpdateType> mapUpdateMultithreaded;
    public static final Supplier<Integer> maxMapCount;
    public static final Supplier<Integer> pityActivationMapCount;
    public static final Supplier<Boolean> requireSliceMaps;
    public static final Supplier<Boolean> requireEmptyMapsToExpand;
    public static final Supplier<Boolean> acceptPaperForEmptyMaps;
    public static final Supplier<Boolean> enableEmptyMapEntryAndFill;
    public static final Supplier<Boolean> creativeTeleport;
    public static final Supplier<Boolean> roundRobinUpdate;
    public static final Supplier<Boolean> lightMap;
    public static final Supplier<Boolean> entityRadar;
    public static final Supplier<String> pinMarkerId;
    public static final Supplier<Integer> mapUpdatePerTick;
    public static final Supplier<Double> mapRange;
    public static final Supplier<ActivationLocation> activationLocation;

    public static final ModConfigHolder SPEC;

    public enum UpdateType {
        OFF, SINGLE_PLAYER_ONLY, ALWAYS_ON
    }

    public static void init() {

    }
}
