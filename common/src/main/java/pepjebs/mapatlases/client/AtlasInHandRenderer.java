package pepjebs.mapatlases.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import pepjebs.mapatlases.client.screen.AtlasOverviewScreen;
import pepjebs.mapatlases.utils.AtlasMap;

public class AtlasInHandRenderer {

    private static final RenderType MAP_BACKGROUND_CHECKERBOARD = RenderTypes.text(Identifier.withDefaultNamespace("textures/map/map_background_checkerboard.png"));
    private static final MapRenderState MAP_RENDER_STATE = new MapRenderState();
    private static final float MAP_PRE_ROT_SCALE = 0.38F;
    private static final float MAP_GLOBAL_X_POS = -0.5F;
    private static final float MAP_GLOBAL_Y_POS = -0.5F;
    private static final float MAP_GLOBAL_Z_POS = 0.0F;
    private static final float MAP_FINAL_SCALE = 0.0078125F;
    private static final int MAP_BORDER = 7;
    private static final int MAP_HEIGHT = 128;
    private static final int MAP_WIDTH = 128;

    public static void render(PoseStack pPoseStack, SubmitNodeCollector collector, int pCombinedLight, ItemStack pStack, Minecraft mc) {
        if (mc.screen instanceof AtlasOverviewScreen) return;

        AtlasMap state = MapAtlasesClient.getActiveMap();
        if (state != null) {
            pPoseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            pPoseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            pPoseStack.scale(MAP_PRE_ROT_SCALE, MAP_PRE_ROT_SCALE, MAP_PRE_ROT_SCALE);
            pPoseStack.translate(MAP_GLOBAL_X_POS, MAP_GLOBAL_Y_POS, MAP_GLOBAL_Z_POS);
            pPoseStack.scale(MAP_FINAL_SCALE, MAP_FINAL_SCALE, MAP_FINAL_SCALE);

            collector.submitCustomGeometry(pPoseStack, MAP_BACKGROUND_CHECKERBOARD, (pose, buffer) -> {
                buffer.addVertex(pose, -MAP_BORDER, MAP_HEIGHT + MAP_BORDER, 0.0F).setColor(-1).setUv(0.0F, 1.0F).setLight(pCombinedLight);
                buffer.addVertex(pose, MAP_WIDTH + MAP_BORDER, MAP_HEIGHT + MAP_BORDER, 0.0F).setColor(-1).setUv(1.0F, 1.0F).setLight(pCombinedLight);
                buffer.addVertex(pose, MAP_WIDTH + MAP_BORDER, -MAP_BORDER, 0.0F).setColor(-1).setUv(1.0F, 0.0F).setLight(pCombinedLight);
                buffer.addVertex(pose, -MAP_BORDER, -MAP_BORDER, 0.0F).setColor(-1).setUv(0.0F, 0.0F).setLight(pCombinedLight);
            });

            MapRenderer mapRenderer = mc.getMapRenderer();
            MapAtlasesClient.setIsDrawingAtlas(true);
            mapRenderer.extractRenderState(state.id, state.data, MAP_RENDER_STATE);
            MapAtlasesClient.setIsDrawingAtlas(false);
            mapRenderer.render(MAP_RENDER_STATE, pPoseStack, collector, false, pCombinedLight);
        }
    }
}
