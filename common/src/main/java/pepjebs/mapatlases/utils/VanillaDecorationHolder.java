package pepjebs.mapatlases.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mehvahdjukaar.moonlight.api.client.util.RenderUtil;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MapDecorationTextureManager;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import pepjebs.mapatlases.PlatStuff;
import pepjebs.mapatlases.networking.C2SRemoveMarkerPacket;

import java.util.Map;
import java.util.Optional;

public final class VanillaDecorationHolder extends DecorationHolder {
    private final MapDecoration deco;

    VanillaDecorationHolder(MapDecoration deco, String id, MapDataHolder data) {
        super(id, data, deco.type().unwrapKey().get().location(), deco.name().orElse(null));
        this.deco = deco;
    }

    public MapDecoration deco() {
        return deco;
    }

    @Override
    protected int decoX() {
        return deco.x();
    }

    @Override
    protected int decoY() {
        return deco.y();
    }

    @Override
    public void renderDecoration(GuiGraphics pGuiGraphics, float centerX, float centerY) {
        PoseStack matrices = pGuiGraphics.pose();
        MultiBufferSource.BufferSource bufferSource = pGuiGraphics.bufferSource();

        matrices.pushPose();
        matrices.translate(centerX, centerY, 0.001);
        matrices.mulPose(Axis.ZP.rotationDegrees((deco.rot() * 360) / 16.0F));
        matrices.scale(-4, -4, 1);

        MapDecorationTextureManager textures = Minecraft.getInstance().gameRenderer.getMapRenderer().decorationTextures;
        if (!PlatStuff.renderForgeMapDecoration(deco, matrices, bufferSource, data.data,
                textures, true, LightTexture.FULL_BRIGHT, 0)) {
            TextureAtlasSprite sprite = textures.get(deco);
            VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.text(sprite.atlasLocation()));
            RenderUtil.renderSprite(matrices, vertexConsumer, LightTexture.FULL_BRIGHT, 255, 255, 255, 255, sprite);
        }
        matrices.popPose();
    }

    @Override
    public void deleteMarker(Optional<BlockPos> lecternPos) {
        Map<String, MapDecoration> decorations = data.data.decorations;
        var d = decorations.get(id);
        if (d != null) {
            NetworkHelper.sendToServer(new C2SRemoveMarkerPacket(data.id, data.type, d.hashCode(), false, lecternPos));
            decorations.remove(id);
        }
    }

    @Override
    public boolean canDeleteMarker() {
        return !deco.type().value().explorationMapElement();
    }
}