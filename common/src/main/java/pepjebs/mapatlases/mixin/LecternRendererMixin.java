package pepjebs.mapatlases.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.LecternRenderer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import pepjebs.mapatlases.utils.AtlasLectern;

import static pepjebs.mapatlases.client.MapAtlasesClient.*;

@Mixin(LecternRenderer.class)
public abstract class LecternRendererMixin {

    @ModifyArg(
            method = "render(Lnet/minecraft/world/level/block/entity/LecternBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/BookModel;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V")
    )
    private VertexConsumer renderMapAtlasInLectern(VertexConsumer original, @Local(argsOnly = true) LecternBlockEntity tile, @Local(argsOnly = true) MultiBufferSource buffer) {
        if (tile instanceof AtlasLectern ah && ah.mapatlases$hasAtlas() && buffer != null) {
            Level level = tile.getLevel();
            if (level == null) {
                return OTHER_TEXTURE.buffer(buffer, RenderType::entitySolid);
            }
            var dimension = level.dimension();
            if (dimension == Level.OVERWORLD) {
                return OVERWORLD_TEXTURE.buffer(buffer, RenderType::entitySolid);
            } else if (dimension == Level.NETHER) {
                return NETHER_TEXTURE.buffer(buffer, RenderType::entitySolid);
            } else if (dimension == Level.END) {
                return END_TEXTURE.buffer(buffer, RenderType::entitySolid);
            } else {
                return OTHER_TEXTURE.buffer(buffer, RenderType::entitySolid);
            }
        }
        return original;
    }

}