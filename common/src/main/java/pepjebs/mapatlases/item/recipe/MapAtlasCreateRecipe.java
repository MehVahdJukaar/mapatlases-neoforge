package pepjebs.mapatlases.item.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.utils.AtlasMap;
import pepjebs.mapatlases.utils.MapType;

import java.util.ArrayList;
import java.util.List;

public class MapAtlasCreateRecipe extends AbstractAtlasRecipe {

    public static final MapCodec<MapAtlasCreateRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(r -> r.bookInfo),
            Ingredient.CODEC.listOf(1, 8).fieldOf("ingredients").forGetter(r -> r.ingredients)
    ).apply(instance, MapAtlasCreateRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MapAtlasCreateRecipe> STREAM_CODEC = StreamCodec.composite(
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, r -> r.bookInfo,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), r -> r.ingredients,
            MapAtlasCreateRecipe::new);

    // some logic copied from shapeless recipes
    private final CraftingRecipe.CraftingBookInfo bookInfo;
    private final List<Ingredient> ingredients;

    public MapAtlasCreateRecipe(CraftingRecipe.CraftingBookInfo bookInfo, List<Ingredient> ingredients) {
        this.bookInfo = bookInfo;
        this.ingredients = ingredients;
    }

    @Override
    public String group() {
        return bookInfo.group();
    }

    @Override
    public CraftingBookCategory category() {
        return bookInfo.category();
    }

    //jei skips special ones
    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public boolean matches(CraftingInput inv, Level level) {
        StackedItemContents stackedContents = new StackedItemContents();
        int i = 0;
        boolean hasMap = false;
        for (int j = 0; j < inv.size(); ++j) {
            ItemStack itemstack = inv.getItem(j);
            if (MapType.isFilledMap(itemstack)) {
                if (hasMap) {
                    return false;
                }
                hasMap = true;
            } else if (!itemstack.isEmpty()) {
                ++i;
                stackedContents.accountStack(itemstack, 1);
            }
        }
        boolean matches = i == this.ingredients.size() && hasMap && stackedContents.canCraft(this.ingredients, null);

        if (matches) {
            rememberLevel(level);
        }
        return matches;
    }

    @Override
    public ItemStack assemble(CraftingInput inv) {
        ItemStack mapItemStack = null;
        for (var item : inv.items()) {
            if (MapType.isFilledMap(item)) {
                mapItemStack = item;
                break;
            }
        }
        Level level = getLevel();
        if (mapItemStack == null || level == null) {
            return ItemStack.EMPTY; //this should never happen
        }
        AtlasMap mapHolder = AtlasMap.fromFilledMapItem(level, mapItemStack);
        if (mapHolder == null) {
            MapAtlasesMod.LOGGER.error("MapAtlasCreateRecipe found null Map ID from Filled Map");
            return ItemStack.EMPTY;
        }

        ItemStack atlas = new ItemStack(MapAtlasesMod.MAP_ATLAS.get());
        //initialize tag
        MapCollection maps = MapAtlasItem.getMaps(atlas, level);
        MapAtlasItem.setSelectedSlice(atlas, mapHolder.slice, level);
        maps.addAndAssign(atlas, level, mapHolder.type, mapHolder.id);

        return atlas;
    }

    @Override
    public List<RecipeDisplay> display() {
        List<SlotDisplay> slots = new ArrayList<>();
        for (Ingredient ingredient : ingredients) slots.add(ingredient.display());
        slots.add(new SlotDisplay.ItemSlotDisplay(Items.FILLED_MAP));
        return List.of(new ShapelessCraftingRecipeDisplay(slots,
                new SlotDisplay.ItemSlotDisplay(MapAtlasesMod.MAP_ATLAS.get()),
                new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
    }

    @Override
    public RecipeSerializer<MapAtlasCreateRecipe> getSerializer() {
        return MapAtlasesMod.MAP_ATLAS_CREATE_RECIPE.get();
    }

}
