package pepjebs.mapatlases.config;

import net.minecraftforge.common.ForgeConfigSpec;
import pepjebs.mapatlases.client.Anchoring;
import pepjebs.mapatlases.client.InHandMode;

import java.util.function.Supplier;

public class MapAtlasesClientConfig {



    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();


        builder.push("minimap");

        hideWhenInHand = builder.comment("Hide minimap when holding the atlas in hand")
                .define("hide_when_in_hand", true);
        yOnlyWithSlice = builder.comment("Only display y coordinates if atlas holds some slice maps")
                .define("only_show_y_when_has_slices", true);

        miniMapScale = builder
                .comment("Global scale of entire minimap HUD. Keep at 1 for pixel perfect consistency")
                .defineInRange("scale", 1f, 0, 20);
        drawMiniMapHUD = builder
                .comment("If 'true', the Mini-Map of the Active Map will be drawn on the HUD while the Atlas is active.")
                .define("enabled", true);

        miniMapZoomMultiplier = builder.comment("How many maps to display in a single minimap. Essentially zoom. Can be a fraction")
                .defineInRange("zoom_multiplier", 1, 0.001, 100);

        miniMapAnchoring = builder
                .comment("Controls anchor position of mini-map")
                .defineEnum("anchoring", Anchoring.UPPER_LEFT);

        miniMapHorizontalOffset = builder
                .comment("An integer which will offset the mini-map horizontally")
                .defineInRange("horizontal_offset", 5, -4000, 4000);

        miniMapVerticalOffset = builder
                .comment("An integer which will offset the mini-map vertically")
                .defineInRange("vertical_offset", 5, -4000, 4000);

        activePotionVerticalOffset = builder
                .comment("The number of pixels to shift vertically when there's an active effect")
                .defineInRange("active_potion_effects_vertical_offset", 26, -4000, 4000);

        drawMinimapCoords = builder
                .comment("When enabled, the player's current Coords will be displayed")
                .define("coordinate_text", true);

        drawMinimapBiome = builder
                .comment("When enabled, the player's current Biome will be displayed")
                .define("biome_text", true);

        minimapCoordsAndBiomeScale = builder
                .comment("Sets the scale of the text rendered for Coords and Biome mini-map data")
                .defineInRange("coords_and_biome_scale", 1, 0, 10d);

        miniMapDecorationScale = builder
                .comment("Sets the scale of the map icons rendered in the mini-map")
                .defineInRange("decoration_scale", 1, 0, 10d);

        miniMapFollowPlayer = builder.comment("Allows minimap to follow player movement instead of only displaying current map")
                .define("follow_player", true);

        miniMapRotate = builder.comment("When enabled the map will align itself with the player")
                .define("rotate_with_player", true);
        drawMinimapCardinals = builder.comment("Draw cardinal directions on minimap")
                .define("cardinal_directions", true);
        miniMapCardinalsScale = builder.comment("Scale of cardinal directions on minimap")
                        .defineInRange("cardinal_scale", 1f, 0, 2);
        miniMapOnlyNorth = builder.comment("Only shows north cardinal direction")
                        .define("only_show_north_cardinal", false);
        miniMapBorder = builder.comment("Shows map separation borders")
                        .define("map_borders", true);

        minimapSkyLight = builder.comment("Use sky color for minimap")
                        .define("darken_at_night", false);

        mapChangeSound = builder.comment("Plays page turn sound when current active map changes. Works best when paired with no rotation and no player follow")
                        .define("map_change_sound", false);

        automaticSlice = builder.comment("Automatically switches to the nearest slice when possible")
                        .define("automatic_slice_change", false);
        builder.pop();

        builder.push("world_map");

        worldMapCrossair = builder.define("crossair", false);
        worldMapBigTexture = builder
                .comment("Use bigger book like texture for worldmap view. Makes the view a bit bigger." +
                        " Recommended to ebe used with map scale 1 (you might want to lower lectern one too if buttons dont show)")
                .define("alternative_texture", false);
        worldMapSmoothPanning = builder.comment("Pan smoothly. When off it will pan in map increments instead")
                .define("smooth_panning", true);
        worldMapSmoothZooming = builder.comment("Makes zooming work smoothly instead of in 2 maps increments")
                .define("smooth_zooming", true);
        worldMapZoomScrollSpeed = builder.define("zoom_scroll_speed", 1d);

        worldMapScale = builder
                .comment("Global scale of the entire world map GUI. Keep at 1 for pixel perfect consistency")
                .defineInRange("scale", 1.25f, 0, 20);
        lecternWorldMapScale = builder
                .comment("Global scale of the entire world map GUI when opening from lectern. Keep at 1 for pixel perfect consistency")
                .defineInRange("lectern_scale", 1f, 0, 20);

        worldMapBorder = builder.comment("Shows map separation borders")
                .define("map_borders", true);
        drawWorldMapCoords = builder
                .comment("When enabled, the Atlas world map coordinates will be displayed")
                .define("draw_coordinates", true);

        worldMapCoordsScale = builder
                .comment("Sets the scale of the text rendered for Coords world-map data")
                .defineInRange("coordinates_scale", 1, 0, 10d);

        worldMapDecorationScale = builder
                .comment("Sets the scale of the map icons rendered in the world-map")
                .defineInRange("decoration_scale", 1, 0, 10d);

        worldMapCompactSliceIndicator = builder
                .comment("Rearranges the position of the slice indicator to be more compact. You will need supplementaries slice maps to use this")
                .define("compact_slices_indicator", false);

        worldMapFollowPlayer = builder.comment("Allows minimap to follow player movement instead of only displaying current map")
                .define("follow_player", true);
        builder.pop();

        soundScalar = builder
                .comment("Multiplier for all the Atlases sound float")
                .defineInRange("soundScalar", 1, 0, 10d);

        mapTextureShrink = builder
                .comment("Proportional to how much map up will be stretched. Useful if you notice gaps appearing in the worldmap screen")
                .defineInRange("shrink_ratio", 0f, 0, 10);

        inHandMode = builder.comment("Render atlas like normal map when in hand")
                .defineEnum("in_hand_renderer", InHandMode.NOT_LOCKED);

        builder.push("moonlight_integration");
        moonlightCompat = builder
                .comment("Enables moonlight compat, which allows to place map markers on map via a special pin button")
                .define("enabled", true);
        moonlightPinTracking = builder.comment("Allows tracking pins by pressing control, making them follow you on minimap")
                .define("pin_tracking", true);
        spec = builder.build();
    }

    public static final Supplier<Boolean> drawMiniMapHUD;
    public static final Supplier<Double> miniMapZoomMultiplier;
    public static final Supplier<Anchoring> miniMapAnchoring;
    public static final Supplier<Integer> miniMapHorizontalOffset;
    public static final Supplier<Integer> miniMapVerticalOffset;
    public static final Supplier<Integer> activePotionVerticalOffset;
    public static final Supplier<Boolean> drawMinimapCoords;
    public static final Supplier<Boolean> drawMinimapBiome;
    public static final Supplier<Boolean> drawWorldMapCoords;
    public static final Supplier<Boolean> drawMinimapCardinals;
    public static final Supplier<Boolean> miniMapOnlyNorth;
    public static final Supplier<Boolean> miniMapBorder;
    public static final Supplier<Boolean> minimapSkyLight;
    public static final Supplier<Boolean> mapChangeSound;
    public static final Supplier<Boolean> automaticSlice;
    public static final Supplier<Boolean> worldMapBorder;
    public static final Supplier<Double> miniMapCardinalsScale;
    public static final Supplier<Double> minimapCoordsAndBiomeScale;
    public static final Supplier<Double> worldMapCoordsScale;
    public static final Supplier<Double> miniMapDecorationScale;
    public static final Supplier<Double> worldMapDecorationScale;
    public static final Supplier<Double> soundScalar;
    public static final Supplier<Double> mapTextureShrink;
    public static final Supplier<Boolean> worldMapCompactSliceIndicator;
    public static final Supplier<Boolean> miniMapRotate;
    public static final Supplier<Boolean> miniMapFollowPlayer;
    public static final Supplier<Boolean> worldMapFollowPlayer;
    public static final Supplier<Boolean> yOnlyWithSlice;
    public static final Supplier<Boolean> worldMapSmoothPanning;
    public static final Supplier<Boolean> worldMapSmoothZooming;
    public static final Supplier<Double> worldMapZoomScrollSpeed;
    public static final Supplier<Boolean> worldMapBigTexture;
    public static final Supplier<Boolean> worldMapCrossair;
    public static final Supplier<Boolean> hideWhenInHand;
    public static final Supplier<InHandMode> inHandMode;
    public static final Supplier<Double> miniMapScale;
    public static final Supplier<Double> worldMapScale;
    public static final Supplier<Double> lecternWorldMapScale;
    public static final Supplier<Boolean> moonlightCompat;
    public static final Supplier<Boolean> moonlightPinTracking;

    public static final ForgeConfigSpec spec;


}
