package pepjebs.mapatlases.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.EmptyMaps;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.MapType;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapAtlasesAddRecipe extends CustomRecipe {

    private WeakReference<Level> levelRef = new WeakReference<>(null);

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
                MapType mapType = getEmptyMapType(itemstack);
                if (mapType != null) {
                    //increment empty
                    newEmptyCount++;
                } else if (!itemstack.isEmpty()) return false;
            }

        }
        if (!atlas.isEmpty() && (newEmptyCount != 0 || !filledMaps.isEmpty())) {

            int extraMaps = newEmptyCount + filledMaps.size();

            // Ensure we're not trying to add too many Maps
            MapCollection maps = MapAtlasItem.getMaps(atlas, level);
            EmptyMaps em = MapAtlasItem.getEmptyMaps(atlas);
            int oldCount = maps.getCount() + em.getSize();
            int maxMapCount = MapAtlasItem.getMaxMapCount();
            if (maxMapCount != -1 && oldCount + extraMaps - 1 > maxMapCount) {
                return false;
            }
            //ensure no duplicates

            int atlasScale = maps.getScale();

            // Ensure Filled Maps are all same Scale & Dimension
            for (var d : filledMaps) {
                if (d.data.scale != atlasScale) return false;
                if (maps.select(d.makeKey()) != null) return false;
            }
            levelRef = new WeakReference<>(level);
            return true;
        }
        return false;
    }

    @Nullable
    private MapType getEmptyMapType(ItemStack itemstack) {
        if (itemstack.isEmpty()) return null;
        MapType mapType = MapType.fromEmptyMap(itemstack.getItem());
        if (mapType != null && MapAtlasesConfig.enableEmptyMapEntryAndFill.get()) {
            return mapType;
        }
        if (itemstack.is(Items.PAPER) && MapAtlasesConfig.acceptPaperForEmptyMaps.get()) {
            return MapType.VANILLA;
        }
        return null;
    }

    @Override
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider registries) {

        Level level = levelRef.get();
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
                MapId mapId = mapType.getMapId(itemstack);
                mapIds.computeIfAbsent(mapType, k -> new ArrayList<>()).add(mapId);
            }else{
                MapType mapType = getEmptyMapType(itemstack);
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
