package pepjebs.mapatlases.config;

import net.minecraftforge.common.ForgeConfigSpec;
import pepjebs.mapatlases.client.ActivationLocation;

import java.util.function.Supplier;

public class MapAtlasesConfig {


    public static final ForgeConfigSpec spec;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        maxMapCount = builder
                .comment("The maximum number of Maps (Filled & Empty combined) allowed to be inside an Atlas (-1 to disable).")
                .defineInRange("maxMapCount", 512, 0, 1024);

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


        spec = builder.build();
    }

    public static final Supplier<Integer> maxMapCount;
    public static final Supplier<Integer> mapEntryValueMultiplier;
    public static final Supplier<Integer> pityActivationMapCount;
    public static final Supplier<Boolean> requireEmptyMapsToExpand;
    public static final Supplier<Boolean> acceptPaperForEmptyMaps;
    public static final Supplier<Boolean> enableEmptyMapEntryAndFill;
    public static final Supplier<ActivationLocation> activationLocation;


}
