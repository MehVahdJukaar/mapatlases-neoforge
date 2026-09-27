package pepjebs.mapatlases.integration.moonlight;

import net.mehvahdjukaar.moonlight.api.map.client.MapDecorationClientManager;
import net.mehvahdjukaar.moonlight.api.map.client.MapDecorationRenderer;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecorationType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2fStack;
import pepjebs.mapatlases.client.AtlasGuiUtils;

public class CustomDecorationButton {

    public static void renderStaticMarker(GuiGraphicsExtractor graphics,
                                          Holder<MLMapDecorationType<?, ?>> type,
                                          float x, float y, boolean outline, int alpha) {
        MapDecorationRenderer<?> renderer = MapDecorationClientManager.getRenderer(type);
        if (renderer != null) {
            TextureAtlasSprite sprite = MapDecorationClientManager.getSprite(renderer.getDefaultSprite());
            Matrix3x2fStack pose = graphics.pose();
            pose.pushMatrix();
            pose.translate(x, y);
            pose.scale(4);
            AtlasGuiUtils.drawMarkerSprite(graphics, sprite, ARGB.color(alpha, 0xFFFFFF), outline);
            pose.popMatrix();
        }
    }
}
