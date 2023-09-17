package pepjebs.mapatlases.mixin;

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
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V",
                    ordinal = 0),
            require = 1)
    private void scaleProxy(PoseStack poseStack, MultiBufferSource bufferSource, boolean active, int packedLight, CallbackInfo ci) {
        MapAtlasesClient.modifyDecorationTransform(poseStack);
    }

}
