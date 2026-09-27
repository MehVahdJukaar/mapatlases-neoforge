package pepjebs.mapatlases.client;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4d;
import org.joml.Vector4d;

public final class AtlasGuiUtils {

    public static Vector4d scaleVector(double mouseX, double mouseY, float scale, int w, int h) {
        Matrix4d matrix4d = new Matrix4d();
        double translateX = w / 2.0;
        double translateY = h / 2.0;
        matrix4d.translate(translateX, translateY, 0);
        matrix4d.scale(scale);
        matrix4d.translate(-translateX, -translateY, 0);
        Vector4d v = new Vector4d(mouseX, mouseY, 0, 1.0F);
        matrix4d.transform(v);
        return v;
    }

    public static void drawScaledText(
            GuiGraphicsExtractor context, Font font, int x, int y,
            String text, float textScaling, int maxTextWidth, int centeredAreaWidth) {
        Matrix3x2fStack pose = context.pose();
        float textWidth = font.width(text);
        float scale = Math.min(1, maxTextWidth * textScaling / textWidth) * textScaling;
        float centerX = x + centeredAreaWidth / 2f;
        pose.pushMatrix();
        pose.translate(centerX, y + 4);
        pose.scale(scale);
        pose.translate(-textWidth / 2f, -4);
        drawStringWithLighterShadow(context, font, text, 0, 0);
        pose.popMatrix();
    }

    public static void drawStringWithLighterShadow(GuiGraphicsExtractor context, Font font, String text, float x, float y) {
        Matrix3x2fStack pose = context.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        context.text(font, text, 1, 1, 0xFF595959, false);
        context.text(font, text, 0, 0, 0xFFE0E0E0, false);
        pose.popMatrix();
    }

    public static void drawMarkerSprite(GuiGraphicsExtractor graphics, TextureAtlasSprite sprite, int color, boolean outline) {
        Matrix3x2fStack pose = graphics.pose();
        if (outline) {
            //outline
            int outlineColor = ARGB.color(ARGB.alpha(color), 0);
            for (int j = -1; j <= 1; ++j) {
                for (int k = -1; k <= 1; ++k) {
                    if (j == 0 && k == 0) continue;
                    pose.pushMatrix();
                    pose.translate(j * 0.125f, k * 0.125f);
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, -1, -1, 2, 2, outlineColor);
                    pose.popMatrix();
                }
            }
        }
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, -1, -1, 2, 2, color);
    }

    public static Pair<Float, Float> getDirectionPos(float radius, float angleDegrees) {
        angleDegrees = Mth.wrapDegrees(90 - angleDegrees);
        float angleRadians = (float) Math.toRadians(angleDegrees);
        float x, y;
        if (angleDegrees >= -45 && angleDegrees < 45) {
            x = radius;
            y = radius * (float) Math.tan(angleRadians);
        } else if (angleDegrees >= 45 && angleDegrees < 135) {
            x = radius / (float) Math.tan(angleRadians);
            y = radius;
        } else if (angleDegrees >= 135 || angleDegrees < -135) {
            x = -radius;
            y = -radius * (float) Math.tan(angleRadians);
        } else {
            x = -radius / (float) Math.tan(angleRadians);
            y = -radius;
        }
        return Pair.of(x, y);
    }
}
