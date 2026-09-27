package pepjebs.mapatlases.integration.moonlight;

import net.mehvahdjukaar.moonlight.api.map.client.MLDecorationRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;

public class EntityPinDecorationRenderer extends AtlasOnlyDecorationRenderer<EntityPinDecoration> {

    public EntityPinDecorationRenderer(Identifier texture) {
        super(texture);
    }

    @Override
    public boolean extract(EntityPinDecoration decoration, MapItemSavedData data, MLDecorationRenderState state) {
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

            return super.extract(decoration, data, state);
        }
        return false;
    }

    @Override
    protected int getColor(EntityPinDecoration decoration) {
        double y = Minecraft.getInstance().player.getY();
        double diff = Math.abs(decoration.getEntity().getY() - y) / 15;
        double i = diff * diff * diff;
        int alpha = (int) Math.max(0, 255 * (1 - i));
        return ARGB.color(alpha, 0xFFFFFF);
    }


}
