package pepjebs.mapatlases.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;

public record MapVertexConsumer(VertexConsumer original, int textureSize) implements VertexConsumer {

    private static final float RATIO = 0;//(float) (double) MapAtlasesClientConfig.mapTextureShrink.get();

    @Override
    public VertexConsumer vertex(double pX, double pY, double pZ) {
        original.vertex(pX, pY, pZ);
        return this;
    }

    @Override
    public VertexConsumer color(int pRed, int pGreen, int pBlue, int pAlpha) {
        original.color(pRed, pGreen, pBlue, pAlpha);
        return this;
    }

    @Override
    public VertexConsumer uv(float u0, float v0) {
        float shrink = RATIO / textureSize;
        original.uv(Mth.lerp(shrink, u0, 0.5f), Mth.lerp(shrink, v0, 0.5f));
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int pU, int pV) {
        original.overlayCoords(pU, pV);
        return this;
    }

    @Override
    public VertexConsumer uv2(int pU, int pV) {
        original.uv2(pU, pV);
        return this;
    }

    @Override
    public VertexConsumer normal(float pX, float pY, float pZ) {
        original.normal(pX, pY, pZ);
        return this;
    }

    @Override
    public void endVertex() {
        original.endVertex();
    }

    @Override
    public void defaultColor(int pDefaultR, int pDefaultG, int pDefaultB, int pDefaultA) {
        original.defaultColor(pDefaultR, pDefaultG, pDefaultB, pDefaultA);
    }

    @Override
    public void unsetDefaultColor() {
        original.unsetDefaultColor();
    }
}
