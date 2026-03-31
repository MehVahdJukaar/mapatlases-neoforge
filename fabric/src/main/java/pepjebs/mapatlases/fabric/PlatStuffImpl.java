package pepjebs.mapatlases.fabric;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class PlatStuffImpl {
    public static boolean isShear(ItemStack bottomItem) {
        return bottomItem.is(Items.SHEARS);
    }

    public static boolean isBoss(EntityType<?> type) {
        return type == EntityType.WARDEN || type == EntityType.ENDER_DRAGON || type ==EntityType.ELDER_GUARDIAN || type == EntityType.WITHER;
    }

    public static void drawString(GuiGraphicsExtractor g, Font font, String text, float x, float y, int i, boolean b) {
        g.text(font, text, Math.round(x), Math.round(y), i, b);
    }

    public static boolean isSimple(NonNullList<Ingredient> ingredients) {
        return false;
    }

    public static boolean findMatches(List<ItemStack> inputs, NonNullList<Ingredient> ingredients) {
        if (inputs.size() != ingredients.size()) {
            return false;
        }
        return findMatches(inputs, ingredients, new boolean[inputs.size()], 0);
    }

    public static Pair<Boolean, Vec3> fireTeleportEvent(ServerPlayer player, double pX, double pY, double pZ) {
        return Pair.of(false, new Vec3(pX,pY,pZ));
    }

    private static boolean findMatches(List<ItemStack> inputs, NonNullList<Ingredient> ingredients, boolean[] used, int ingredientIndex) {
        if (ingredientIndex >= ingredients.size()) {
            return true;
        }

        Ingredient ingredient = ingredients.get(ingredientIndex);
        for (int inputIndex = 0; inputIndex < inputs.size(); inputIndex++) {
            if (used[inputIndex]) {
                continue;
            }
            if (!ingredient.test(inputs.get(inputIndex))) {
                continue;
            }

            used[inputIndex] = true;
            if (findMatches(inputs, ingredients, used, ingredientIndex + 1)) {
                return true;
            }
            used[inputIndex] = false;
        }
        return false;
    }
}
