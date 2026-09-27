package pepjebs.mapatlases.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.blockentity.LecternRenderer;
import net.minecraft.client.renderer.blockentity.state.LecternRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pepjebs.mapatlases.client.AtlasLecternRenderState;
import pepjebs.mapatlases.utils.AtlasLectern;

import static pepjebs.mapatlases.client.MapAtlasesClient.*;

@Mixin(LecternRenderer.class)
public abstract class LecternRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/level/block/entity/LecternBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/LecternRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
            at = @At("RETURN"))
    private void mapAtlases$extractAtlasTexture(LecternBlockEntity tile, LecternRenderState state, float partialTicks, Vec3 cameraPos,
                                                ModelFeatureRenderer.CrumblingOverlay breakProgress, CallbackInfo ci) {
        SpriteId texture = null;
        if (tile instanceof AtlasLectern ah && ah.mapatlases$hasAtlas()) {
            Level level = tile.getLevel();
            var dimension = level == null ? null : level.dimension();
            texture = OTHER_TEXTURE;
            if (dimension == Level.OVERWORLD) texture = OVERWORLD_TEXTURE;
            else if (dimension == Level.NETHER) texture = NETHER_TEXTURE;
            else if (dimension == Level.END) texture = END_TEXTURE;
        }
        ((AtlasLecternRenderState) state).mapatlases$setAtlasTexture(texture);
    }

    @ModifyArg(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/LecternRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;IIILnet/minecraft/client/resources/model/sprite/SpriteId;Lnet/minecraft/client/resources/model/sprite/SpriteGetter;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"),
            index = 6)
    private SpriteId mapAtlases$renderAtlasInLectern(SpriteId original, @Local(argsOnly = true) LecternRenderState state) {
        SpriteId atlas = ((AtlasLecternRenderState) state).mapatlases$getAtlasTexture();
        return atlas == null ? original : atlas;
    }

}
