package pepjebs.mapatlases;

import com.mojang.datafixers.util.Pair;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.phys.Vec3;
import pepjebs.mapatlases.fabric.PlatStuffImpl;

import java.util.List;

public class PlatStuff {
    @ExpectPlatform
    public static boolean isShear(ItemStack bottomItem) {
        return PlatStuffImpl.isShear(bottomItem);
    }

    @ExpectPlatform
    public static boolean isBoss(EntityType<?> type) {
        return PlatStuffImpl.isBoss(type);
    }

    @Environment(EnvType.CLIENT)
    @ExpectPlatform
    public static void drawString(GuiGraphicsExtractor g, Font font, String text, float x, float y, int i, boolean b) {
        PlatStuffImpl.drawString(g, font, text, x, y, i, b);
    }

    @ExpectPlatform
    public static boolean isSimple(NonNullList<Ingredient> ingredients) {
        return PlatStuffImpl.isSimple(ingredients);
    }

    @ExpectPlatform
    public static boolean findMatches(List<ItemStack> inputs, NonNullList<Ingredient> ingredients) {
        return PlatStuffImpl.findMatches(inputs, ingredients);
    }

    @ExpectPlatform
    public static Pair<Boolean, Vec3> fireTeleportEvent(ServerPlayer player, double pX, double pY, double pZ) {
        return PlatStuffImpl.fireTeleportEvent(player, pX, pY, pZ);
    }
}
