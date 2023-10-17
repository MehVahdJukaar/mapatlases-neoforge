package pepjebs.mapatlases.integration;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.moonlight.api.map.CustomMapDecoration;
import net.mehvahdjukaar.moonlight.api.map.ExpandedMapData;
import net.mehvahdjukaar.moonlight.api.map.client.DecorationRenderer;
import net.mehvahdjukaar.moonlight.api.map.client.MapDecorationClientManager;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.client.screen.AtlasOverviewScreen;
import pepjebs.mapatlases.client.screen.DecorationBookmarkButton;
import pepjebs.mapatlases.networking.C2SRemoveMarkerPacket;
import pepjebs.mapatlases.networking.MapAtlasesNetworking;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.util.Locale;
import java.util.Map;

public class CustomDecorationButton extends DecorationBookmarkButton {

    public static DecorationBookmarkButton create(int px, int py, AtlasOverviewScreen screen, MapDataHolder data, Object mapDecoration) {
        return new CustomDecorationButton(px, py, screen, data, (CustomMapDecoration) mapDecoration);
    }

    private final CustomMapDecoration decoration;

    private CustomDecorationButton(int px, int py, AtlasOverviewScreen screen, MapDataHolder data, CustomMapDecoration mapDecoration) {
        super(px, py, screen, data);
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
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

        renderStaticMarker(pGuiGraphics, decoration, mapData.data, getX() + width / 2f, getY() + height / 2f, index);
        setSelected(false);

    }


    @Override
    protected void deleteMarker() {
        Map<String, CustomMapDecoration> decorations = ((ExpandedMapData) mapData.data).getCustomDecorations();
        for (var d : decorations.entrySet()) {
            CustomMapDecoration deco = d.getValue();
            if (deco == decoration) {
                MapAtlasesNetworking.sendToServer(new C2SRemoveMarkerPacket(mapData.stringId, deco.hashCode()));
                decorations.remove(d.getKey());
                ClientMarker.removeDeco(mapData.stringId, d.getKey());
                return;
            }
        }
    }

    public static void renderStaticMarker(GuiGraphics pGuiGraphics, CustomMapDecoration decoration,
                                          MapItemSavedData data, float x, float y, int index) {
        DecorationRenderer<CustomMapDecoration> renderer = MapDecorationClientManager.getRenderer(decoration);

        if (renderer != null) {
            PoseStack matrices = pGuiGraphics.pose();

            matrices.pushPose();
            matrices.translate(x, y, 1.0D);

            // de translates by the amount the decoration renderer will translate
            matrices.translate(-(float) decoration.getX() / 2.0F - 64.0F,
                    -(float) decoration.getY() / 2.0F - 64.0F, -0.02F);

            var buffer = pGuiGraphics.bufferSource();

            VertexConsumer vertexBuilder = buffer.getBuffer(MapDecorationClientManager.MAP_MARKERS_RENDER_TYPE);
            renderer.rendersText = false;
            renderer.render(decoration, matrices,
                    vertexBuilder, buffer, data,
                    false, LightTexture.FULL_BRIGHT, index);
            renderer.rendersText = true;

            matrices.popPose();
        }
    }

}
