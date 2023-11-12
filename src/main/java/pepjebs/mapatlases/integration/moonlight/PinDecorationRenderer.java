package pepjebs.mapatlases.integration.moonlight;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.mehvahdjukaar.moonlight.api.map.client.DecorationRenderer;
import net.mehvahdjukaar.moonlight.api.map.client.MapDecorationClientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.client.MapAtlasesClient;

public class PinDecorationRenderer extends DecorationRenderer<PinDecoration> {

    public PinDecorationRenderer(ResourceLocation texture) {
        super(texture);
    }

    @Override
    public boolean render(PinDecoration decoration, PoseStack matrixStack, VertexConsumer vertexBuilder, MultiBufferSource buffer, @Nullable MapItemSavedData mapData, boolean isOnFrame, int light, int index, boolean rendersText) {
        if(!MapAtlasesClient.isDrawingAtlas())return false;
        return super.render(decoration, matrixStack, vertexBuilder, buffer, mapData, isOnFrame, light, index, rendersText);
    }

    @Override
    public void renderSprite(PinDecoration decoration, PoseStack poseStack, VertexConsumer vertexBuilder, int light, int index) {
        super.renderSprite(decoration, poseStack, vertexBuilder, light, index);

        if(decoration.focused) {
            RenderSystem.setShaderColor(1, 1, 1, 1);
            VertexConsumer vb2 = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(PinDecorationRenderer.getOutlineRenderType());
            for (int j = -1; j <= 1; ++j) {
                for (int k = -1; k <= 1; ++k) {
                    if (j != 0 || k != 0) {
                        poseStack.pushPose();
                        poseStack.translate(j * 0.125, k * 0.125, 0.0015);
                        super.renderSprite(decoration, poseStack, vb2, light, index);
                        poseStack.popPose();
                    }
                }
            }
        }


    }

    public static RenderType getOutlineRenderType() {
        return R.COLOR_TEXT;
    }

    private static class R extends RenderType {
        protected static final RenderStateShard.ShaderStateShard SHARD = new RenderStateShard.ShaderStateShard(
                () -> MapAtlasesClient.TEXT_ALPHA_SHADER);

        public static final RenderType COLOR_TEXT = create("map_atlases_text_colored",
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, 256, false, true,

                CompositeState.builder()
                        .setShaderState(SHARD)
                        .setTextureState(new TextureStateShard(MapDecorationClientManager.LOCATION_MAP_MARKERS,
                                false, true))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setLightmapState(LIGHTMAP)
                        .createCompositeState(false));

        public R(String pName, VertexFormat pFormat, VertexFormat.Mode pMode, int pBufferSize, boolean pAffectsCrumbling, boolean pSortOnUpload, Runnable pSetupState, Runnable pClearState) {
            super(pName, pFormat, pMode, pBufferSize, pAffectsCrumbling, pSortOnUpload, pSetupState, pClearState);
        }
    }

}
