package pepjebs.mapatlases.config;

import net.minecraftforge.common.ForgeConfigSpec;
import pepjebs.mapatlases.utils.ActivationLocation;

import java.util.function.Supplier;

public class MapAtlasesConfig {



    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        maxMapCount = builder
                .comment("The maximum number of Maps (Filled & Empty combined) allowed to be inside an Atlas (-1 to disable).")
                .define("maxMapCount", 512);

        acceptPaperForEmptyMaps = builder
                .comment("If enabled, you can increase the Empty Map count by inserting Paper")
                .define("acceptPaperForEmptyMaps", false);

        requireEmptyMapsToExpand = builder
                .comment("If true, the Atlas is required to have spare Empty Maps stored to expand the Filled Map size")
                .define("requireEmptyMapsToExpand", true);

        mapEntryValueMultiplier = builder
                .comment("Controls how many usable Maps are added when you add a single Map to the Atlas")
                .defineInRange("mapEntryValueMultiplier", 1, 0, 64);

        pityActivationMapCount = builder
                .comment("Controls how many free Empty Maps you get for 'activating' an Inactive Atlas")
                .defineInRange("pityActivationMapCount", 9, 0, 64);


        enableEmptyMapEntryAndFill = builder
                .comment("If 'true', Atlases will be able to store Empty Maps and auto-fill them as you explore.")
                .define("enableEmptyMapEntryAndFill", true);

        activationLocation = builder
                .comment("Locations of where an atlas will be scanned for. By default only hotbar will be scanned")
                .defineEnum("activationLocations", ActivationLocation.HOTBAR_AND_HANDS);

        creativeTeleport = builder
                .comment("Allows players in creative to teleport using the atlas. Hold shift and press anywhere")
                .define("creative_teleport", true);

        pinMarkerId = builder.comment("Marker id associated with the red pin button on the atlas screen. Set to empty string to disable")
                .define("pin_marked_id", "minecraft:target_point");

        builder.push("update_logic");
       roundRobinUpdate = builder.comment("Update maps in simple round robin fashion instead of prioritizing the ones closer. Overrides configs below")
                .define("round_robin", false);

        builder.pop();

        spec = builder.build();
    }

    public static final Supplier<Integer> maxMapCount;
    public static final Supplier<Integer> mapEntryValueMultiplier;
    public static final Supplier<Integer> pityActivationMapCount;
    public static final Supplier<Boolean> requireEmptyMapsToExpand;
    public static final Supplier<Boolean> acceptPaperForEmptyMaps;
    public static final Supplier<Boolean> enableEmptyMapEntryAndFill;
    public static final Supplier<Boolean> creativeTeleport;
    public static final Supplier<Boolean> roundRobinUpdate;
    public static final Supplier<String> pinMarkerId;
    public static final Supplier<ActivationLocation> activationLocation;
    public static final ForgeConfigSpec spec;


}
