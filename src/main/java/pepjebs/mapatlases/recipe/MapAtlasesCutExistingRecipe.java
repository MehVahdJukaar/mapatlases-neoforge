package pepjebs.mapatlases.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.capabilities.MapCollectionCap;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.lang.ref.WeakReference;

public class MapAtlasesCutExistingRecipe extends CustomRecipe {

    private WeakReference<Level> levelRef = new WeakReference<>(null);

    public MapAtlasesCutExistingRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        ItemStack atlas = ItemStack.EMPTY;
        ItemStack shears = ItemStack.EMPTY;
        for (ItemStack i : inv.getItems()) {
            if (!i.isEmpty()) {
                if (i.is(MapAtlasesMod.MAP_ATLAS.get()) &&
                        (MapAtlasItem.getEmptyMaps(i) > 0 || MapAtlasItem.getMaps(i, level).getCount() > 0)) {
                    if (!atlas.isEmpty()) return false;
                    atlas = i;
                } else if (i.is(Items.SHEARS) && i.getDamageValue() < i.getMaxDamage() - 1) {
                    if (!shears.isEmpty()) return false;
                    shears = i;
                } else return false;
            }
        }
        boolean b = !shears.isEmpty() && !atlas.isEmpty();
        if(b){
            levelRef = new WeakReference<>(level);
        }
        return b;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess registryManager) {
        ItemStack atlas = ItemStack.EMPTY;
        for (ItemStack i : inv.getItems()) {
            if (i.is(MapAtlasesMod.MAP_ATLAS.get())) {
                atlas = i;
                break;
            }
        }
        MapCollectionCap maps = MapAtlasItem.getMaps(atlas, levelRef.get());
        if (maps.getCount() > 1) {
            String stringId = maps.getActive().getFirst();
            int mapId = MapAtlasesAccessUtils.getMapIntFromString(stringId);
            return MapAtlasesAccessUtils.createMapItemStackFromId(mapId);
        }
        if (MapAtlasItem.getEmptyMaps(atlas) > 0) {
            return new ItemStack(Items.MAP);
        }
        //should never run
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        NonNullList<ItemStack> list = NonNullList.create();
        for (ItemStack i : container.getItems()) {
            ItemStack stack = i.copy();

            if (stack.getItem() == Items.SHEARS) {
                stack.hurt(1, RandomSource.create(), null);
            } else if (stack.is(MapAtlasesMod.MAP_ATLAS.get())) {
                boolean didRemoveFilled = false;
                MapCollectionCap maps = MapAtlasItem.getMaps(stack, levelRef.get());
                if (!maps.isEmpty()) {
                    maps.remove(maps.getActive().getFirst());
                    didRemoveFilled = true;
                }
                int emptyMaps = MapAtlasItem.getEmptyMaps(stack);
                if (emptyMaps > 0 && !didRemoveFilled) {
                    int multiplier = MapAtlasesConfig.mapEntryValueMultiplier.get();
                    int amountToSet = Math.max(emptyMaps - multiplier, 0);
                    MapAtlasItem.setEmptyMaps(stack, amountToSet);
                }
            }
            list.add(stack);
        }
        return list;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width + height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MapAtlasesMod.MAP_ATLAS_CUT_RECIPE.get();
    }
}
