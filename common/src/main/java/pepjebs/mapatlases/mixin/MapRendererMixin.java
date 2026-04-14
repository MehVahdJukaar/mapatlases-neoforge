package pepjebs.mapatlases.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.MapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pepjebs.mapatlases.client.MapAtlasesClient;

@Mixin(value = MapRenderer.class, priority = 1200)
public class MapRendererMixin {

    @Inject(method = "render",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V",
                    ordinal = 0),
            require = 1)
    private void scaleProxy(MapRenderState mapRenderState, PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector, boolean showOnlyFrame, int lightCoords,
            CallbackInfo ci) {
        MapAtlasesClient.modifyDecorationTransform(poseStack);
    }

    @Inject(method = "render",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V",
                    ordinal = 1),
            require = 1)
    private void scaleTextProxy(MapRenderState mapRenderState, PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector, boolean showOnlyFrame, int lightCoords,
            CallbackInfo ci,
            @Local(ordinal = 0) float width, @Local(ordinal = 0) float scale) {
        MapAtlasesClient.modifyTextDecorationTransform(poseStack, width, scale);
    }
}