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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.common.util.RecipeMatcher;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.item.MapAtlasItem;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MapAtlasCreateRecipe extends CustomRecipe {

    // some logic copied from shapeless recipes
    private final NonNullList<Ingredient> ingredients;
    private final boolean isSimple;

    // to prevent the world from not being unloaded
    private WeakReference<Level> levelReference = new WeakReference<>(null);

    public MapAtlasCreateRecipe(ResourceLocation id,   NonNullList<Ingredient> ingredients) {
        super(id);
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
        for(int j = 0; j < inv.getContainerSize(); ++j) {
            ItemStack itemstack = inv.getItem(j);
            if(itemstack.is(Items.FILLED_MAP)){
                if(hasMap || MapItem.getSavedData(itemstack, level) == null){
                    return false;
                }hasMap = true;
            }
            else if (!itemstack.isEmpty()) {
                ++i;
                if (isSimple)
                    stackedcontents.accountStack(itemstack, 1);
                else inputs.add(itemstack);
            }
        }

        boolean matches = i == this.ingredients.size() && hasMap &&
                (isSimple ? stackedcontents.canCraft(this, null) :
                        RecipeMatcher.findMatches(inputs,  this.ingredients) != null);

        if(matches){
            levelReference = new WeakReference<>(level);
        }
        return matches;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv) {
        ItemStack mapItemStack = null;
        for (int j = 0; j < inv.getContainerSize(); ++j) {
            ItemStack item = inv.getItem(j);
            if (item.is(Items.FILLED_MAP)) {
                mapItemStack = item;
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
        MapItemSavedData mapState = MapItem.getSavedData(mapId, level);
        if (mapState == null) return ItemStack.EMPTY;

        ItemStack atlas = new ItemStack(MapAtlasesMod.MAP_ATLAS.get());

        if(!MapAtlasItem.getMaps(atlas, level).add(mapId, level)){
            MapAtlasItem.increaseEmptyMaps(atlas,1);
        }
        //initialize tag
        atlas.getOrCreateTag();
        MapAtlasItem.increaseEmptyMaps(atlas,0);
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

            NonNullList<Ingredient> ingredients = NonNullList.withSize(buffer.readVarInt(), Ingredient.EMPTY);
            ingredients.replaceAll(ignored -> Ingredient.fromNetwork(buffer));

            return new MapAtlasCreateRecipe(pRecipeId, ingredients);
        }

        @Override
        public void toNetwork(FriendlyByteBuf pBuffer, MapAtlasCreateRecipe pRecipe) {

            pBuffer.writeVarInt(pRecipe.ingredients.size());
            for(Ingredient ingredient : pRecipe.ingredients) {
                ingredient.toNetwork(pBuffer);
            }
        }

        @Override
        public MapAtlasCreateRecipe fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {
           NonNullList<Ingredient> nonnulllist = itemsFromJson(GsonHelper.getAsJsonArray(pSerializedRecipe, "ingredients"));

            return new MapAtlasCreateRecipe(pRecipeId, nonnulllist);
        }

        private static NonNullList<Ingredient> itemsFromJson(JsonArray pIngredientArray) {
            NonNullList<Ingredient> nonnulllist = NonNullList.create();
            for(int i = 0; i < pIngredientArray.size(); ++i) {
                nonnulllist.add(Ingredient.fromJson(pIngredientArray.get(i)));
            }
            return nonnulllist;
        }

    }
}
