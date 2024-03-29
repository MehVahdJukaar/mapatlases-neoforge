package pepjebs.mapatlases;

import com.mojang.datafixers.util.Pair;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class PlatStuff {
    @ExpectPlatform
    public static boolean isShear(ItemStack bottomItem) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isBoss(EntityType<?> type) {
        throw new AssertionError();
    }

    @Environment(EnvType.CLIENT)
    @ExpectPlatform
    public static void drawString(GuiGraphics g, Font font, String text, float x, float y, int i, boolean b) {

        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isSimple(NonNullList<Ingredient> ingredients) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean findMatches(List<ItemStack> inputs, NonNullList<Ingredient> ingredients) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Pair<Boolean, Vec3> fireTeleportEvent(ServerPlayer player, double pX, double pY, double pZ) {
        throw new AssertionError();
    }
}
