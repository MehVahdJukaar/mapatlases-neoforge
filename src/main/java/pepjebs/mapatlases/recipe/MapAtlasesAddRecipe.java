package pepjebs.mapatlases.recipe;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.capabilities.MapCollectionCap;
import pepjebs.mapatlases.capabilities.MapKey;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MapAtlasesAddRecipe extends CustomRecipe {

    private WeakReference<Level> levelRef = new WeakReference<>(null);

    public MapAtlasesAddRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        ItemStack atlas = ItemStack.EMPTY;
        int emptyMaps = 0;
        List<Pair<String, MapItemSavedData>> filledMaps = new ArrayList<>();
        // ensure 1 and one only atlas
        for (int j = 0; j < inv.getContainerSize(); ++j) {
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
            MapCollectionCap maps = MapAtlasItem.getMaps(atlas, level);
            int mapCount = maps.getCount() + MapAtlasItem.getEmptyMaps(atlas);
            if (MapAtlasItem.getMaxMapCount() != -1 && mapCount + extraMaps - 1 > MapAtlasItem.getMaxMapCount()) {
                return false;
            }
            //ensure no duplicates

            int atlasScale = maps.getScale();

            // Ensure Filled Maps are all same Scale & Dimension
            for (var d : filledMaps) {
                if (d.getSecond().scale != atlasScale) return false;
                if (maps.select(MapKey.of(d)) != null) return false;
            }
            levelRef = new WeakReference<>(level);
            return true;
        }
        return false;
    }

    private boolean isEmptyMap(ItemStack itemstack) {
        if (itemstack.isEmpty()) return false;
        if (itemstack.is(Items.MAP)) {
            return MapAtlasesConfig.enableEmptyMapEntryAndFill.get();
        }
        if (itemstack.is(Items.PAPER)) {
            return MapAtlasesConfig.acceptPaperForEmptyMaps.get();
        }
        return false;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess registryManager) {

        Level level = levelRef.get();
        ItemStack atlas = ItemStack.EMPTY;
        int emptyMapCount = 0;
        List<Integer> mapIds = new ArrayList<>();
        // ensure 1 and one only atlas
        for (int j = 0; j < inv.getContainerSize(); ++j) {
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
        MapCollectionCap maps = MapAtlasItem.getMaps(atlas, level);
        for (var i : mapIds) {
            maps.add(i, level);
        }

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

}
