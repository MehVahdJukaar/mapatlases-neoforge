package pepjebs.mapatlases.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
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
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MapAtlasesAddRecipe extends CustomRecipe {
    public static final MapCodec<MapAtlasesAddRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(MapAtlasesAddRecipe::category)
    ).apply(instance, MapAtlasesAddRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MapAtlasesAddRecipe> STREAM_CODEC = StreamCodec.of(
            (buffer, recipe) -> buffer.writeEnum(recipe.category()),
            buffer -> new MapAtlasesAddRecipe(buffer.readEnum(CraftingBookCategory.class))
    );

    public static final RecipeSerializer<MapAtlasesAddRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final CraftingBookCategory category;
    private WeakReference<Level> levelRef = new WeakReference<>(null);

    public MapAtlasesAddRecipe(CraftingBookCategory category) {
        super();
        this.category = category;
    }

    @Override
    public boolean matches(CraftingInput inv, Level level) {
        ItemStack atlas = ItemStack.EMPTY;
        int stickyCount = 0;
        int emptyMaps = 0;
        List<MapDataHolder> filledMaps = new ArrayList<>();

        for (int j = 0; j < inv.size(); ++j) {
            ItemStack itemstack = inv.getItem(j);
            if (itemstack.isEmpty()) continue;
            if (itemstack.is(MapAtlasesMod.MAP_ATLAS.get())) {
                if (!atlas.isEmpty()) return false;
                atlas = itemstack;
            } else if (itemstack.is(Items.SLIME_BALL) || itemstack.is(Items.HONEY_BOTTLE)) {
                stickyCount++;
            } else if (isEmptyMap(itemstack)) {
                emptyMaps++;
            } else if (MapAtlasesAccessUtils.isValidFilledMap(itemstack)) {
                filledMaps.add(MapAtlasesAccessUtils.findMapFromItemStack(level, itemstack));
            } else {
                return false;
            }
        }

        int totalMaps = emptyMaps + filledMaps.size();
        if (atlas.isEmpty() || stickyCount == 0 || totalMaps == 0) return false;
        if (stickyCount != totalMaps) return false; // sticky must match maps exactly

        IMapCollection maps = MapAtlasItem.getMaps(atlas, level);
        int mapCount = maps.getCount() + MapAtlasItem.getEmptyMaps(atlas);
        if (MapAtlasItem.getMaxMapCount() != -1 && mapCount + totalMaps > MapAtlasItem.getMaxMapCount()) return false;

        int atlasScale = maps.getScale();
        for (var d : filledMaps) {
            if (d.data.scale != atlasScale) return false;
            if (maps.select(d.makeKey()) != null) return false;
        }

        levelRef = new WeakReference<>(level);
        return true;
    }

    private boolean isEmptyMap(ItemStack itemstack) {
        if (itemstack.isEmpty()) return false;
        if (MapAtlasesAccessUtils.isValidEmptyMap(itemstack)) {
            return MapAtlasesConfig.enableEmptyMapEntryAndFill.get();
        }
        if (itemstack.is(Items.PAPER)) {
            return MapAtlasesConfig.acceptPaperForEmptyMaps.get();
        }
        return false;
    }

    @Override
    public ItemStack assemble(CraftingInput inv) {
        Level level = levelRef.get();
        ItemStack atlas = ItemStack.EMPTY;
        int emptyMapCount = 0;
        List<Integer> mapIds = new ArrayList<>();

        for (int j = 0; j < inv.size(); ++j) {
            ItemStack itemstack = inv.getItem(j);
            if (itemstack.is(MapAtlasesMod.MAP_ATLAS.get())) {
                atlas = itemstack.copyWithCount(1);
            } else if (isEmptyMap(itemstack)) {
                emptyMapCount++;
            } else if (MapAtlasesAccessUtils.isValidFilledMap(itemstack)) {
                Integer mapId = MapAtlasesAccessUtils.getMapId(itemstack);
                if (mapId != null) mapIds.add(mapId);
            }
        }

        emptyMapCount *= MapAtlasesConfig.mapEntryValueMultiplier.get();
        IMapCollection maps = MapAtlasItem.getMaps(atlas, level);
        for (var i : mapIds) {
            maps.add(i, level);
        }
        MapAtlasItem.increaseEmptyMaps(atlas, emptyMapCount);

        return atlas;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput inv) {
        NonNullList<ItemStack> remainingItems = NonNullList.withSize(inv.size(), ItemStack.EMPTY);

        for (int j = 0; j < inv.size(); ++j) {
            if (inv.getItem(j).is(Items.HONEY_BOTTLE)) {
                remainingItems.set(j, new ItemStack(Items.GLASS_BOTTLE, 1));
            }
        }

        return remainingItems;
    }

    @Override
    public RecipeSerializer<MapAtlasesAddRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(java.util.List.of(
                Ingredient.of(MapAtlasesMod.MAP_ATLAS.get()),
                Ingredient.of(Items.SLIME_BALL, Items.HONEY_BOTTLE),
                Ingredient.of(Items.MAP, Items.PAPER)
        ));
    }

    @Override
    public CraftingBookCategory category() {
        return category;
    }

    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }
}