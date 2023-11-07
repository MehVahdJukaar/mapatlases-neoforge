package pepjebs.mapatlases.integration.supplementaries;

import net.mehvahdjukaar.supplementaries.common.misc.AntiqueInkHelper;
import net.mehvahdjukaar.supplementaries.common.misc.map_markers.WeatheredMap;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.capabilities.MapCollectionCap;
import pepjebs.mapatlases.item.MapAtlasItem;

import java.lang.ref.WeakReference;

public class AntiqueAtlasRecipe extends CustomRecipe {

    private WeakReference<Level> levelRef = new WeakReference<>(null);

    public AntiqueAtlasRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        ItemStack atlas = ItemStack.EMPTY;
        ItemStack ink = ItemStack.EMPTY;
        // ensure 1 and one only atlas
        for (int j = 0; j < inv.getContainerSize(); ++j) {
            ItemStack itemstack = inv.getItem(j);
            if (itemstack.is(MapAtlasesMod.MAP_ATLAS.get())) {
                if (!atlas.isEmpty()) return false;
                if (AntiqueInkHelper.hasAntiqueInk(itemstack)) return false;
                atlas = itemstack;
            } else if (itemstack.is(ModRegistry.ANTIQUE_INK.get())) {
                if (!ink.isEmpty()) return false;
                ink = itemstack;
            } else if (!itemstack.isEmpty()) return false;
        }
        if (!atlas.isEmpty() && ink.isEmpty()) {
            levelRef = new WeakReference<>(level);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess registryManager) {

        Level level = levelRef.get();
        ItemStack newAtlas = ItemStack.EMPTY;
        ItemStack oldAtlas = ItemStack.EMPTY;
        // ensure 1 and one only atlas
        for (int j = 0; j < inv.getContainerSize(); ++j) {
            ItemStack itemstack = inv.getItem(j);
            if (itemstack.is(MapAtlasesMod.MAP_ATLAS.get())) {
                newAtlas = itemstack.copyWithCount(1);
                oldAtlas = itemstack;
            }
        }

        // Get the Map Ids in the Grid
        // Set NBT Data
        MapCollectionCap maps = MapAtlasItem.getMaps(newAtlas, level);
        MapCollectionCap oldMaps = MapAtlasItem.getMaps(oldAtlas, level);
        for (var i : maps.getAll()) {
            int newId =0;// WeatheredMap.createAntique();
            oldMaps.remove(i);
            oldMaps.add(newId, level);
        }
        AntiqueInkHelper.setAntiqueInk(newAtlas, true);
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
