package pepjebs.mapatlases.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.LecternRenderer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pepjebs.mapatlases.utils.AtlasHolder;

import static pepjebs.mapatlases.client.MapAtlasesClient.*;

@Mixin(LecternRenderer.class)
public abstract class MapAtlasesLecternBlockEntityRenderer {

    @Unique
    @Nullable
    private static BlockEntity mapatlases$capturedBe = null;
    @Unique
    @Nullable
    private static MultiBufferSource mapatlases$capturedBuffer = null;

    @ModifyArg(
            method = "render(Lnet/minecraft/world/level/block/entity/LecternBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/BookModel;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V")
    )
    private VertexConsumer renderMapAtlasInLectern(VertexConsumer original) {
        if (mapatlases$capturedBe instanceof AtlasHolder ah && ah.mapatlases$hasAtlas()
                && mapatlases$capturedBuffer != null) {
            Level level = mapatlases$capturedBe.getLevel();
            if (level == null) {
                return OTHER_TEXTURE.buffer(mapatlases$capturedBuffer, RenderType::entitySolid);
            }
            var dimension = level.dimension();
            if (dimension == Level.OVERWORLD) {
                return OVERWORLD_TEXTURE.buffer(mapatlases$capturedBuffer, RenderType::entitySolid);
            } else if (dimension == Level.NETHER) {
                return NETHER_TEXTURE.buffer(mapatlases$capturedBuffer, RenderType::entitySolid);
            } else if (dimension == Level.END) {
                return END_TEXTURE.buffer(mapatlases$capturedBuffer, RenderType::entitySolid);
            } else {
                return OTHER_TEXTURE.buffer(mapatlases$capturedBuffer, RenderType::entitySolid);
            }
        }
        return original;
    }

    @Inject(method = "render(Lnet/minecraft/world/level/block/entity/LecternBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("HEAD"))
    private void captureBE(LecternBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, CallbackInfo ci) {
        mapatlases$capturedBe = blockEntity;
        mapatlases$capturedBuffer = bufferSource;
    }
}