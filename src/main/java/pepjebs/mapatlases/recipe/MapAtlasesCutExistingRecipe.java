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
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MapAtlasesCutExistingRecipe extends CustomRecipe {

    public MapAtlasesCutExistingRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level world) {
        ItemStack atlas = ItemStack.EMPTY;
        ItemStack shears = ItemStack.EMPTY;
        for (ItemStack i : inv.getItems()) {
            if (!i.isEmpty()) {
                if (i.is(MapAtlasesMod.MAP_ATLAS.get()) &&
                        (MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(i) > 0 ||
                                MapAtlasesAccessUtils.getMapIdsFromItemStack(atlas).length > 0)) {
                    if (!atlas.isEmpty()) return false;
                    atlas = i;
                } else if (i.is(Items.SHEARS) && i.getDamageValue() < i.getMaxDamage() - 1) {
                    if (!shears.isEmpty()) return false;
                    shears = i;
                } else return false;
            }
        }
        return !shears.isEmpty() && !atlas.isEmpty();
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
        int[] mapIds = MapAtlasesAccessUtils.getMapIdsFromItemStack(atlas);
        if (mapIds.length > 1) {
            int lastId = mapIds[mapIds.length - 1];
            return MapAtlasesAccessUtils.createMapItemStackFromId(lastId);
        }
        if (MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(atlas) > 0) {
            return new ItemStack(Items.MAP);
        }
        //should never run
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        NonNullList<ItemStack> list = NonNullList.create();
        for (ItemStack i : container.getItems()) {
            ItemStack cur = i.copy();
            //TODO: improve
            if (cur.getItem() == Items.SHEARS) {
                cur.hurt(1, RandomSource.create(), null);
            } else if (cur.is(MapAtlasesMod.MAP_ATLAS.get()) && cur.getTag() != null) {
                boolean didRemoveFilled = false;
                if (MapAtlasesAccessUtils.getMapCountFromItemStack(cur) > 1) {
                    List<Integer> mapIds = Arrays.stream(cur.getTag()
                            .getIntArray(MapAtlasItem.MAP_LIST_NBT)).boxed().collect(Collectors.toList());
                    if (!mapIds.isEmpty()) {
                        mapIds.remove(mapIds.size() - 1);
                        cur.getTag().putIntArray(MapAtlasItem.MAP_LIST_NBT, mapIds);
                        didRemoveFilled = true;
                    }

                }
                if (MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(cur) > 0 && !didRemoveFilled) {
                    int multiplier = MapAtlasesConfig.mapEntryValueMultiplier.get();
                    int amountToSet = Math.max(cur.getTag().getInt(MapAtlasItem.EMPTY_MAP_NBT) - multiplier, 0);
                    cur.getTag().putInt(MapAtlasItem.EMPTY_MAP_NBT, amountToSet);
                }
            }
            list.add(cur);
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
