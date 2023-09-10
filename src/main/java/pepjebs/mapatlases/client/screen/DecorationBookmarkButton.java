package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.Locale;

public class DecorationBookmarkButton extends BookmarkButton {

    private final MapDecoration mapIcon;

    protected DecorationBookmarkButton(int pX, int pY, MapDecoration mapIcon) {
        super(pX, pY, false);
        this.mapIcon = mapIcon;

        Component mapIconComponent = mapIcon.getName() == null
                ? Component.literal(
                MapAtlasesAtlasOverviewScreen.getReadableName(mapIcon.getType().name().toLowerCase(Locale.ROOT)))
                : mapIcon.getName();

        // draw text
        MutableComponent coordsComponent = Component.literal("X: " + mapIcon.getX() + ", Z: " + mapIcon.getY());
        MutableComponent formattedCoords = coordsComponent.setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY));
        this.setTooltip(Tooltip.create(mapIconComponent));
    }

    @Nullable
    @Override
    public Tooltip getTooltip() {
        return super.getTooltip();
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        int i = 1;
        PoseStack matrices = pGuiGraphics.pose();
        // Draw map Icon
        matrices.pushPose();

        matrices.mulPose(Axis.ZP.rotationDegrees((mapIcon.getRot() * 360) / 16.0F));
        matrices.scale(4, 4, 1);
        matrices.translate(-0.125D, 0.125D, -1.0D);
        byte b = mapIcon.getImage();
        float g = (b % 16 + 0) / 16.0F;
        float h = (b / 16 + 0) / 16.0F;
        float l = (b % 16 + 1) / 16.0F;
        float m = (b / 16 + 1) / 16.0F;
        Matrix4f matrix4f2 = matrices.last().pose();
        int light = 0xF000F0;
        MultiBufferSource.BufferSource vcp = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        VertexConsumer vertexConsumer2 = vcp.getBuffer(MapAtlasesAtlasOverviewScreen. MAP_ICONS);
        vertexConsumer2.vertex(matrix4f2, -1.0F, 1.0F, i * 0.001F)
                .color(255, 255, 255, 255).uv(g, h).uv2(light).endVertex();
        vertexConsumer2.vertex(matrix4f2, 1.0F, 1.0F, i * 0.002F)
                .color(255, 255, 255, 255).uv(l, h).uv2(light).endVertex();
        vertexConsumer2.vertex(matrix4f2, 1.0F, -1.0F, i * 0.003F)
                .color(255, 255, 255, 255).uv(l, m).uv2(light).endVertex();
        vertexConsumer2.vertex(matrix4f2, -1.0F, -1.0F, i * 0.004F)
                .color(255, 255, 255, 255).uv(g, m).uv2(light).endVertex();
        vcp.endBatch();
        matrices.popPose();
    }
}
