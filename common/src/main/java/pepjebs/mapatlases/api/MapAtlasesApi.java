package pepjebs.mapatlases.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.SupplementariesCompat;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.map_collection.MapGridKey;
import pepjebs.mapatlases.utils.AtlasLookup;
import pepjebs.mapatlases.utils.AtlasMap;
import pepjebs.mapatlases.utils.Slice;

public class MapAtlasesApi {

    public static boolean isAtlas(ItemStack stack) {
        return stack.is(MapAtlasesMod.MAP_ATLAS.get());
    }

    public static boolean isAtlas(Item item) {
        return item == MapAtlasesMod.MAP_ATLAS.get();
    }

    /**
     * the atlas the player would use for the hud
     */
    public static ItemStack getActiveAtlas(Player player) {
        return AtlasLookup.getAtlasFromPlayerByConfig(player);
    }

    /**
     * map data at the given position, using the slice currently selected in the atlas for that level
     */
    @Nullable
    public static MapItemSavedData getMapDataAt(ItemStack atlas, Level level, double x, double z) {
        if (!isAtlas(atlas)) return null;
        MapCollection maps = MapAtlasItem.getMaps(atlas, level);
        if (maps.isEmpty()) return null;
        Slice slice = MapAtlasItem.getSelectedSlice(atlas, level.dimension());
        AtlasMap map = maps.getMapAt(MapGridKey.at(maps.getScale(), slice, x, z));
        return map == null ? null : map.data;
    }

    @Nullable
    public static MapId getMapId(ItemStack atlas, Level level, MapItemSavedData data) {
        if (!isAtlas(atlas)) return null;
        for (AtlasMap map : MapAtlasItem.getMaps(atlas, level).getAllFound()) {
            if (map.data == data) return map.id;
        }
        return null;
    }

    public static boolean canPlayerSeeDeathMarker(Player player) {
        return SupplementariesCompat.canPlayerSeeDeathMarker(player);
    }
}
