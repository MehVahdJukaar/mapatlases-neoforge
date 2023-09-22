package pepjebs.mapatlases.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.capabilities.MapCollectionCap;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.item.MapAtlasItem;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MapAtlasesAddRecipe extends CustomRecipe {

    private WeakReference<Level> levelRef = new WeakReference<>(null);

    public MapAtlasesAddRecipe(ResourceLocation id) {
        super(id);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        ItemStack atlas = ItemStack.EMPTY;
        int emptyMaps = 0;
        List<MapItemSavedData> filledMaps = new ArrayList<>();
        // ensure 1 and one only atlas
        for (int j = 0; j < inv.getContainerSize(); ++j) {
            ItemStack itemstack = inv.getItem(j);
            if (itemstack.is(MapAtlasesMod.MAP_ATLAS.get())) {
                if (!atlas.isEmpty()) return false;
                atlas = itemstack;
            } else if (isEmptyMap(itemstack)) {
                emptyMaps++;
            } else if (itemstack.is(Items.FILLED_MAP)) {
                filledMaps.add(MapItem.getSavedData(itemstack, level));
            }else if(!itemstack.isEmpty()) return false;
        }
        if (!atlas.isEmpty() && (emptyMaps != 0 || !filledMaps.isEmpty())) {

            int extraMaps = emptyMaps + filledMaps.size();

            // Ensure we're not trying to add too many Maps
            MapCollectionCap maps = MapAtlasItem.getMaps(atlas, level);
            int mapCount = maps.getCount() + MapAtlasItem.getEmptyMaps(atlas);
            if (MapAtlasItem.getMaxMapCount() != -1 && mapCount + extraMaps - 1 > MapAtlasItem.getMaxMapCount()) {
                return false;
            }

            int atlasScale = maps.getScale();

            // Ensure Filled Maps are all same Scale & Dimension
            for (var d : filledMaps) {
                if (d.scale != atlasScale) return false;
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
    public ItemStack assemble(CraftingContainer inv) {

        Level level = levelRef.get();
        ItemStack atlas = ItemStack.EMPTY;
        int emptyMapCount = 0;
        List<Integer> mapIds = new ArrayList<>();
        // ensure 1 and one only atlas
        for (int j = 0; j < inv.getContainerSize(); ++j) {
            ItemStack itemstack = inv.getItem(j);
            if (itemstack.is(MapAtlasesMod.MAP_ATLAS.get())) {
                atlas = itemstack.copy();
                atlas.setCount(1);
            } else if (isEmptyMap(itemstack)) {
                emptyMapCount++;
            } else if (itemstack.is(Items.FILLED_MAP)) {
                mapIds.add(MapItem.getMapId(itemstack));
            }
        }

        // Get the Map Ids in the Grid
        // Set NBT Data
        emptyMapCount *= MapAtlasesConfig.mapEntryValueMultiplier.get();
        for (var i : mapIds) MapAtlasItem.getMaps(atlas, level).add(i, level);

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
