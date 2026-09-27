package pepjebs.mapatlases.integration.moonlight;

import net.mehvahdjukaar.moonlight.api.map.MapDataRegistry;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecorationType;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapMarker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;
import pepjebs.mapatlases.client.AtlasGuiUtils;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.utils.MapType;
import pepjebs.mapatlases.utils.Slice;

import java.util.WeakHashMap;

public class ClientMarkersRenderer {

    private static final WeakHashMap<MLMapDecorationType<?, ?>, Identifier> SMALL_PINS = new WeakHashMap<>();

    public static void renderDecorationPreview(GuiGraphicsExtractor pGuiGraphics, float x, float y, int index, boolean outline, int alpha) {
        CustomDecorationButton.renderStaticMarker(pGuiGraphics, ClientMarkers.getPinWithIndex(index), x, y, outline, alpha);
    }

    public static void drawSmallPins(GuiGraphicsExtractor graphics, Font font, double mapCenterX, double mapCenterZ, Slice slice,
                                     float widgetWorldLen, Player player, boolean rotateWithPlayer, MapCollection collection) {

        if (slice.type() != MapType.VANILLA) return;

        Registry<MLMapDecorationType<?, ?>> reg = MapDataRegistry.getMapDecorationRegistry(player.level().registryAccess());
        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.MAP_DECORATIONS);
        Matrix3x2fStack pose = graphics.pose();
        float yRot = rotateWithPlayer ? player.getYRot() : 180;
        BlockPos playerPos = rotateWithPlayer ? player.blockPosition() : BlockPos.containing(mapCenterX, 0, mapCenterZ);
        for (var entry : ClientMarkers.MARKERS_PER_MAP.entrySet()) {
            MapId mapId = entry.getKey();
            if (!collection.hasMap(mapId, MapType.VANILLA)) continue;
            var pins = entry.getValue();
            for (var clientMarker : pins) {
                MLMapMarker<?> marker = clientMarker.marker();
                BlockPos pos = marker.getPos();
                Vec3 dist = playerPos.getCenter().subtract(pos.getCenter());
                if (marker instanceof PinMarker mp && mp.isFocused() && !isOffscreen(widgetWorldLen, yRot, dist)) {
                    pose.pushMatrix();
                    double angle = Mth.RAD_TO_DEG * (Math.atan2(dist.x, dist.z)) + yRot;
                    var pp = AtlasGuiUtils.getDirectionPos(29F, (float) angle);
                    float a = pp.getFirst();
                    float b = pp.getSecond();

                    pose.translate(a, b);
                    pose.scale(4);
                    pose.translate(-0.25f, -0.25f);

                    Identifier texture = SMALL_PINS.computeIfAbsent(marker.getType().value(), t ->
                            reg.getKey(t).withPath(k -> k + "_small"));
                    TextureAtlasSprite sprite = atlas.getSprite(texture);
                    AtlasGuiUtils.drawMarkerSprite(graphics, sprite, -1, false);
                    pose.popMatrix();
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
