package pepjebs.mapatlases.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pepjebs.mapatlases.client.MapAtlasesClient;

@Mixin(value = MapRenderer.MapInstance.class, priority = 1200)
public class MapRendererMixin {

    @Shadow
    private boolean requiresUpload;

    @Inject(method = "draw",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V",
                    ordinal = 0),
            require = 1)
    private void mapAtlases$scaleProxy(PoseStack poseStack, MultiBufferSource bufferSource, boolean active, int packedLight, CallbackInfo ci) {
        MapAtlasesClient.modifyDecorationTransform(poseStack);
    }

    @Inject(method = "draw",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V",
                    ordinal = 1),
            require = 1)
    private void mapAtlases$scaleTextProxy(PoseStack poseStack, MultiBufferSource bufferSource, boolean active, int packedLight, CallbackInfo ci,
                                @Local(ordinal = 6) float width, @Local(ordinal = 7) float scale) {
        MapAtlasesClient.modifyTextDecorationTransform(poseStack, width, scale);
    }

    /*
    @WrapOperation(method = "draw",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/MapRenderer$MapInstance;updateTexture()V"))
    private void reduceUploads(MapRenderer.MapInstance instance, Operation<Void> original, @Share("needsUnset") LocalBooleanRef needsUnset) {
        //this is so we don't upload constantly encase it would happen multiple time in same tick due to packets
        int threshold = MapAtlasesClient.uploadFrequency();
        if (System.currentTimeMillis() - map_atlases$lastUpdateTime > threshold) {
            original.call(instance);
        } else needsUnset.set(true);
    }

    @Inject(method = "draw",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/MapRenderer$MapInstance;updateTexture()V",
                    shift = At.Shift.BY, by = 2))
    private void unsetUpdated(PoseStack poseStack, MultiBufferSource bufferSource, boolean active, int packedLight, CallbackInfo ci,
                              @Share("needsUnset") LocalBooleanRef needsUnset) {
        if (needsUnset.get()) this.requiresUpload = true;
    }*/


}
