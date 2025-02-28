package pepjebs.mapatlases.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.ImmutableMapCollection;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MapAtlasesAddRecipe extends CustomRecipe {

    private WeakReference<Level> levelRef = new WeakReference<>(null);

    public MapAtlasesAddRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput inv, Level level) {
        ItemStack atlas = ItemStack.EMPTY;
        int emptyMaps = 0;
        List<MapDataHolder> filledMaps = new ArrayList<>();
        // ensure 1 and one only atlas
        for (int j = 0; j < inv.size(); ++j) {
            ItemStack itemstack = inv.getItem(j);
            if (itemstack.is(MapAtlasesMod.MAP_ATLAS.get())) {
                if (!atlas.isEmpty()) return false;
                atlas = itemstack;
            } else if (isEmptyMap(itemstack)) {
                emptyMaps++;
            } else if (MapAtlasesAccessUtils.isValidFilledMap(itemstack)) {
                filledMaps.add(MapAtlasesAccessUtils.findMapFromItemStack(level, itemstack));
            } else if (!itemstack.isEmpty()) return false;
        }
        if (!atlas.isEmpty() && (emptyMaps != 0 || !filledMaps.isEmpty())) {

            int extraMaps = emptyMaps + filledMaps.size();

            // Ensure we're not trying to add too many Maps
            ImmutableMapCollection maps = MapAtlasItem.getMaps(atlas, level);
            int mapCount = maps.getCount() + MapAtlasItem.getEmptyMaps(atlas);
            if (MapAtlasItem.getMaxMapCount() != -1 && mapCount + extraMaps - 1 > MapAtlasItem.getMaxMapCount()) {
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

    private boolean isEmptyMap(ItemStack itemstack) {
        if (itemstack.isEmpty()) return false;
        if (MapAtlasesAccessUtils.isValidEmptyMap(itemstack)) {
            return MapAtlasesConfig.enableEmptyMapEntryAndFill.get();
        }
        if (itemstack.is(Items.PAPER)) {
            return MapAtlasesConfig.acceptPaperForEmptyMaps.get();
        }
        return false;
    }

    @Override
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider registries) {

        Level level = levelRef.get();
        ItemStack atlas = ItemStack.EMPTY;
        int emptyMapCount = 0;
        List<Integer> mapIds = new ArrayList<>();
        // ensure 1 and one only atlas
        for (int j = 0; j < inv.size(); ++j) {
            ItemStack itemstack = inv.getItem(j);
            if (itemstack.is(MapAtlasesMod.MAP_ATLAS.get())) {
                atlas = itemstack.copyWithCount(1);
            } else if (isEmptyMap(itemstack)) {
                emptyMapCount++;
            } else if (MapAtlasesAccessUtils.isValidFilledMap(itemstack)) {
                mapIds.add(MapItem.getMapId(itemstack));
            }
        }

        // Get the Map Ids in the Grid
        // Set NBT Data
        emptyMapCount *= MapAtlasesConfig.mapEntryValueMultiplier.get();
        ImmutableMapCollection maps = MapAtlasItem.getMaps(atlas, level);
        maps.addAndAssigns(atlas, level, mapIds);

        MapAtlasItem.increaseEmptyMaps(atlas, emptyMapCount);
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
