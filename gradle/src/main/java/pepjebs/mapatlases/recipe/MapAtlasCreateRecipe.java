package pepjebs.mapatlases.recipe;

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
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.item.MapAtlasItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapAtlasCreateRecipe extends SpecialCraftingRecipe {

    private World world = null;

    public MapAtlasCreateRecipe(Identifier id, CraftingRecipeCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(RecipeInputInventory inv, World world) {
        this.world = world;
        ArrayList<ItemStack> itemStacks = new ArrayList<>();
        ItemStack filledMap = ItemStack.EMPTY;
        for(int i = 0; i < inv.size(); i++) {
            if (!inv.getStack(i).isEmpty()) {
                itemStacks.add(inv.getStack(i));
                if (inv.getStack(i).getItem() == Items.FILLED_MAP) {
                    filledMap = inv.getStack(i);
                }
            }
        }
        if (itemStacks.size() == 3) {
            List<Item> items = itemStacks.stream().map(ItemStack::getItem).toList();
            boolean hasAllCrafting =
                    items.containsAll(Arrays.asList(Items.FILLED_MAP, Items.BOOK)) && itemStacks.stream()
                            .anyMatch(i -> i.isIn(TagKey.of(Registries.ITEM.getKey(), MapAtlasesMod.STICKY_ITEMS_ID)));
            if (hasAllCrafting && !filledMap.isEmpty()) {
                MapState state = FilledMapItem.getMapState(filledMap, world);
                return state != null;
            }
        }
        return false;
    }

    @Override
    public ItemStack craft(RecipeInputInventory inv, DynamicRegistryManager registryManager) {
        ItemStack mapItemStack = null;
        for(int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).isOf(Items.FILLED_MAP)) {
                mapItemStack = inv.getStack(i);
            }
        }
        if (mapItemStack == null || world == null || mapItemStack.getNbt() == null) {
            return ItemStack.EMPTY;
        }
        MapState mapState = FilledMapItem.getMapState(mapItemStack.getNbt().getInt("map"), world);
        if (mapState == null) return ItemStack.EMPTY;
        NbtCompound compoundTag = new NbtCompound();
        Integer mapId = FilledMapItem.getMapId(mapItemStack);
        if (mapId == null) {
            MapAtlasesMod.LOGGER.warn("MapAtlasCreateRecipe found null Map ID from Filled Map");
            compoundTag.putIntArray(MapAtlasItem.MAP_LIST_NBT, new int[]{});
        }
        else
            compoundTag.putIntArray(MapAtlasItem.MAP_LIST_NBT, new int[]{mapId});
        ItemStack atlasItemStack = new ItemStack(MapAtlasesMod.MAP_ATLAS);
        atlasItemStack.setNbt(compoundTag);
        return atlasItemStack;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MapAtlasesMod.MAP_ATLAS_CREATE_RECIPE;
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 3;
    }
}
