package pepjebs.mapatlases.integration.moonlight;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.moonlight.api.map.client.MapDecorationClientManager;
import net.mehvahdjukaar.moonlight.api.map.client.MapDecorationRenderer;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecorationType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Holder;

public class CustomDecorationButton {

    public static void renderStaticMarker(GuiGraphics pGuiGraphics,
                                          Holder<MLMapDecorationType<?, ?>> type,
                                          float x, float y,
                                          int index, boolean outline, int alpha) {
        MapDecorationRenderer<?> renderer = MapDecorationClientManager.getRenderer(type);
        if (renderer != null) {
            PoseStack poseStack = pGuiGraphics.pose();
            poseStack.pushPose();
            poseStack.translate(x, y, 0.005);
            poseStack.scale(4, 4, -3);

            var buffer = pGuiGraphics.bufferSource();
            VertexConsumer vertexBuilder = buffer.getBuffer(MapDecorationClientManager.MAP_MARKERS_RENDER_TYPE);
            renderer.renderDecorationSprite(poseStack, buffer, vertexBuilder, LightTexture.FULL_BRIGHT, index,
                    -1, alpha, outline);

            poseStack.popPose();
        }
    }
}