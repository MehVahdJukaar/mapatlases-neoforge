package pepjebs.mapatlases.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.MapDecorationTextureManager;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pepjebs.mapatlases.client.MapAtlasesClient;
import twilightforest.client.renderer.map.MagicMapPlayerIconRenderer;

@Pseudo
@Mixin(MagicMapPlayerIconRenderer.class)
public abstract class CompatTFMapRendererMixin {
    @Inject(method = "render",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V",
                    ordinal = 0),
            require = 1)
    private void mapAtlases$scaleProxy(MapDecoration decoration, PoseStack stack, MultiBufferSource bufferSource, MapItemSavedData mapData, MapDecorationTextureManager decorationTextures, boolean inItemFrame, int packedLight, int index, CallbackInfoReturnable<Boolean> cir) {
        MapAtlasesClient.modifyDecorationTransform(stack);
    }
}
