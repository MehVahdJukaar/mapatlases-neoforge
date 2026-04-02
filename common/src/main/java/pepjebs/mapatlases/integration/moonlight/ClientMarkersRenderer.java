package pepjebs.mapatlases.integration.moonlight;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import pepjebs.mapatlases.map_collection.IMapCollection;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.Slice;
import pepjebs.mapatlases.client.ui.MapAtlasesHUD;

public class ClientMarkersRenderer {

    public static void renderDecorationPreview(GuiGraphicsExtractor graphics, float x, float y, int index, boolean outline, int alpha) {
        CustomDecorationButton.renderStaticMarker(graphics, index, x, y, 1, outline, alpha);
    }

    public static void drawSmallPins(GuiGraphicsExtractor graphics, Font font, double mapCenterX, double mapCenterZ,
                                     Slice slice, float widgetWorldLen, Player player, boolean rotateWithPlayer,
                                     IMapCollection collection) {
        float yRot = rotateWithPlayer ? player.getYRot() : 180;
        Vec3 playerPos = rotateWithPlayer
                ? player.position()
                : new Vec3(mapCenterX, player.getY(), mapCenterZ);
        for (MapDataHolder holder : collection.selectSection(slice)) {
            for (var decorationHolder : MoonlightCompat.getCustomDecorations(holder)) {
                if (!(decorationHolder.deco() instanceof InternalPinDecoration pin) ||
                        !ClientMarkers.isClientDecoFocused(holder, pin)) {
                    continue;
                }

                double scale = 1 << holder.data.scale;
                double worldX = holder.data.centerX + pin.decoration().x() * scale / 2.0D;
                double worldZ = holder.data.centerZ + pin.decoration().y() * scale / 2.0D;
                Vec3 dist = playerPos.subtract(worldX, playerPos.y, worldZ);
                if (isOffscreen(widgetWorldLen, yRot, dist)) {
                    continue;
                }

                double angle = Mth.RAD_TO_DEG * Math.atan2(dist.x, dist.z) + yRot;
                var pos = MapAtlasesHUD.getDirectionPos(29F, (float) angle);
                graphics.nextStratum();
                graphics.blit(RenderPipelines.GUI_TEXTURED, ClientMarkers.getPinTexture(pin.index(), true),
                        Math.round(pos.getFirst() - 4), Math.round(pos.getSecond() - 4),
                        0, 0, 8, 8, 8, 8);
            }
        }
    }

    private static boolean isOffscreen(float maxSize, float playerYRot, Vec3 dist) {
        Vec3 rotated = dist.yRot(playerYRot * Mth.DEG_TO_RAD);
        float limit = maxSize / 2 + 5;
        return rotated.z <= limit && rotated.z >= -limit && rotated.x <= limit && rotated.x >= -limit;
    }
}
