package pepjebs.mapatlases.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import pepjebs.mapatlases.MapAtlasesMod;

@Config(name = MapAtlasesMod.MOD_ID)
public class MapAtlasesConfig implements ConfigData {

    @ConfigEntry.Gui.Tooltip()
    @Comment("The maximum number of Maps (Filled & Empty combined) allowed to be inside an Atlas (-1 to disable).")
    public int maxMapCount = 512;

    @ConfigEntry.Gui.Tooltip()
    @Comment("If true, the Atlas is required to have spare Empty Maps stored to expand the Filled Map size")
    public boolean requireEmptyMapsToExpand = true;

    @ConfigEntry.Gui.Tooltip()
    @Comment("Scale the mini-map to a given % of the height of your screen.")
    public int forceMiniMapScaling = 30;

    @ConfigEntry.Gui.Tooltip()
    @Comment("If 'true', the Mini-Map of the Active Map will be drawn on the HUD while the Atlas is active.")
    public boolean drawMiniMapHUD = true;

    @ConfigEntry.Gui.Tooltip()
    @Comment("If 'true', Atlases will be able to store Empty Maps and auto-fill them as you explore.")
    public boolean enableEmptyMapEntryAndFill = true;

    @ConfigEntry.Gui.Tooltip()
    @Comment("Controls location where mini-map displays. Any of: 'HANDS', 'HOTBAR', or 'INVENTORY'.")
    public String activationLocation = "HOTBAR";

    @ConfigEntry.Gui.Tooltip()
    @Comment("Scale the world-map to a given % of the height of your screen.")
    public int forceWorldMapScaling = 80;

    @ConfigEntry.Gui.Tooltip()
    @Comment("Set to any of 'Upper'/'Lower' & 'Left'/'Right' to control anchor position of mini-map")
    public String miniMapAnchoring = "UpperLeft";

    @ConfigEntry.Gui.Tooltip()
    @Comment("Enter an integer which will offset the mini-map horizontally")
    public int miniMapHorizontalOffset = 5;

    @ConfigEntry.Gui.Tooltip()
    @Comment("Enter an integer which will offset the mini-map vertically")
    public int miniMapVerticalOffset = 5;

    @ConfigEntry.Gui.Tooltip()
    @Comment("Controls how many usable Maps are added when you add a single Map to the Atlas")
    public int mapEntryValueMultiplier = 1;

    @ConfigEntry.Gui.Tooltip()
    @Comment("Controls how many free Empty Maps you get for 'activating' an Inactive Atlas")
    public int pityActivationMapCount = 9;

    @ConfigEntry.Gui.Tooltip()
    @Comment("The number of pixels to shift vertically when there's an active effect")
    public int activePotionVerticalOffset = 26;

    @ConfigEntry.Gui.Tooltip()
    @Comment("When enabled, the player's current Coords will be displayed")
    public boolean drawMinimapCoords = true;

    @ConfigEntry.Gui.Tooltip()
    @Comment("When enabled, the player's current Biome will be displayed")
    public boolean drawMinimapBiome = true;

    @ConfigEntry.Gui.Tooltip()
    @Comment("Sets the scale of the text rendered for Coords and Biome mini-map data")
    public float minimapCoordsAndBiomeScale = 1.0f;

    @ConfigEntry.Gui.Tooltip()
    @Comment("When enabled, the Atlas world map coordinates will be displayed")
    public boolean drawWorldMapCoords = true;

    @ConfigEntry.Gui.Tooltip()
    @Comment("Sets the scale of the text rendered for Coords world-map data")
    public float worldMapCoordsScale = 1.0f;

    @ConfigEntry.Gui.Tooltip()
    @Comment("Sets the scale of the map icons rendered in the mini-map")
    public float miniMapIconScale = 1.0f;

    @ConfigEntry.Gui.Tooltip()
    @Comment("Sets the scale of the map icons rendered in the world-map")
    public float worldMapIconScale = 1.0f;

    @ConfigEntry.Gui.Tooltip()
    @Comment("If enabled, you can increase the Empty Map count by inserting Paper")
    public boolean acceptPaperForEmptyMaps = false;

    @ConfigEntry.Gui.Tooltip()
    @Comment("Multiplier for all the Atlases sound float")
    public float soundScalar = 1.0f;
}
