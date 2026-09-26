package pepjebs.mapatlases.api;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import pepjebs.mapatlases.client.MapAtlasesClient;

public class MapAtlasesClientApi {

    /**
     * applies the atlas decoration scale and rotation config. call before rendering a map decoration
     */
    @ClientOnly
    public static void scaleDecoration(PoseStack poseStack) {
        MapAtlasesClient.modifyDecorationTransform(poseStack);
    }

    @ClientOnly
    public static void scaleDecorationText(PoseStack poseStack, float textWidth, float textScale) {
        MapAtlasesClient.modifyTextDecorationTransform(poseStack, textWidth, textScale);
    }
}
