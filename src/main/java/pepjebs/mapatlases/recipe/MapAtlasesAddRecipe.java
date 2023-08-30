package pepjebs.mapatlases.recipe;

import com.google.common.primitives.Ints;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.*;
import java.util.stream.Collectors;

public class MapAtlasesAddRecipe extends CustomRecipe {

    private Level world = null;

    public MapAtlasesAddRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level world) {
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
        MapItemSavedData sampleMap = MapAtlasesAccessUtils.getFirstMapItemSavedDataFromAtlas(world, atlas);

        // Ensure only correct ingredients are present
        List<Item> additems = new ArrayList<>(Arrays.asList(Items.FILLED_MAP, MapAtlasesMod.MAP_ATLAS.get()));
        if (MapAtlasesConfig.enableEmptyMapEntryAndFill.get())
            additems.add(Items.MAP);
        if (MapAtlasesConfig.acceptPaperForEmptyMaps.get())
            additems.add(Items.PAPER);
        if (!(itemStacks.size() > 1 && isListOnlyIngredients(
                itemStacks,
                additems))) {
            return false;
        }
        List<MapItemSavedData> mapStates = getMapItemSavedDatasFromItemStacks(world, itemStacks);

        // Ensure we're not trying to add too many Maps
        int mapCount = MapAtlasesAccessUtils.getMapCountFromItemStack(atlas)
                + MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(atlas);
        if (MapAtlasItem.getMaxMapCount() != -1 && mapCount + itemStacks.size() - 1 > MapAtlasItem.getMaxMapCount()) {
            return false;
        }

        // Ensure Filled Maps are all same Scale & Dimension
        if (mapStates.size() > 0 && sampleMap != null && !areMapsSameScale(sampleMap, mapStates)) return false;

        // Ensure there's only one Atlas
        long atlasCount = itemStacks.stream().filter(i ->
                i.is(MapAtlasesMod.MAP_ATLAS.get())).count();
        return atlasCount == 1;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess registryManager) {
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
        int emptyMapCount = (int) itemStacks.stream()
                .filter(i -> i != null && (i.is(Items.MAP) || i.is(Items.PAPER))).count();
        emptyMapCount *= MapAtlasesConfig.mapEntryValueMultiplier.get();
        CompoundTag compoundTag = atlas.getOrCreateTag();
        Set<Integer> existingMaps = new HashSet<>(Ints.asList(compoundTag.getIntArray(MapAtlasItem.MAP_LIST_NBT)));
        existingMaps.addAll(mapIds);
        compoundTag.putIntArray(
                MapAtlasItem.MAP_LIST_NBT, existingMaps.stream().filter(Objects::nonNull).mapToInt(i -> i).toArray());
        compoundTag.putInt(MapAtlasItem.EMPTY_MAP_NBT, emptyMapCount + compoundTag.getInt(MapAtlasItem.EMPTY_MAP_NBT));
        atlas.setTag(compoundTag);
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

    private boolean areMapsSameScale(MapItemSavedData testAgainst, List<MapItemSavedData> newMaps) {
        return newMaps.stream().filter(m -> m.scale == testAgainst.scale).count() == newMaps.size();
    }

    private boolean areMapsSameDimension(MapItemSavedData testAgainst, List<MapItemSavedData> newMaps) {
        return newMaps.stream().filter(m -> m.dimension == testAgainst.dimension).count() == newMaps.size();
    }

    private ItemStack getAtlasFromItemStacks(List<ItemStack> itemStacks) {
        Optional<ItemStack> item = itemStacks.stream()
                .filter(i -> i.is(MapAtlasesMod.MAP_ATLAS.get())).findFirst();
        return item.orElse(ItemStack.EMPTY).copy();
    }

    private List<MapItemSavedData> getMapItemSavedDatasFromItemStacks(Level world, List<ItemStack> itemStacks) {
        return itemStacks.stream()
                .filter(i -> i.is(Items.FILLED_MAP))
                .map(m -> MapItem.getSavedData(m, world))
                .collect(Collectors.toList());
    }

    private Set<Integer> getMapIdsFromItemStacks(List<ItemStack> itemStacks) {
        return itemStacks.stream().map(MapItem::getMapId).collect(Collectors.toSet());
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
