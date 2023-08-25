package pepjebs.mapatlases.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.EnchantingTableBlockEntityRenderer;
import net.minecraft.client.render.block.entity.LecternBlockEntityRenderer;
import net.minecraft.client.render.entity.model.BookModel;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.item.MapAtlasItem;

@Mixin(LecternBlockEntityRenderer.class)
public class MapAtlasesLecternBlockEntityRenderer {

    private static final SpriteIdentifier OVERWORLD_TEXTURE =
            new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, MapAtlasesClient.LECTERN_OVERWORLD_ID);
    private static final SpriteIdentifier NETHER_TEXTURE =
            new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, MapAtlasesClient.LECTERN_NETHER_ID);
    private static final SpriteIdentifier END_TEXTURE =
            new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, MapAtlasesClient.LECTERN_END_ID);
    private static final SpriteIdentifier OTHER_TEXTURE =
            new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, MapAtlasesClient.LECTERN_OTHER_ID);

    @Redirect(
            method = "render",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/render/entity/model/BookModel;renderBook(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V"
            )
    )
    private void renderMapAtlasInLectern(
            // Call to renderBook
            BookModel model,
            MatrixStack matrices,
            VertexConsumer vertices,
            int light,
            int overlay,
            float red,
            float green,
            float blue,
            float alpha,
            // Call to render
            LecternBlockEntity lecternBlockEntity,
            float f,
            MatrixStack matrixStack,
            VertexConsumerProvider vertexConsumerProvider,
            int i,
            int j
    ) {
        BlockState blockState = lecternBlockEntity.getCachedState();
        VertexConsumer vertexConsumer;
        if (blockState.get(LecternBlock.HAS_BOOK) && blockState.contains(MapAtlasItem.HAS_ATLAS)
                && blockState.get(MapAtlasItem.HAS_ATLAS)) {

            if(lecternBlockEntity.getWorld() == null) {
                vertexConsumer =
                        OTHER_TEXTURE.getVertexConsumer(vertexConsumerProvider, RenderLayer::getEntitySolid);
            } else if(lecternBlockEntity.getWorld().getRegistryKey() == World.OVERWORLD) {
                vertexConsumer =
                        OVERWORLD_TEXTURE.getVertexConsumer(vertexConsumerProvider, RenderLayer::getEntitySolid);
            } else if(lecternBlockEntity.getWorld().getRegistryKey() == World.NETHER) {
                vertexConsumer =
                        NETHER_TEXTURE.getVertexConsumer(vertexConsumerProvider, RenderLayer::getEntitySolid);
            } else if(lecternBlockEntity.getWorld().getRegistryKey() == World.END) {
                vertexConsumer =
                        END_TEXTURE.getVertexConsumer(vertexConsumerProvider, RenderLayer::getEntitySolid);
            } else {
                vertexConsumer =
                        OTHER_TEXTURE.getVertexConsumer(vertexConsumerProvider, RenderLayer::getEntitySolid);
            }
        } else {
            vertexConsumer = vertices;
        }
        model.renderBook(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
    }
}