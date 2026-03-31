package pepjebs.mapatlases.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.PlatStuff;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.ItemStackData;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MapAtlasCreateRecipe extends CustomRecipe {
    public static final MapCodec<MapAtlasCreateRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(MapAtlasCreateRecipe::category),
            Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(recipe -> recipe.ingredients)
    ).apply(instance, (category, ingredients) -> new MapAtlasCreateRecipe(category, copyIngredients(ingredients))));

    public static final StreamCodec<RegistryFriendlyByteBuf, MapAtlasCreateRecipe> STREAM_CODEC = StreamCodec.of(
            MapAtlasCreateRecipe::encode,
            MapAtlasCreateRecipe::decode
    );

    public static final RecipeSerializer<MapAtlasCreateRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    // some logic copied from shapeless recipes
    private final CraftingBookCategory category;
    private final NonNullList<Ingredient> ingredients;
    private final boolean isSimple;
    private final PlacementInfo placementInfo;

    // to prevent the world from not being unloaded
    private WeakReference<Level> levelReference = new WeakReference<>(null);

    public MapAtlasCreateRecipe(CraftingBookCategory category, NonNullList<Ingredient> ingredients) {
        super();
        this.category = category;
        this.ingredients = ingredients;
        this.isSimple = PlatStuff.isSimple(ingredients);
        this.placementInfo = PlacementInfo.create(List.of(
                Ingredient.of(Items.SLIME_BALL, Items.HONEY_BOTTLE),
                Ingredient.of(Items.BOOK)
        ));
    }

    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public boolean matches(CraftingInput inv, Level level) {
        StackedItemContents stackedcontents = new StackedItemContents();
        List<ItemStack> inputs = new ArrayList<>();
        int i = 0;
        boolean hasMap = false;
        for (int j = 0; j < inv.size(); ++j) {
            ItemStack itemstack = inv.getItem(j);
            if (MapAtlasesAccessUtils.isValidFilledMap(itemstack)) {
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
                        PlatStuff.findMatches(inputs, ingredients));

        if (matches) {
            levelReference = new WeakReference<>(level);
        }
        return matches;
    }

    @Override
    public ItemStack assemble(CraftingInput inv) {
        ItemStack mapItemStack = null;
        for (var item : inv.items()) {
            if (MapAtlasesAccessUtils.isValidFilledMap(item)) {
                mapItemStack = item;
                break;
            }
        }
        if (mapItemStack == null) {
            return ItemStack.EMPTY;
        }
        Integer mapId = MapAtlasesAccessUtils.getMapId(mapItemStack);
        if (mapId == null) {
            MapAtlasesMod.LOGGER.error("MapAtlasCreateRecipe found null Map ID from Filled Map");
            return ItemStack.EMPTY;
        }
        ItemStack atlas = new ItemStack(MapAtlasesMod.MAP_ATLAS.get());
        ItemStackData.update(atlas, tag -> tag.putIntArray(MapCollection.MAP_LIST_NBT, new int[]{mapId}));
        return atlas;
    }

    @Override
    public RecipeSerializer<MapAtlasCreateRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public PlacementInfo placementInfo() {
        return placementInfo;
    }

    @Override
    public CraftingBookCategory category() {
        return category;
    }

    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    private static NonNullList<Ingredient> copyIngredients(List<Ingredient> ingredients) {
        NonNullList<Ingredient> list = NonNullList.create();
        list.addAll(ingredients);
        return list;
    }

    private static MapAtlasCreateRecipe decode(RegistryFriendlyByteBuf buffer) {
        CraftingBookCategory craftingbookcategory = buffer.readEnum(CraftingBookCategory.class);

        int size = buffer.readVarInt();
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (int i = 0; i < size; i++) {
            ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
        }

        return new MapAtlasCreateRecipe(craftingbookcategory, ingredients);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, MapAtlasCreateRecipe recipe) {
        buffer.writeEnum(recipe.category());

        buffer.writeVarInt(recipe.ingredients.size());
        for (Ingredient ingredient : recipe.ingredients) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
        }
    }

}
