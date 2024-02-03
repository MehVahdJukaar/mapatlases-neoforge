package pepjebs.mapatlases.forge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.util.RecipeMatcher;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.EntityTeleportEvent;

import java.util.List;

public class PlatStuffImpl {
    public static boolean isShear(ItemStack bottomItem) {
        return bottomItem.is(Tags.Items.SHEARS);
    }

    public static boolean isBoss(EntityType<?> type) {
        return type.is(Tags.EntityTypes.BOSSES);
    }

    public static void drawString(PoseStack g, Font font, String text, float x, float y, int i, boolean b) {
        GuiComponent.drawString(g, font, text, (int) x, (int) y, i);
    }

    public static boolean isSimple(NonNullList<Ingredient> ingredients) {
        return ingredients.stream().allMatch(Ingredient::isSimple);
    }

    public static boolean findMatches(List<ItemStack> inputs, NonNullList<Ingredient> ingredients) {
        return RecipeMatcher.findMatches(inputs, ingredients) != null;
    }

    public static Pair<Boolean, Vec3> fireTeleportEvent(ServerPlayer player, double pX, double pY, double pZ) {
        EntityTeleportEvent event = ForgeEventFactory.onEntityTeleportCommand(player, pX, pY, pZ);
        return Pair.of(event.isCanceled(), new Vec3(event.getTargetX(), event.getTargetY(), event.getTargetZ()));
    }
}
