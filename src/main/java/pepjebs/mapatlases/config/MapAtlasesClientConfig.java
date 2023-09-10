package pepjebs.mapatlases.config;

import net.minecraftforge.common.ForgeConfigSpec;
import pepjebs.mapatlases.client.ActivationLocation;
import pepjebs.mapatlases.client.Anchoring;

import java.util.function.Supplier;

public class MapAtlasesClientConfig {


    public static final ForgeConfigSpec spec;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();



        forceMiniMapScaling = builder
                .comment("Scale the mini-map to a given % of the height of your screen.")
                .defineInRange("forceMiniMapScaling", 30, 0, 100);

        drawMiniMapHUD = builder
                .comment("If 'true', the Mini-Map of the Active Map will be drawn on the HUD while the Atlas is active.")
                .define("drawMiniMapHUD", true);


        miniMapAnchoring = builder
                .comment("Controls anchor position of mini-map")
                .defineEnum("miniMapAnchoring", Anchoring.UPPER_LEFT);

        miniMapHorizontalOffset = builder
                .comment("An integer which will offset the mini-map horizontally")
                .defineInRange("miniMapHorizontalOffset", 5, 0, 4000);

        miniMapVerticalOffset = builder
                .comment("An integer which will offset the mini-map vertically")
                .defineInRange("miniMapVerticalOffset", 5, 0, 4000);

        activePotionVerticalOffset = builder
                .comment("The number of pixels to shift vertically when there's an active effect")
                .defineInRange("activePotionVerticalOffset", 26, 0, 4000);

        drawMinimapCoords = builder
                .comment("When enabled, the player's current Coords will be displayed")
                .define("drawMinimapCoords", true);

        drawMinimapBiome = builder
                .comment("When enabled, the player's current Biome will be displayed")
                .define("drawMinimapBiome", true);

        drawWorldMapCoords = builder
                .comment("When enabled, the Atlas world map coordinates will be displayed")
                .define("drawWorldMapCoords", true);

        minimapCoordsAndBiomeScale = builder
                .comment("Sets the scale of the text rendered for Coords and Biome mini-map data")
                .defineInRange("minimapCoordsAndBiomeScale", 1, 0, 10d);

        worldMapCoordsScale = builder
                .comment("Sets the scale of the text rendered for Coords world-map data")
                .defineInRange("worldMapCoordsScale", 1, 0, 10d);

        miniMapDecorationScale = builder
                .comment("Sets the scale of the map icons rendered in the mini-map")
                .defineInRange("miniMapDecorationScale", 1, 0, 10d);

        worldMapDecorationScale = builder
                .comment("Sets the scale of the map icons rendered in the world-map")
                .defineInRange("worldMapDecorationScale", 1, 0, 10d);

        soundScalar = builder
                .comment("Multiplier for all the Atlases sound float")
                .defineInRange("soundScalar", 1, 0, 10d);

        spec = builder.build();
    }

    public static final Supplier<Boolean> drawMiniMapHUD;
    public static final Supplier<Integer> forceMiniMapScaling;
    public static final Supplier<Anchoring> miniMapAnchoring;
    public static final Supplier<Integer> miniMapHorizontalOffset;
    public static final Supplier<Integer> miniMapVerticalOffset;
    public static final Supplier<Integer> activePotionVerticalOffset;
    public static final Supplier<Boolean> drawMinimapCoords;
    public static final Supplier<Boolean> drawMinimapBiome;
    public static final Supplier<Boolean> drawWorldMapCoords;
    public static final Supplier<Double> minimapCoordsAndBiomeScale;
    public static final Supplier<Double> worldMapCoordsScale;
    public static final Supplier<Double> miniMapDecorationScale;
    public static final Supplier<Double> worldMapDecorationScale;
    public static final Supplier<Double> soundScalar;



}
