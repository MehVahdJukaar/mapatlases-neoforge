package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.joml.Matrix4d;
import org.joml.Vector4d;
import pepjebs.mapatlases.PlatStuff;

public final class AtlasScreenUtils {

    public static Vector4d scaleVector(double mouseX, double mouseZ, float scale, int w, int h) {
        Matrix4d matrix4d = new Matrix4d();
        double translateX = w / 2.0;
        double translateY = h / 2.0;
        matrix4d.translate(translateX, translateY, 0);
        matrix4d.scale(scale);
        matrix4d.translate(-translateX, -translateY, 0);
        Vector4d v = new Vector4d(mouseX, mouseZ, 0, 1.0F);
        matrix4d.transform(v);
        return v;
    }

    public static void drawScaledComponent(
            GuiGraphics context, Font font, int x, int y,
            String text, float textScaling, int maxWidth, int targetWidth) {
        PoseStack pose = context.pose();
        float textWidth = font.width(text);
        float scale = Math.min(1, maxWidth * textScaling / textWidth) * textScaling;
        float centerX = x + targetWidth / 2f;
        pose.pushPose();
        pose.translate(centerX, y + 4, 5);
        pose.scale(scale, scale, 1);
        pose.translate(-textWidth / 2f, -4, 0);
        drawStringWithLighterShadow(context, font, text, 0, 0);
        pose.popPose();
    }

    public static void drawStringWithLighterShadow(GuiGraphics context, Font font, String text, float x, float y) {
        PlatStuff.drawString(context, font, text, x + 1, y + 1, 0x595959, false);
        PlatStuff.drawString(context, font, text, x, y, 0xE0E0E0, false);
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