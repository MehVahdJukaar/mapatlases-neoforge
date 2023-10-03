package pepjebs.mapatlases.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.joml.Matrix4f;
import pepjebs.mapatlases.capabilities.MapKey;
import pepjebs.mapatlases.client.screen.AtlasOverviewScreen;
import pepjebs.mapatlases.item.MapAtlasItem;

public class AtlasInHandRenderer {

    private static final RenderType MAP_BACKGROUND = RenderType.text(new ResourceLocation("textures/map/map_background.png"));
    private static final RenderType MAP_BACKGROUND_CHECKERBOARD = RenderType.text(new ResourceLocation("textures/map/map_background_checkerboard.png"));
    private static final float MAP_PRE_ROT_SCALE = 0.38F;
    private static final float MAP_GLOBAL_X_POS = -0.5F;
    private static final float MAP_GLOBAL_Y_POS = -0.5F;
    private static final float MAP_GLOBAL_Z_POS = 0.0F;
    private static final float MAP_FINAL_SCALE = 0.0078125F;
    private static final int MAP_BORDER = 7;
    private static final int MAP_HEIGHT = 128;
    private static final int MAP_WIDTH = 128;

    public static void render(PoseStack pPoseStack, MultiBufferSource pBuffer, int pCombinedLight, ItemStack pStack, Minecraft mc) {
        if (mc.screen instanceof AtlasOverviewScreen) return;

        pPoseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        pPoseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        pPoseStack.scale(MAP_PRE_ROT_SCALE, MAP_PRE_ROT_SCALE, MAP_PRE_ROT_SCALE);
        pPoseStack.translate(MAP_GLOBAL_X_POS, MAP_GLOBAL_Y_POS, MAP_GLOBAL_Z_POS);
        pPoseStack.scale(MAP_FINAL_SCALE, MAP_FINAL_SCALE, MAP_FINAL_SCALE);

        MapKey activeMapKey = MapAtlasesClient.getActiveMapKey();
        var ss = MapAtlasItem.getMaps(MapAtlasesClient.getCurrentActiveAtlas(), mc.level).select(activeMapKey);
        if (ss == null) return;
        Integer integer = activeMapKey.slice().getMapId(ss.getFirst());
        MapItemSavedData mapitemsaveddata = ss.getSecond();
        VertexConsumer vertexconsumer = pBuffer.getBuffer(mapitemsaveddata == null ? MAP_BACKGROUND : MAP_BACKGROUND_CHECKERBOARD);
        Matrix4f matrix4f = pPoseStack.last().pose();
        vertexconsumer.vertex(matrix4f, -MAP_BORDER, MAP_HEIGHT + MAP_BORDER, 0.0F).color(255, 255, 255, 255).uv(0.0F, 1.0F).uv2(pCombinedLight).endVertex();
        vertexconsumer.vertex(matrix4f, MAP_WIDTH + MAP_BORDER, MAP_HEIGHT + MAP_BORDER, 0.0F).color(255, 255, 255, 255).uv(1.0F, 1.0F).uv2(pCombinedLight).endVertex();
        vertexconsumer.vertex(matrix4f, MAP_WIDTH + MAP_BORDER, -MAP_BORDER, 0.0F).color(255, 255, 255, 255).uv(1.0F, 0.0F).uv2(pCombinedLight).endVertex();
        vertexconsumer.vertex(matrix4f, -MAP_BORDER, -MAP_BORDER, 0.0F).color(255, 255, 255, 255).uv(0.0F, 0.0F).uv2(pCombinedLight).endVertex();
        if (mapitemsaveddata != null) {
            mc.gameRenderer.getMapRenderer().render(pPoseStack, pBuffer, integer, mapitemsaveddata, false, pCombinedLight);
        }

    }
}
