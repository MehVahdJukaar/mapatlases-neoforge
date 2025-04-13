package pepjebs.mapatlases.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.PlatStuff;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MapAtlasCreateRecipe extends CustomRecipe {

    // some logic copied from shapeless recipes
    private final NonNullList<Ingredient> ingredients;
    private final boolean isSimple;
    private final String group;

    // to prevent the world from not being unloaded
    private WeakReference<Level> levelReference = new WeakReference<>(null);

    public MapAtlasCreateRecipe(String group, CraftingBookCategory category, NonNullList<Ingredient> ingredients) {
        super(category);
        this.group = group;
        this.ingredients = ingredients;
        this.isSimple = PlatStuff.isSimple(ingredients);
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public boolean matches(CraftingInput inv, Level level) {
        StackedContents stackedcontents = new StackedContents();
        List<ItemStack> inputs = new ArrayList<>();
        int i = 0;
        boolean hasMap = false;
        for (int j = 0; j < inv.size(); ++j) {
            ItemStack itemstack = inv.getItem(j);
            if (MapAtlasesAccessUtils.isValidFilledMap(itemstack)) {
                if (hasMap) {
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
                        PlatStuff.findMatches(inputs, ingredients));

        if (matches) {
            levelReference = new WeakReference<>(level);
        }
        return matches;
    }

    @Override
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider registries) {
        ItemStack mapItemStack = null;
        for (var item : inv.items()) {
            if (MapAtlasesAccessUtils.isValidFilledMap(item)) {
                mapItemStack = item;
                break;
            }
        }
        Level level = levelReference.get();
        if (mapItemStack == null || level == null) {
            return ItemStack.EMPTY; //this should never happen
        }
        MapDataHolder mapHolder = MapAtlasesAccessUtils.findMapFromItemStack(level, mapItemStack);
        if (mapHolder == null) {
            MapAtlasesMod.LOGGER.error("MapAtlasCreateRecipe found null Map ID from Filled Map");
            return ItemStack.EMPTY;
        }

        ItemStack atlas = new ItemStack(MapAtlasesMod.MAP_ATLAS.get());
        //initialize tag
        MapCollection maps = MapAtlasItem.getMaps(atlas, level);
        MapAtlasItem.setSelectedSlice(atlas, mapHolder.slice, level);
        maps.addAndAssigns(atlas, level, mapHolder.type, mapHolder.id);

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

        private static final MapCodec<MapAtlasCreateRecipe> CODEC = RecordCodecBuilder.mapCodec((instance) -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter((shapelessRecipe) -> shapelessRecipe.group),
                CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(CustomRecipe::category),
                Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").flatXmap((list) -> {
                    Ingredient[] ingredients = list.stream().filter((ingredient) -> !ingredient.isEmpty()).toArray(Ingredient[]::new);
                    if (ingredients.length == 0) {
                        return DataResult.error(() -> "No ingredients for shapeless recipe");
                    } else {
                        return ingredients.length > 9 ? DataResult.error(() -> "Too many ingredients for shapeless recipe") : DataResult.success(NonNullList.of(Ingredient.EMPTY, ingredients));
                    }
                }, DataResult::success).forGetter((shapelessRecipe) -> shapelessRecipe.ingredients)
        ).apply(instance, MapAtlasCreateRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, MapAtlasCreateRecipe> STREAM_CODEC = StreamCodec.of(
                MapAtlasCreateRecipe.Serializer::toNetwork, MapAtlasCreateRecipe.Serializer::fromNetwork);


        private static MapAtlasCreateRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            String string = buffer.readUtf();
            CraftingBookCategory craftingBookCategory = buffer.readEnum(CraftingBookCategory.class);
            int i = buffer.readVarInt();
            NonNullList<Ingredient> nonNullList = NonNullList.withSize(i, Ingredient.EMPTY);
            nonNullList.replaceAll((ingredient) -> Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
            return new MapAtlasCreateRecipe(string, craftingBookCategory, nonNullList);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, MapAtlasCreateRecipe recipe) {
            buffer.writeUtf(recipe.group);
            buffer.writeEnum(recipe.category());
            buffer.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
            }
        }

        @Override
        public MapCodec<MapAtlasCreateRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MapAtlasCreateRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }


}
