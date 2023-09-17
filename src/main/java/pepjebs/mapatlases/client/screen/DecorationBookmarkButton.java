package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.integration.MoonlightCompat;

import java.util.Locale;

import static pepjebs.mapatlases.client.AbstractAtlasWidget.MAP_DIMENSION;

public abstract class DecorationBookmarkButton extends BookmarkButton {

    public static final ResourceLocation MAP_ICON_TEXTURE = new ResourceLocation("textures/map/map_icons.png");

    private static final int BUTTON_H = 14;
    private static final int BUTTON_W = 24;

    protected int index = 0;

    protected DecorationBookmarkButton(int pX, int pY, AtlasOverviewScreen parentScreen) {
        super(pX - BUTTON_W, pY, BUTTON_W, BUTTON_H, 0, AtlasOverviewScreen.IMAGE_HEIGHT + 36, parentScreen);

    }

    public static DecorationBookmarkButton of(int px, int py, Object mapDecoration, AtlasOverviewScreen screen) {
        if (mapDecoration instanceof MapDecoration md) return new Vanilla(px, py, screen, md);
        else {
            return MoonlightCompat.makeCustomButton(px, py, mapDecoration, screen);
        }
    }


    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        this.setSelected(true);
        parentScreen.focusDecoration(this);
    }


    public abstract double getWorldX(MapItemSavedData data);

    public abstract double getWorldZ(MapItemSavedData data);


    protected static double getDecorationPos(int decoX, MapItemSavedData data) {
        float s = (1 << data.scale) * (float) MAP_DIMENSION;
        return  (s / 2.0d) - ((s / 2.0d) * ((decoX + MAP_DIMENSION) / (float) MAP_DIMENSION));
    }

    public  int getBatchGroup() {
        return 0;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public static class Vanilla extends DecorationBookmarkButton {

        private final MapDecoration decoration;

        public Vanilla(int px, int py, AtlasOverviewScreen screen, MapDecoration mapDecoration) {
            super(px, py, screen);
            this.decoration = mapDecoration;
            this.setTooltip(createTooltip());
        }

        @Override
        public double getWorldX(MapItemSavedData data) {
            return data.centerX - getDecorationPos(decoration.getX(), data);
        }

        @Override
        public double getWorldZ(MapItemSavedData data) {
            return data.centerZ - getDecorationPos(decoration.getY(), data);
        }

        @Override
        public Tooltip createTooltip() {
            Component name = decoration.getName();
            Component mapIconComponent = name == null
                    ? Component.literal(
                    AtlasOverviewScreen.getReadableName(decoration.getType().name().toLowerCase(Locale.ROOT)))
                    : name;

            // draw text
            MutableComponent coordsComponent = Component.literal("X: " + decoration.getX() + ", Z: " + decoration.getY());
            MutableComponent formattedCoords = coordsComponent.setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY));
            this.setTooltip(Tooltip.create(mapIconComponent));
            return Tooltip.create(mapIconComponent);
        }

        @Override
        protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            PoseStack matrices = pGuiGraphics.pose();
            matrices.pushPose();
            matrices.translate(0,0,0.01*this.index);
            super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

            byte b = decoration.getImage();

            int u = (b % 16) * 8;
            int v = (b / 16) * 8;

            matrices.translate(getX() + width / 2f, getY() + height / 2f, 1.0D);
            matrices.mulPose(Axis.ZP.rotationDegrees((decoration.getRot() * 360) / 16.0F));
            matrices.scale(-1, -1, 1);

            pGuiGraphics.blit(MAP_ICON_TEXTURE, -4, -4, u, v, 8, 8, 128, 128);

            matrices.popPose();

            //hide waiting to be activated by mapWidget
            setSelected(false);
        }

    }

}
