package pepjebs.mapatlases.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.SupplementariesCompat;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.lang.ref.WeakReference;

public class AntiqueAtlasRecipe extends CustomRecipe {

    private WeakReference<Level> levelRef = new WeakReference<>(null);

    public AntiqueAtlasRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput inv, Level level) {
        if (!MapAtlasesMod.SUPPLEMENTARIES) return false;
        ItemStack atlas = ItemStack.EMPTY;
        ItemStack ink = ItemStack.EMPTY;
        // ensure 1 and one only atlas
        for (int j = 0; j < inv.size(); ++j) {
            ItemStack itemstack = inv.getItem(j);
            if (itemstack.is(MapAtlasesMod.MAP_ATLAS.get())) {
                if (!atlas.isEmpty()) return false;
                if (SupplementariesCompat.hasAntiqueInk(itemstack)) return false;
                atlas = itemstack;
            } else if (SupplementariesCompat.isAntiqueInk(itemstack)) {
                if (!ink.isEmpty()) return false;
                ink = itemstack;
            } else if (!itemstack.isEmpty()) return false;
        }
        if (!atlas.isEmpty() && !ink.isEmpty()) {
            levelRef = new WeakReference<>(level);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider registries) {

        Level level = levelRef.get();
        ItemStack newAtlas = ItemStack.EMPTY;
        ItemStack oldAtlas = ItemStack.EMPTY;
        // ensure 1 and one only atlas
        for (int j = 0; j < inv.size(); ++j) {
            ItemStack itemstack = inv.getItem(j);
            if (itemstack.is(MapAtlasesMod.MAP_ATLAS.get())) {
                newAtlas = itemstack.copyWithCount(1);
                oldAtlas = itemstack;
            }
        }

        // Get the Map Ids in the Grid
        // Set NBT Data
        MapCollection maps = MapAtlasItem.getMaps(newAtlas, level);
        MapCollection oldMaps = MapAtlasItem.getMaps(oldAtlas, level);
        var map = oldMaps.getIdsCopy();
        for (MapDataHolder holder : maps.getAllFound()) {
            oldMaps = oldMaps.removeAndAssigns(oldAtlas, level, holder.id, holder.type);
            MapId newId = SupplementariesCompat.createAntiqueMapData(holder.data, level, true, false);
            if (newId != null) {
                oldMaps = oldMaps.addAndAssigns(oldAtlas, level, holder.type, newId);
            }
        }
        SupplementariesCompat.setAntiqueInk(newAtlas);
        return newAtlas;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MapAtlasesMod.MAP_ANTIQUE_RECIPE.get();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

}
