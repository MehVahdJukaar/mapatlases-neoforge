package pepjebs.mapatlases.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.blockentity.LecternRenderer;
import net.minecraft.client.renderer.blockentity.state.LecternRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.AtlasLecternRenderState;
import pepjebs.mapatlases.utils.AtlasLectern;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LecternRenderer.class)
public abstract class LecternRendererMixin {
    private static final SpriteId OVERWORLD_TEXTURE = new SpriteId(TextureAtlas.LOCATION_BLOCKS, MapAtlasesMod.res("entity/lectern_atlas"));
    private static final SpriteId NETHER_TEXTURE = new SpriteId(TextureAtlas.LOCATION_BLOCKS, MapAtlasesMod.res("entity/lectern_atlas_nether"));
    private static final SpriteId END_TEXTURE = new SpriteId(TextureAtlas.LOCATION_BLOCKS, MapAtlasesMod.res("entity/lectern_atlas_end"));
    private static final SpriteId OTHER_TEXTURE = new SpriteId(TextureAtlas.LOCATION_BLOCKS, MapAtlasesMod.res("entity/lectern_atlas_unknown"));

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/level/block/entity/LecternBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/LecternRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
            at = @At("TAIL")
    )
    private void captureAtlasState(LecternBlockEntity tile, LecternRenderState state, float partialTick, Vec3 cameraOffset,
                                   ModelFeatureRenderer.CrumblingOverlay overlay, CallbackInfo ci) {
        if (state instanceof AtlasLecternRenderState atlasState) {
            atlasState.mapatlases$setHasAtlas(tile instanceof AtlasLectern ah && ah.mapatlases$hasAtlas());
            atlasState.mapatlases$setTexture(getLecternTexture(tile.getLevel()));
        }
    }

    @ModifyArg(
            method = "submit(Lnet/minecraft/client/renderer/blockentity/state/LecternRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;IIILnet/minecraft/client/resources/model/sprite/SpriteId;Lnet/minecraft/client/resources/model/sprite/SpriteGetter;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"),
            index = 6
    )
    private SpriteId renderMapAtlasInLectern(SpriteId original, @Local(argsOnly = true) LecternRenderState state) {
        if (state.hasBook && state instanceof AtlasLecternRenderState atlasState && atlasState.mapatlases$hasAtlas()) {
            return atlasState.mapatlases$getTexture();
        }
        return original;
    }

    private static SpriteId getLecternTexture(Level level) {
        if (level == null) {
            return OTHER_TEXTURE;
        }
        var dimension = level.dimension();
        if (dimension == Level.OVERWORLD) {
            return OVERWORLD_TEXTURE;
        }
        if (dimension == Level.NETHER) {
            return NETHER_TEXTURE;
        }
        if (dimension == Level.END) {
            return END_TEXTURE;
        }
        return OTHER_TEXTURE;
    }

}
