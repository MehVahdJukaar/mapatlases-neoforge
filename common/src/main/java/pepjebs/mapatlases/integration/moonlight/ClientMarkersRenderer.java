package pepjebs.mapatlases.integration.moonlight;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.moonlight.api.client.util.RenderUtil;
import net.mehvahdjukaar.moonlight.api.map.MapDataRegistry;
import net.mehvahdjukaar.moonlight.api.map.client.MapDecorationClientManager;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecorationType;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapMarker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.phys.Vec3;
import pepjebs.mapatlases.client.ui.MapAtlasesHUD;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.utils.MapType;
import pepjebs.mapatlases.utils.Slice;

import java.util.WeakHashMap;

public class ClientMarkersRenderer {

    private static final WeakHashMap<MLMapDecorationType<?, ?>, ResourceLocation> SMALL_PINS = new WeakHashMap<>();

    public static void renderDecorationPreview(GuiGraphics pGuiGraphics, float x, float y, int index, boolean outline, int alpha) {
        CustomDecorationButton.renderStaticMarker(pGuiGraphics, ClientMarkers.getPinWithIndex(index), x, y, 1, outline, alpha);
    }

    public static void drawSmallPins(GuiGraphics graphics, Font font, double mapCenterX, double mapCenterZ, Slice slice,
                                     float widgetWorldLen, Player player, boolean rotateWithPlayer, MapCollection collection) {

        if (slice.type() != MapType.VANILLA) return;

        Registry<MLMapDecorationType<?, ?>> reg = MapDataRegistry.getMapDecorationRegistry(player.level().registryAccess());
        PoseStack matrixStack = graphics.pose();
        int i = 0;
        VertexConsumer vertexBuilder = graphics.bufferSource().getBuffer(MapDecorationClientManager.MAP_MARKERS_RENDER_TYPE);
        float yRot = rotateWithPlayer ? player.getYRot() : 180;
        BlockPos playerPos = rotateWithPlayer ? player.blockPosition() : BlockPos.containing(mapCenterX, 0, mapCenterZ);
        for (var entry : ClientMarkers.markersPerMap.entrySet()) {
            MapId mapId = entry.getKey();
            if (!collection.hasMap(mapId, MapType.VANILLA)) continue;
            var pins = entry.getValue();
            for (var clientMarker : pins) {
                MLMapMarker<?> marker = clientMarker.marker();
                BlockPos pos = marker.getPos();
                Vec3 dist = playerPos.getCenter().subtract(pos.getCenter());
                if (marker instanceof PinMarker mp && mp.isFocused() && !isOffscreen(widgetWorldLen, yRot, dist)) {
                    matrixStack.pushPose();
                    double angle = Mth.RAD_TO_DEG * (Math.atan2(dist.x, dist.z)) + yRot;
                    var pp = MapAtlasesHUD.getDirectionPos(29F, (float) angle);
                    float a = pp.getFirst();
                    float b = pp.getSecond();

                    matrixStack.translate(a, b, 5);
                    matrixStack.scale(4, 4, 0);
                    matrixStack.translate(-0.25, -0.25, 0);

                    ResourceLocation texture = SMALL_PINS.computeIfAbsent(marker.getType().value(), t ->
                            reg.getKey(t).withPath(k -> k + "_small"));
                    TextureAtlasSprite sprite = Minecraft.getInstance().getMapDecorationTextures().getSprite(texture);
                    RenderUtil.renderSprite(matrixStack, vertexBuilder, LightTexture.FULL_BRIGHT, i++, 255, 255, 255, sprite);
                    matrixStack.popPose();
                }
            }
        }
    }

    private static boolean isOffscreen(float maxSize, float playerYRot, Vec3 dist) {
        var c = dist.yRot(playerYRot * Mth.DEG_TO_RAD);
        float l = maxSize / 2 + 5;
        return (c.z <= l) && (c.z >= -l) && (c.x <= l) && (c.x >= -l);
    }


}
