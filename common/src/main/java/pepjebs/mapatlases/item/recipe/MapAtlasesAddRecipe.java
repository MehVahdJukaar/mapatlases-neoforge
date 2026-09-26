package pepjebs.mapatlases.item.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.EmptyMaps;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.map_collection.MapGridKey;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.MapType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapAtlasesAddRecipe extends AbstractAtlasRecipe {

    public MapAtlasesAddRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput inv, Level level) {
        ItemStack atlas = ItemStack.EMPTY;
        int newEmptyCount = 0;
        List<MapDataHolder> filledMaps = new ArrayList<>();
        // ensure 1 and one only atlas
        for (int j = 0; j < inv.size(); ++j) {
            ItemStack itemstack = inv.getItem(j);
            if (itemstack.is(MapAtlasesMod.MAP_ATLAS.get())) {
                if (!atlas.isEmpty()) return false;
                atlas = itemstack;
            } else if (MapAtlasesAccessUtils.isValidFilledMap(itemstack)) {
                filledMaps.add(MapAtlasesAccessUtils.findMapFromItemStack(level, itemstack));
            } else {
                MapType mapType = MapAtlasesAccessUtils.getEmptyMapType(itemstack);
                if (mapType != null) {
                    //increment empty
                    newEmptyCount++;
                } else if (!itemstack.isEmpty()) return false;
            }

        }
        if (!atlas.isEmpty() && (newEmptyCount != 0 || !filledMaps.isEmpty())) {

            int extraMaps = newEmptyCount + filledMaps.size();

            // Ensure we're not trying to add too many Maps
            if (extraMaps > MapAtlasItem.getFreeMapSlots(atlas, level)) return false;
            MapCollection maps = MapAtlasItem.getMaps(atlas, level);
            Integer atlasScale = maps.isEmpty() ? null : (int) maps.getScale();

            // Ensure Filled Maps are all same Scale & no duplicates, neither with the atlas nor within the grid
            Set<MapGridKey> gridKeys = new HashSet<>();
            for (var d : filledMaps) {
                if (d == null) return false;
                if (atlasScale == null) atlasScale = (int) d.data.scale;
                if (d.data.scale != atlasScale) return false;
                MapGridKey key = d.makeKey();
                if (maps.select(key) != null) return false;
                if (!gridKeys.add(key)) return false;
            }
            rememberLevel(level);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider registries) {

        Level level = getLevel();
        if (level == null) return ItemStack.EMPTY;
        ItemStack atlas = ItemStack.EMPTY;
        Map<MapType, Integer> emptyMapCount = new HashMap<>();
        Map<MapType, List<MapId>> mapIds = new HashMap<>();
        // ensure 1 and one only atlas
        for (int j = 0; j < inv.size(); ++j) {
            ItemStack itemstack = inv.getItem(j);
            if (itemstack.is(MapAtlasesMod.MAP_ATLAS.get())) {
                atlas = itemstack.copyWithCount(1);
            } else if (MapAtlasesAccessUtils.isValidFilledMap(itemstack)) {
                MapType mapType = MapType.fromFilledMap(itemstack.getItem());
                MapId mapId = itemstack.get(DataComponents.MAP_ID);
                mapIds.computeIfAbsent(mapType, k -> new ArrayList<>()).add(mapId);
            }else{
                MapType mapType = MapAtlasesAccessUtils.getEmptyMapType(itemstack);
                if (mapType != null) {
                    emptyMapCount.put(mapType, emptyMapCount.getOrDefault(mapType, 0) + 1);
                }
            }
        }

        // Get the Map Ids in the Grid
        // Set NBT Data
        MapCollection maps = MapAtlasItem.getMaps(atlas, level);
        maps.addAndAssigns(atlas, level, mapIds);

        EmptyMaps em = MapAtlasItem.getEmptyMaps(atlas);
        em.addAndAssigns(atlas, emptyMapCount);

        return atlas;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MapAtlasesMod.MAP_ATLAS_ADD_RECIPE.get();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }
}
