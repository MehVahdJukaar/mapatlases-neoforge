package pebjebs.mapatlases.recipe;

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
import pebjebs.mapatlases.MapAtlasesMod;
import pebjebs.mapatlases.item.MapAtlasItem;

import java.lang.ref.WeakReference;

public class MapAtlasCreateRecipe extends CustomRecipe {

    // to prevent the world from not being unloaded
    private WeakReference<Level> levelReference = new WeakReference<>(null);

    public MapAtlasCreateRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        this.levelReference = new WeakReference<>(level);
        boolean filledMap = false;
        boolean book = false;
        boolean sticky = false;
        for (ItemStack item : inv.getItems()) {
            if (!item.isEmpty()) {
                Item i = item.getItem();
                if (i == Items.FILLED_MAP) {
                    if (filledMap) return false;
                    if(MapItem.getSavedData(item, level)!= null){
                        filledMap = true;
                    }else return false;
                }
                else if(i == Items.BOOK){
                    if (book) return false;
                    book = true;
                }
                else if(item.is(MapAtlasesMod.STICKY_ITEMS)){
                    if (sticky) return false;
                    sticky = true;
                } else return false;
            }
        }
        return sticky && book && filledMap;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess registryManager) {
        ItemStack mapItemStack = null;
        for (var item : inv.getItems()) {
            if (item.is(Items.FILLED_MAP)) {
                mapItemStack = item;
            }
        }
        Level level = levelReference.get();
        if (mapItemStack == null || level == null || mapItemStack.getTag() == null) {
            return ItemStack.EMPTY;
        }
        MapItemSavedData mapState = MapItem.getSavedData(mapItemStack.getTag().getInt("map"), level);
        if (mapState == null) return ItemStack.EMPTY;
        CompoundTag compoundTag = new CompoundTag();
        Integer mapId = MapItem.getMapId(mapItemStack);
        if (mapId == null) {
            MapAtlasesMod.LOGGER.warn("MapAtlasCreateRecipe found null Map ID from Filled Map");
            compoundTag.putIntArray(MapAtlasItem.MAP_LIST_NBT, new int[]{});
        } else
            compoundTag.putIntArray(MapAtlasItem.MAP_LIST_NBT, new int[]{mapId});
        ItemStack atlasItemStack = new ItemStack(MapAtlasesMod.MAP_ATLAS.get());
        atlasItemStack.setTag(compoundTag);
        return atlasItemStack;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MapAtlasesMod.MAP_ATLAS_CREATE_RECIPE.get();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }
}
