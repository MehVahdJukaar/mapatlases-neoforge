package net.mehvahdjukaar.moonlight.api.platform;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.function.Predicate;
import java.util.function.Supplier;

public final class RegHelper {

    private RegHelper() {
    }

    public static Supplier<SoundEvent> registerSound(Identifier id) {
        SoundEvent value = Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
        return () -> value;
    }

    public static <T extends Recipe<?>> Supplier<RecipeSerializer<T>> registerRecipeSerializer(Identifier id,
                                                                                                Supplier<RecipeSerializer<T>> supplier) {
        RecipeSerializer<T> value = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id, supplier.get());
        return () -> value;
    }

    public static <T extends Item> Supplier<T> registerItem(Identifier id, Supplier<T> supplier) {
        T value = Registry.register(BuiltInRegistries.ITEM, id, supplier.get());
        return () -> value;
    }

    public static void addItemsToTabsRegistration(java.util.function.Consumer<ItemToTabEvent> consumer) {
    }

    public interface ItemToTabEvent {
        void addAfter(Object tab, Predicate<ItemStack> after, Item item);
    }
}
