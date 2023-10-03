package pepjebs.mapatlases.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pepjebs.mapatlases.client.MapAtlasesClient;
import twilightforest.TFMagicMapData;

@Pseudo
@Mixin(TFMagicMapData.TFMapDecoration.class)
public class CompatTFMapRendererMixin {

    @Inject(method = "render",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V",
                    ordinal = 0),
            require = 1)
    private void scaleProxy(int idx, CallbackInfoReturnable<Boolean> cir) {
        MapAtlasesClient.modifyDecorationTransform(TFMagicMapData.TFMapDecoration.RenderContext.stack);
    }
}
