package pepjebs.mapatlases.integration.moonlight;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;

public class EntityPinDecorationRenderer extends AtlasOnlyDecorationRenderer<EntityPinDecoration> {

    public EntityPinDecorationRenderer(ResourceLocation texture) {
        super(texture);
    }

    @Override
    public boolean render(EntityPinDecoration decoration, PoseStack matrixStack, VertexConsumer vertexBuilder,
                          MultiBufferSource buffer, @Nullable MapItemSavedData data, boolean isOnFrame, int light, int index, boolean rendersText) {
        Entity entity = decoration.getEntity();
        if (entity == null || data == null || entity.isRemoved()) return false;
        double worldX = entity.getX();
        double worldZ = entity.getZ();
        double rotation = entity.getYRot();

        int scaleFactor = 1 << data.scale;
        float f = (float) (worldX - data.centerX) / scaleFactor;
        float f1 = (float) (worldZ - data.centerZ) / scaleFactor;
        byte mapX = (byte) ((int) ((f * 2.0F) + 0.5D));
        byte mapY = (byte) ((int) ((f1 * 2.0F) + 0.5D));

        if (f >= -64.0F && f1 >= -64.0F && f <= 64.0F && f1 <= 64.0F) {
            if (MapAtlasesClientConfig.radarRotation.get()) {
                rotation = rotation + (rotation < 0.0D ? -8.0D : 8.0D);
                byte rot = (byte) ((int) (rotation * 16.0D / 360.0D));
                decoration.setRot(rot);
            }
            decoration.setX(mapX);
            decoration.setY(mapY);

            return super.render(decoration, matrixStack, vertexBuilder, buffer, data, isOnFrame, light, index, rendersText);
        }
        return false;
    }


    @Override
    public int getAlpha(EntityPinDecoration decoration) {
        double y = Minecraft.getInstance().player.getY();
        double diff = Math.abs(decoration.getEntity().getY() - y)/15;
        double i = diff * diff * diff;
        return (int) Math.max(0, 255 * (1 - i));
    }


}
