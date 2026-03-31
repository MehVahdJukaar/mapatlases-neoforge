package pepjebs.mapatlases.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.SupplementariesCompat;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.IMapCollection;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.lang.ref.WeakReference;

public class AntiqueAtlasRecipe extends CustomRecipe {
    public static final MapCodec<AntiqueAtlasRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(AntiqueAtlasRecipe::category)
    ).apply(instance, AntiqueAtlasRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AntiqueAtlasRecipe> STREAM_CODEC = StreamCodec.of(
            (buffer, recipe) -> buffer.writeEnum(recipe.category()),
            buffer -> new AntiqueAtlasRecipe(buffer.readEnum(CraftingBookCategory.class))
    );

    public static final RecipeSerializer<AntiqueAtlasRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final CraftingBookCategory category;
    private WeakReference<Level> levelRef = new WeakReference<>(null);

    public AntiqueAtlasRecipe(CraftingBookCategory category) {
        super();
        this.category = category;
    }

    @Override
    public boolean matches(CraftingInput inv, Level level) {
        if (!MapAtlasesMod.SUPPLEMENTARIES) return false;
        ItemStack atlas = ItemStack.EMPTY;
        ItemStack ink = ItemStack.EMPTY;
        // ensure 1 and one only atlas
        for (int j = 0; j < inv.size(); ++j) {
            ItemStack itemstack = inv.getItem(j);
            if (itemstack.is(MapAtlasesMod.MAP_ATLAS.get())) {
                if (!atlas.isEmpty()) return false;
                if (SupplementariesCompat.hasAntiqueInk(itemstack)) return false;
                atlas = itemstack;
            } else if (SupplementariesCompat.isAntiqueInk(itemstack)) {
                if (!ink.isEmpty()) return false;
                ink = itemstack;
            } else if (!itemstack.isEmpty()) return false;
        }
        if (!atlas.isEmpty() && !ink.isEmpty()) {
            levelRef = new WeakReference<>(level);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack assemble(CraftingInput inv) {

        Level level = levelRef.get();
        ItemStack newAtlas = ItemStack.EMPTY;
        ItemStack oldAtlas = ItemStack.EMPTY;
        // ensure 1 and one only atlas
        for (int j = 0; j < inv.size(); ++j) {
            ItemStack itemstack = inv.getItem(j);
            if (itemstack.is(MapAtlasesMod.MAP_ATLAS.get())) {
                newAtlas = itemstack.copyWithCount(1);
                oldAtlas = itemstack;
            }
        }

        // Get the Map Ids in the Grid
        // Set NBT Data
        IMapCollection maps = MapAtlasItem.getMaps(newAtlas, level);
        IMapCollection oldMaps = MapAtlasItem.getMaps(oldAtlas, level);
        for (MapDataHolder holder : maps.getAll()) {
            oldMaps.remove(holder);
            Integer newId = SupplementariesCompat.createAntiqueMapData(holder.data,level,true, false);
            if(newId != null) oldMaps.add(newId, level);
        }
        SupplementariesCompat.setAntiqueInk(newAtlas);
        return newAtlas;
    }

    @Override
    public RecipeSerializer<AntiqueAtlasRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public PlacementInfo placementInfo() {
        if (!MapAtlasesMod.SUPPLEMENTARIES) {
            return PlacementInfo.NOT_PLACEABLE;
        }
        return PlacementInfo.create(java.util.List.of(
                Ingredient.of(MapAtlasesMod.MAP_ATLAS.get())
        ));
    }

    @Override
    public CraftingBookCategory category() {
        return category;
    }

    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

}
