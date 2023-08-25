package pebjebs.mapatlases.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pebjebs.mapatlases.client.MapAtlasesClient;

@Mixin(value = MapRenderer.MapInstance.class, priority = 1100)
public class MapRendererMixin {

    @Inject(method = "draw",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"))
    private void scaleProxy(PoseStack poseStack, MultiBufferSource bufferSource, boolean active, int packedLight, CallbackInfo ci) {
        float multiplier = MapAtlasesClient.getWorldMapZoomLevel();
        poseStack.scale(multiplier, multiplier, 1);
    }
}
