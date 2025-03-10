package pepjebs.mapatlases.fabric;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.MapDecorationTextureManager;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class PlatStuffImpl {
    public static boolean isShear(ItemStack bottomItem) {
        return bottomItem.is(ConventionalItemTags.SHEAR_TOOLS);
    }

    public static boolean isBoss(EntityType<?> type) {
        return type == EntityType.WARDEN || type == EntityType.ENDER_DRAGON || type ==EntityType.ELDER_GUARDIAN || type == EntityType.WITHER;
    }

    public static void drawString(GuiGraphics g, Font font, String text, float x, float y, int i, boolean b) {
        g.drawString(font,text, (int) x, (int) y,i,b);
    }

    public static boolean isSimple(NonNullList<Ingredient> ingredients) {
        return true;
    }

    public static boolean findMatches(List<ItemStack> inputs, NonNullList<Ingredient> ingredients) {
        return false;
    }

    public static Pair<Boolean, Vec3> fireTeleportEvent(ServerPlayer player, double pX, double pY, double pZ) {
        return Pair.of(false, new Vec3(pX,pY,pZ));
    }

    @Environment(EnvType.CLIENT)
    public static boolean renderForgeMapDecoration(MapDecoration mapdecoration, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, MapItemSavedData data, MapDecorationTextureManager decorationTextures, boolean active, int packedLight, int index) {
        return false;
    }
}
