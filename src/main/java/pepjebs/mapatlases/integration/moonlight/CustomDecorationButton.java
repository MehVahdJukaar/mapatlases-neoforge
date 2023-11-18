package pepjebs.mapatlases.integration.moonlight;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.moonlight.api.map.CustomMapDecoration;
import net.mehvahdjukaar.moonlight.api.map.ExpandedMapData;
import net.mehvahdjukaar.moonlight.api.map.client.DecorationRenderer;
import net.mehvahdjukaar.moonlight.api.map.client.MapDecorationClientManager;
import net.mehvahdjukaar.moonlight.api.map.type.MapDecorationType;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.mehvahdjukaar.moonlight.core.MoonlightClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.client.screen.AtlasOverviewScreen;
import pepjebs.mapatlases.client.screen.DecorationBookmarkButton;
import pepjebs.mapatlases.networking.C2SRemoveMarkerPacket;
import pepjebs.mapatlases.networking.MapAtlasesNetworking;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.util.Locale;
import java.util.Map;


public class CustomDecorationButton extends DecorationBookmarkButton {

    public static DecorationBookmarkButton create(int px, int py, AtlasOverviewScreen screen, MapDataHolder data, Object mapDecoration, String id) {
        return new CustomDecorationButton(px, py, screen, data, (CustomMapDecoration) mapDecoration, id);
    }

    private final CustomMapDecoration decoration; // could not match whats in maps

    private CustomDecorationButton(int px, int py, AtlasOverviewScreen screen,
                                   MapDataHolder data, CustomMapDecoration mapDecoration, String id) {
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
                AtlasOverviewScreen.getReadableName(Utils.getID(decoration.getType()).getPath()
                        .toLowerCase(Locale.ROOT)))
                : displayName;
    }

    @Override
    protected void renderDecoration(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        renderStaticMarker(pGuiGraphics, decoration.getType(), mapData.data, getX() + width / 2f, getY() + height / 2f,
                2, decoration instanceof PinDecoration p && p.isFocused());
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
        return pButton == 0 || (pButton == 1 && canFocusMarker());
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
        Map<String, CustomMapDecoration> decorations = ((ExpandedMapData) mapData.data).getCustomDecorations();
        for (var d : decorations.entrySet()) {
            String targetKey = d.getKey();
            if (targetKey.equals(decorationId)) {
                MapAtlasesNetworking.sendToServer(new C2SRemoveMarkerPacket(mapData.stringId, d.getValue().hashCode()));
                decorations.remove(d.getKey());
                ClientMarkers.removeDeco(mapData.stringId, d.getKey());
                return;
            }
        }
        int aa = 1;
    }

    public static void renderStaticMarker(GuiGraphics pGuiGraphics,
                                          MapDecorationType<?, ?> type,
                                          MapItemSavedData data, float x, float y,
                                          int index, boolean outline) {
        DecorationRenderer<?> renderer = MapDecorationClientManager.getRenderer(type);

        if (renderer != null) {
            PoseStack poseStack = pGuiGraphics.pose();

            poseStack.pushPose();
            poseStack.translate(x, y, 0.005);
            poseStack.scale(4, 4, -3);

            var buffer = pGuiGraphics.bufferSource();

            VertexConsumer vertexBuilder = buffer.getBuffer(MapDecorationClientManager.MAP_MARKERS_RENDER_TYPE);

            renderer.renderDecorationSprite(poseStack,
                    buffer, vertexBuilder, LightTexture.FULL_BRIGHT, index,
                    -1, 255, outline);

            poseStack.popPose();
        }
    }


}
