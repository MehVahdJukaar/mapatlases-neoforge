package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.Locale;

public class DecorationBookmarkButton extends BookmarkButton {

    public static final ResourceLocation MAP_ICON_TEXTURE = new ResourceLocation("textures/map/map_icons.png");

    private final MapDecoration mapIcon;
    private final MapAtlasesAtlasOverviewScreen parentScreen;

    protected DecorationBookmarkButton(int pX, int pY, MapDecoration mapIcon, MapAtlasesAtlasOverviewScreen parentScreen) {
        super(pX, pY, false);
        this.mapIcon = mapIcon;
        this.parentScreen = parentScreen;

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
        PoseStack matrices = pGuiGraphics.pose();
        matrices.pushPose();
        byte b = mapIcon.getImage();

        int u = (b % 16) * 8;
        int v = (b / 16) * 8;

        matrices.translate(getX() + width/2f  , getY() + height/2f, 1.0D);
        matrices.mulPose(Axis.ZP.rotationDegrees((mapIcon.getRot() * 360) / 16.0F));
        matrices.scale(-1, -1, 1);

        pGuiGraphics.blit(MAP_ICON_TEXTURE, -4,-4, u, v, 8, 8, 128, 128);

        matrices.popPose();

        //hide waiting to be activated by mapWidget
        setSelected(false);
    }

    public MapDecoration getDecoration() {
        return mapIcon;
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
       this.setSelected(true);
        parentScreen.focusDecoration(this);
    }
}
