package pepjebs.mapatlases.neoforge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.MapDecorationTextureManager;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.gui.map.MapDecorationRendererManager;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.RecipeMatcher;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;

import java.util.List;

public class PlatStuffImpl {
    public static boolean isShear(ItemStack bottomItem) {
        return bottomItem.is(Tags.Items.TOOLS_SHEAR);
    }

    public static boolean isBoss(EntityType<?> type) {
        return type.is(Tags.EntityTypes.BOSSES);
    }

    public static void drawString(GuiGraphics g, Font font, String text, float x, float y, int i, boolean b) {
        g.drawString(font, text, x, y, i, b);
    }

    public static boolean isSimple(NonNullList<Ingredient> ingredients) {
        return ingredients.stream().allMatch(Ingredient::isSimple);
    }

    public static boolean findMatches(List<ItemStack> inputs, NonNullList<Ingredient> ingredients) {
        return RecipeMatcher.findMatches(inputs, ingredients) != null;
    }

    public static Pair<Boolean, Vec3> fireTeleportEvent(ServerPlayer player, double pX, double pY, double pZ) {
        EntityTeleportEvent event = EventHooks.onEntityTeleportCommand(player, pX, pY, pZ);
        return Pair.of(event.isCanceled(), new Vec3(event.getTargetX(), event.getTargetY(), event.getTargetZ()));
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean renderForgeMapDecoration(MapDecoration mapdecoration, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                                   MapItemSavedData data, MapDecorationTextureManager decorationTextures, boolean active, int packedLight, int index) {
        return MapDecorationRendererManager.render(mapdecoration, poseStack, bufferSource, data, decorationTextures, active, packedLight, index);
    }
}
