package pepjebs.mapatlases.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mehvahdjukaar.moonlight.api.client.util.RenderUtil;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MapDecorationTextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.PlatStuff;
import pepjebs.mapatlases.client.screen.AtlasScreenUtils;
import pepjebs.mapatlases.networking.C2SRemoveMarkerPacket;

import java.util.Locale;
import java.util.Map;

import static pepjebs.mapatlases.client.AbstractAtlasDisplay.MAP_DIMENSION;

public final class VanillaDecorationHolder extends DecorationHolder {
    private final MapDecoration deco;

    VanillaDecorationHolder(MapDecoration deco, String id, MapDataHolder data) {
        super(id, data, getSortingString(deco));
        this.deco = deco;
    }

    public MapDecoration deco() {
        return deco;
    }

    @Override
    public double decorationDistSq(double px, double pz) {
        var d = this.data.data;
        double wx = d.centerX, wz = d.centerZ;
        int scale = 1 << d.scale;
        wx += scale * deco.x() / 2.0;
        wz += scale * deco.y() / 2.0;
        return Mth.square(wx - px) + Mth.square(wz - pz);
    }

    @Override
    public double getWorldX() {
        return data.data.centerX - decoPos(deco.x(), data.data);
    }

    @Override
    public double getWorldZ() {
        return data.data.centerZ - decoPos(deco.y(), data.data);
    }

    @Override
    public Component getDecorationName() {
        return deco.name().orElseGet(() -> Component.literal(
                AtlasScreenUtils.getReadableName(deco.type().unwrapKey().get()
                        .location().getPath().toLowerCase(Locale.ROOT))));
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
    public void deleteMarker() {
        Map<String, MapDecoration> decorations = data.data.decorations;
        var d = decorations.get(id);
        if (d != null) {
            NetworkHelper.sendToServer(new C2SRemoveMarkerPacket(data.id, data.type, d.hashCode(), false));
            decorations.remove(id);
        }
    }

    @Override
    public boolean canDeleteMarker() {
        return !deco.type().value().explorationMapElement();
    }

    private static double decoPos(int coord, MapItemSavedData mapData) {
        float s = (1 << mapData.scale) * (float) MAP_DIMENSION;
        return (s / 2.0d) - ((s / 2.0d) * ((coord + MAP_DIMENSION) / (float) MAP_DIMENSION));
    }

    private static String getSortingString(MapDecoration md) {
        StringBuilder sb = new StringBuilder();
        sb.append(md.type().unwrapKey().get().location().getPath());
        md.name().ifPresent(n -> sb.append(" ").append(n.getString()));
        return sb.toString();
    }
}