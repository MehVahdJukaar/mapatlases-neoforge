package pepjebs.mapatlases.recipe;

import com.google.common.primitives.Ints;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.*;
import java.util.stream.Collectors;

public class MapAtlasesAddRecipe extends SpecialCraftingRecipe {

    private World world = null;

    public MapAtlasesAddRecipe(Identifier id, CraftingRecipeCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(RecipeInputInventory inv, World world) {
        this.world = world;
        List<ItemStack> itemStacks = MapAtlasesAccessUtils
                .getItemStacksFromGrid(inv)
                .stream()
                .map(ItemStack::copy)
                .toList();
        ItemStack atlas = getAtlasFromItemStacks(itemStacks).copy();

        // Ensure there's an Atlas
        if (atlas.isEmpty()) {
            return false;
        }
        MapState sampleMap = MapAtlasesAccessUtils.getFirstMapStateFromAtlas(world, atlas);

        // Ensure only correct ingredients are present
        List<Item> additems = new ArrayList<>(Arrays.asList(Items.FILLED_MAP, MapAtlasesMod.MAP_ATLAS));
        if (MapAtlasesMod.CONFIG == null || MapAtlasesMod.CONFIG.enableEmptyMapEntryAndFill)
            additems.add(Items.MAP);
        if (MapAtlasesMod.CONFIG != null && MapAtlasesMod.CONFIG.acceptPaperForEmptyMaps)
            additems.add(Items.PAPER);
        if (!(itemStacks.size() > 1 && isListOnlyIngredients(
                itemStacks,
                additems))) {
            return false;
        }
        List<MapState> mapStates = getMapStatesFromItemStacks(world, itemStacks);

        // Ensure we're not trying to add too many Maps
        int mapCount = MapAtlasesAccessUtils.getMapCountFromItemStack(atlas)
                + MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(atlas);
        if (MapAtlasItem.getMaxMapCount() != -1 && mapCount + itemStacks.size() - 1 > MapAtlasItem.getMaxMapCount()) {
            return false;
        }

        // Ensure Filled Maps are all same Scale & Dimension
        if(mapStates.size() > 0 && sampleMap != null && !areMapsSameScale(sampleMap, mapStates)) return false;

        // Ensure there's only one Atlas
        long atlasCount = itemStacks.stream().filter(i ->
                i.isOf(MapAtlasesMod.MAP_ATLAS)).count();
        return atlasCount == 1;
    }

    @Override
    public ItemStack craft(RecipeInputInventory inv, DynamicRegistryManager registryManager) {
        if (world == null) return ItemStack.EMPTY;
        List<ItemStack> itemStacks = MapAtlasesAccessUtils.getItemStacksFromGrid(inv)
                .stream()
                .map(ItemStack::copy)
                .toList();
        // Grab the Atlas in the Grid
        ItemStack atlas = getAtlasFromItemStacks(itemStacks).copy();
        // Get the Map Ids in the Grid
        Set<Integer> mapIds = getMapIdsFromItemStacks(itemStacks);
        // Set NBT Data
        int emptyMapCount = (int)itemStacks.stream()
                .filter(i -> i != null && (i.isOf(Items.MAP) || i.isOf(Items.PAPER))).count();
        if (MapAtlasesMod.CONFIG != null) {
            emptyMapCount *= MapAtlasesMod.CONFIG.mapEntryValueMultiplier;
        }
        NbtCompound compoundTag = atlas.getOrCreateNbt();
        Set<Integer> existingMaps = new HashSet<>(Ints.asList(compoundTag.getIntArray(MapAtlasItem.MAP_LIST_NBT)));
        existingMaps.addAll(mapIds);
        compoundTag.putIntArray(
                MapAtlasItem.MAP_LIST_NBT, existingMaps.stream().filter(Objects::nonNull).mapToInt(i->i).toArray());
        compoundTag.putInt(MapAtlasItem.EMPTY_MAP_NBT, emptyMapCount + compoundTag.getInt(MapAtlasItem.EMPTY_MAP_NBT));
        atlas.setNbt(compoundTag);
        return atlas;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MapAtlasesMod.MAP_ATLAS_ADD_RECIPE;
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 2;
    }

    private boolean areMapsSameScale(MapState testAgainst, List<MapState> newMaps) {
        return newMaps.stream().filter(m -> m.scale == testAgainst.scale).count() == newMaps.size();
    }

    private boolean areMapsSameDimension(MapState testAgainst, List<MapState> newMaps) {
        return newMaps.stream().filter(m -> m.dimension == testAgainst.dimension).count() == newMaps.size();
    }

    private ItemStack getAtlasFromItemStacks(List<ItemStack> itemStacks) {
        Optional<ItemStack> item =  itemStacks.stream()
                .filter(i -> i.isOf(MapAtlasesMod.MAP_ATLAS)).findFirst();
        return item.orElse(ItemStack.EMPTY).copy();
    }

    private List<MapState> getMapStatesFromItemStacks(World world, List<ItemStack> itemStacks) {
        return itemStacks.stream()
                .filter(i -> i.isOf(Items.FILLED_MAP))
                .map(m -> FilledMapItem.getMapState(m, world))
                .collect(Collectors.toList());
    }

    private Set<Integer> getMapIdsFromItemStacks(List<ItemStack> itemStacks) {
        return itemStacks.stream().map(FilledMapItem::getMapId).collect(Collectors.toSet());
    }

    private boolean isListOnlyIngredients(List<ItemStack> itemStacks, List<Item> items) {
        return itemStacks.stream().filter(is -> {
            for (Item i : items) {
                if (i == is.getItem()) return true;
            }
            return false;
        }).count() == itemStacks.size();
    }
}
