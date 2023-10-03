package pepjebs.mapatlases.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.RecipeMatcher;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.capabilities.MapCollectionCap;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.Slice;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MapAtlasCreateRecipe extends CustomRecipe {

    // some logic copied from shapeless recipes
    private final NonNullList<Ingredient> ingredients;
    private final boolean isSimple;

    // to prevent the world from not being unloaded
    private WeakReference<Level> levelReference = new WeakReference<>(null);

    public MapAtlasCreateRecipe(ResourceLocation id, CraftingBookCategory category, NonNullList<Ingredient> ingredients) {
        super(id, category);
        this.ingredients = ingredients;
        this.isSimple = ingredients.stream().allMatch(Ingredient::isSimple);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        StackedContents stackedcontents = new StackedContents();
        List<ItemStack> inputs = new ArrayList<>();
        int i = 0;
        boolean hasMap = false;
        for (int j = 0; j < inv.getContainerSize(); ++j) {
            ItemStack itemstack = inv.getItem(j);
            if ( MapAtlasesAccessUtils.isValidFilledMap(itemstack)) {
                if (hasMap || MapItem.getSavedData(itemstack, level) == null) {
                    return false;
                }
                hasMap = true;
            } else if (!itemstack.isEmpty()) {
                ++i;
                if (isSimple)
                    stackedcontents.accountStack(itemstack, 1);
                else inputs.add(itemstack);
            }
        }

        boolean matches = i == this.ingredients.size() && hasMap &&
                (isSimple ? stackedcontents.canCraft(this, null) :
                        RecipeMatcher.findMatches(inputs, this.ingredients) != null);

        if (matches) {
            levelReference = new WeakReference<>(level);
        }
        return matches;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess registryManager) {
        ItemStack mapItemStack = null;
        for (var item : inv.getItems()) {
            if( MapAtlasesAccessUtils.isValidFilledMap(item)){
                mapItemStack = item;
                break;
            }
        }
        Level level = levelReference.get();
        if (mapItemStack == null || level == null || mapItemStack.getTag() == null) {
            return ItemStack.EMPTY; //this should never happen
        }
        Integer mapId = MapItem.getMapId(mapItemStack);
        if (mapId == null) {
            MapAtlasesMod.LOGGER.error("MapAtlasCreateRecipe found null Map ID from Filled Map");
            return ItemStack.EMPTY;
        }
        MapDataHolder mapState = MapDataHolder.findFromId(level, mapId);
        if (mapState == null) return ItemStack.EMPTY;

        ItemStack atlas = new ItemStack(MapAtlasesMod.MAP_ATLAS.get());
        //initialize tag
        atlas.getOrCreateTag();
        MapCollectionCap maps = MapAtlasItem.getMaps(atlas, level);
        MapAtlasItem.setSelectedSlice(atlas, mapState.slice(), level.dimension());
        if (!maps.add(mapId, level)) {
            MapAtlasItem.increaseEmptyMaps(atlas, 1);
        }

        MapAtlasItem.increaseEmptyMaps(atlas, 0);
        return atlas;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MapAtlasesMod.MAP_ATLAS_CREATE_RECIPE.get();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    public static class Serializer implements RecipeSerializer<MapAtlasCreateRecipe> {

        @Override
        public MapAtlasCreateRecipe fromNetwork(ResourceLocation pRecipeId, FriendlyByteBuf buffer) {
            CraftingBookCategory craftingbookcategory = buffer.readEnum(CraftingBookCategory.class);

            NonNullList<Ingredient> ingredients = NonNullList.withSize(buffer.readVarInt(), Ingredient.EMPTY);
            ingredients.replaceAll(ignored -> Ingredient.fromNetwork(buffer));

            return new MapAtlasCreateRecipe(pRecipeId, craftingbookcategory, ingredients);
        }

        @Override
        public void toNetwork(FriendlyByteBuf pBuffer, MapAtlasCreateRecipe pRecipe) {
            pBuffer.writeEnum(pRecipe.category());

            pBuffer.writeVarInt(pRecipe.ingredients.size());
            for (Ingredient ingredient : pRecipe.ingredients) {
                ingredient.toNetwork(pBuffer);
            }
        }

        @Override
        public MapAtlasCreateRecipe fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {
            CraftingBookCategory craftingbookcategory = CraftingBookCategory.CODEC.byName(GsonHelper.getAsString(pSerializedRecipe, "category", null), CraftingBookCategory.MISC);
            NonNullList<Ingredient> nonnulllist = itemsFromJson(GsonHelper.getAsJsonArray(pSerializedRecipe, "ingredients"));

            return new MapAtlasCreateRecipe(pRecipeId, craftingbookcategory, nonnulllist);
        }

        private static NonNullList<Ingredient> itemsFromJson(JsonArray pIngredientArray) {
            NonNullList<Ingredient> nonnulllist = NonNullList.create();
            for (int i = 0; i < pIngredientArray.size(); ++i) {
                nonnulllist.add(Ingredient.fromJson(pIngredientArray.get(i), false));
            }
            return nonnulllist;
        }

    }
}
