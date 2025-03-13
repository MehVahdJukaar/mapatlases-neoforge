package pepjebs.mapatlases.integration.moonlight;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.moonlight.api.map.ExpandedMapData;
import net.mehvahdjukaar.moonlight.api.map.client.MapDecorationClientManager;
import net.mehvahdjukaar.moonlight.api.map.client.MapDecorationRenderer;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecoration;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecorationType;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import pepjebs.mapatlases.client.screen.AtlasOverviewScreen;
import pepjebs.mapatlases.client.screen.DecorationBookmarkButton;
import pepjebs.mapatlases.networking.C2SRemoveMarkerPacket;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.util.Locale;


public class CustomDecorationButton extends DecorationBookmarkButton {

    public static DecorationBookmarkButton create(int px, int py, AtlasOverviewScreen screen, MapDataHolder data, Object mapDecoration, String id) {
        return new CustomDecorationButton(px, py, screen, data, (MLMapDecoration) mapDecoration, id);
    }

    private final MLMapDecoration decoration; // could not match whats in maps

    private CustomDecorationButton(int px, int py, AtlasOverviewScreen screen,
                                   MapDataHolder data, MLMapDecoration mapDecoration, String id) {
        super(px, py, screen, data, id);
        this.decoration = mapDecoration;
        this.setTooltip(createTooltip());
    }

    @Override
    public double getWorldX() {
        return mapData.data.centerX - getDecorationPos(decoration.getX(), mapData.data);
    }

    @Override
    public double getWorldZ() {
        return mapData.data.centerZ - getDecorationPos(decoration.getY(), mapData.data);
    }

    @Override
    public int getBatchGroup() {
        return 1;
    }

    @Override
    public Component getDecorationName() {
        Component displayName = decoration.getDisplayName();
        return displayName == null
                ? Component.literal(
                AtlasOverviewScreen.getReadableName(decoration.getType().unwrapKey().get().location().getPath()
                        .toLowerCase(Locale.ROOT)))
                : displayName;
    }

    @Override
    protected void renderDecoration(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        renderStaticMarker(pGuiGraphics, decoration.getType(), getX() + width / 2f, getY() + height / 2f,
                1, decoration instanceof PinDecoration p && p.isFocused(), 255);
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        if (control || button == 1) {
            focusMarker();
        } else super.onClick(mouseX, mouseY);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (control) {
            focusMarker();
        } else super.onClick(mouseX, mouseY);
    }

    @Override
    protected boolean isValidClickButton(int pButton) {
        return (pButton == 0 && canDeleteMarker()) || (pButton == 1 && canFocusMarker());
    }

    protected void focusMarker() {
        ClientMarkers.focusMarker(mapData, decoration, !ClientMarkers.isDecorationFocused(mapData, decoration));
    }

    @Override
    protected boolean canFocusMarker() {
        return decoration instanceof PinDecoration;
    }

    @Override
    protected void deleteMarker() {
        var decorations = ((ExpandedMapData) mapData.data).ml$getCustomDecorations();
        MLMapDecoration d = decorations.get(decorationId);
        if (d != null) {
            //in case this is is a pin
            if (!ClientMarkers.removeDeco(mapData.id, decorationId)) {
                //we cant use string id because server has them diferent...
                NetworkHelper.sendToServer(new C2SRemoveMarkerPacket(mapData.id, mapData.type, d.hashCode(), true));
                //also removes immediately from client side
            }
            decorations.remove(decorationId);
        }
    }

    public static void renderStaticMarker(GuiGraphics pGuiGraphics,
                                          Holder<MLMapDecorationType<?, ?>> type,
                                          float x, float y,
                                          int index, boolean outline, int alpha) {
        MapDecorationRenderer<?> renderer = MapDecorationClientManager.getRenderer(type);

        if (renderer != null) {
            PoseStack poseStack = pGuiGraphics.pose();

            poseStack.pushPose();
            poseStack.translate(x, y, 0.005);
            poseStack.scale(4, 4, -3);

            var buffer = pGuiGraphics.bufferSource();

            VertexConsumer vertexBuilder = buffer.getBuffer(MapDecorationClientManager.MAP_MARKERS_RENDER_TYPE);

            renderer.renderDecorationSprite(poseStack,
                    buffer, vertexBuilder, LightTexture.FULL_BRIGHT, index,
                    -1, alpha, outline);

            poseStack.popPose();
        }
    }


}
