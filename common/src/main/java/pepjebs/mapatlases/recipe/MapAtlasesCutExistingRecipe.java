package pepjebs.mapatlases.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.IMapCollection;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.Slice;

import java.lang.ref.WeakReference;
import java.util.Optional;

public class MapAtlasesCutExistingRecipe extends CustomRecipe {
    public static final MapCodec<MapAtlasesCutExistingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(MapAtlasesCutExistingRecipe::category)
    ).apply(instance, MapAtlasesCutExistingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MapAtlasesCutExistingRecipe> STREAM_CODEC = StreamCodec.of(
            (buffer, recipe) -> buffer.writeEnum(recipe.category()),
            buffer -> new MapAtlasesCutExistingRecipe(buffer.readEnum(CraftingBookCategory.class))
    );

    public static final RecipeSerializer<MapAtlasesCutExistingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final CraftingBookCategory category;
    private WeakReference<Level> levelRef = new WeakReference<>(null);

    public MapAtlasesCutExistingRecipe(CraftingBookCategory category) {
        super();
        this.category = category;
    }

    @Override
    public boolean matches(CraftingInput inv, Level level) {
        ItemStack atlas = ItemStack.EMPTY;
        ItemStack shears = ItemStack.EMPTY;
        for (ItemStack i : inv.items()) {
            if (!i.isEmpty()) {
                if (i.is(MapAtlasesMod.MAP_ATLAS.get()) &&
                        (MapAtlasItem.getEmptyMaps(i) > 0 || MapAtlasItem.getMaps(i, level).getCount() > 0)) {
                    if (!atlas.isEmpty()) return false;
                    atlas = i;
                } else if (i.is(Items.SHEARS) && i.getDamageValue() < i.getMaxDamage() - 1) {
                    if (!shears.isEmpty()) return false;
                    shears = i;
                } else return false;
            }
        }
        boolean b = !shears.isEmpty() && !atlas.isEmpty();
        if (b) {
            levelRef = new WeakReference<>(level);
        }
        return b;
    }

    @Override
    public ItemStack assemble(CraftingInput inv) {
        ItemStack atlas = ItemStack.EMPTY;
        for (ItemStack i : inv.items()) {
            if (i.is(MapAtlasesMod.MAP_ATLAS.get())) {
                atlas = i;
                break;
            }
        }
        IMapCollection maps = MapAtlasItem.getMaps(atlas, levelRef.get());
        //not using count. we want actual maps
        if (maps.getAll().size() > 1) {
            var slice = MapAtlasItem.getSelectedSlice(atlas, levelRef.get().dimension());
            //TODO: very ugly and wont work in many cases
            MapDataHolder toRemove = getMapToRemove(inv, maps, slice);
            return toRemove.createExistingMapItem();
        }
        if (MapAtlasItem.getEmptyMaps(atlas) > 0) {
            return new ItemStack(Items.MAP);
        }
        //should never run
        return ItemStack.EMPTY;
    }

    private static MapDataHolder getMapToRemove(CraftingInput inv, IMapCollection maps, Slice slice) {
        return maps.getAll().stream().findAny().get();
    }


    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput inv) {
        NonNullList<ItemStack> list = NonNullList.create();
        for (ItemStack i : inv.items()) {
            ItemStack stack = i.copy();

            if (stack.getItem() == Items.SHEARS) {
                stack.setDamageValue(stack.getDamageValue() + 1);
            } else if (stack.is(MapAtlasesMod.MAP_ATLAS.get())) {
                boolean didRemoveFilled = false;
                IMapCollection maps = MapAtlasItem.getMaps(stack, levelRef.get());
                if (!maps.isEmpty()) {
                    var slice = MapAtlasItem.getSelectedSlice(stack, levelRef.get().dimension());
                    maps.remove(getMapToRemove(inv, maps, slice));
                    var tree = maps.getHeightTree(slice.dimension(), slice.type());
                    if (!tree.contains(slice.heightOrTop())) {
                        Optional<Integer> first = tree.stream().findFirst();
                        if (first.isPresent()) {
                            Integer newH = first.get();
                            MapAtlasItem.setSelectedSlice(stack, Slice.of(slice.type(),
                                    newH, slice.dimension()));
                        }
                    }
                    didRemoveFilled = true;
                }
                int emptyMaps = MapAtlasItem.getEmptyMaps(stack);
                if (emptyMaps > 0 && !didRemoveFilled) {
                    int multiplier = MapAtlasesConfig.mapEntryValueMultiplier.get();
                    int amountToSet = Math.max(emptyMaps - multiplier, 0);
                    MapAtlasItem.setEmptyMaps(stack, amountToSet);
                }
            }
            list.add(stack);
        }
        return list;
    }

    public boolean canCraftInDimensions(int width, int height) {
        return width + height >= 2;
    }

    @Override
    public RecipeSerializer<MapAtlasesCutExistingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(java.util.List.of(
                Ingredient.of(MapAtlasesMod.MAP_ATLAS.get()),
                Ingredient.of(Items.SHEARS)
        ));
    }

    @Override
    public CraftingBookCategory category() {
        return category;
    }
}
