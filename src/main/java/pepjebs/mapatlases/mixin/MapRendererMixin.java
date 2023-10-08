package pepjebs.mapatlases.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pepjebs.mapatlases.client.MapAtlasesClient;

@Mixin(value = MapRenderer.MapInstance.class, priority = 1100)
public class MapRendererMixin {
    @Inject(method = "draw",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"),
            require = 2)
    private void scaleProxy(PoseStack poseStack, MultiBufferSource bufferSource, boolean active, int packedLight, CallbackInfo ci) {
        MapAtlasesClient.modifyDecorationTransform(poseStack);
    }

    @Inject(method = "draw",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V",
                    ordinal = 1),
            require = 1)
    private void scaleTextProxy(PoseStack poseStack, MultiBufferSource bufferSource, boolean active, int packedLight, CallbackInfo ci,
                                @Local(ordinal = 6) float width,  @Local(ordinal = 7) float scale) {
        MapAtlasesClient.modifyTextDecorationTransform(poseStack, width, scale);
    }


}
