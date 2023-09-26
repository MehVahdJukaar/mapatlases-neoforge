package pepjebs.mapatlases.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.client.MapVertexConsumer;

@Mixin(value = MapRenderer.MapInstance.class, priority = 1100)
public class MapRendererMixin {
    @Inject(method = "draw",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V",
                    ordinal = 0),
            require = 1)
    private void scaleProxy(PoseStack poseStack, MultiBufferSource bufferSource, boolean active, int packedLight, CallbackInfo ci) {
        MapAtlasesClient.modifyDecorationTransform(poseStack);
    }

    @ModifyVariable(method = "draw", at = @At(value = "STORE", ordinal = 0))
    private VertexConsumer wrapVertexConsumer(VertexConsumer original){
        return new MapVertexConsumer(original, 128);
    }

}
