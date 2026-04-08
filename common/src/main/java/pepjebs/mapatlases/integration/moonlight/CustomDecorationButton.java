package pepjebs.mapatlases.integration.moonlight;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import pepjebs.mapatlases.client.screen.AtlasOverviewScreen;
import pepjebs.mapatlases.client.screen.DecorationBookmarkButton;
import pepjebs.mapatlases.networking.C2SRemoveMarkerPacket;
import pepjebs.mapatlases.networking.MapAtlasesNetworking;
import pepjebs.mapatlases.utils.MapDataHolder;

public class CustomDecorationButton extends DecorationBookmarkButton {

    private final InternalPinDecoration decoration;

    public static DecorationBookmarkButton create(int px, int py, AtlasOverviewScreen screen, MapDataHolder data,
                                                  Object mapDecoration, String id) {
        return new CustomDecorationButton(px, py, screen, data, (InternalPinDecoration) mapDecoration, id);
    }

    private CustomDecorationButton(int px, int py, AtlasOverviewScreen screen, MapDataHolder data, InternalPinDecoration decoration, String id) {
        super(px, py, screen, data, id);
        this.decoration = decoration;
        this.setTooltip(createTooltip());
    }

    @Override
    protected void deleteMarker() {
        MapAtlasesNetworking.CHANNEL.sendToServer(new C2SRemoveMarkerPacket(mapData.stringId, decoration.decoration().hashCode(), true));
        var decorations = pepjebs.mapatlases.client.MapAtlasesClient.getMutableDecorations(mapData.data);
        decorations.remove(decorationId);
        ClientMarkers.removeClientDeco(mapData.id, decorationId);
        pepjebs.mapatlases.client.MapAtlasesClient.markDecorationsDirty(mapData.data);
    }

    @Override
    public double getWorldX() {
        return mapData.data.centerX - getDecorationPos(decoration.decoration().x(), mapData.data);
    }

    @Override
    public double getWorldZ() {
        return mapData.data.centerZ - getDecorationPos(decoration.decoration().y(), mapData.data);
    }

    @Override
    public Component getDecorationName() {
        return decoration.decoration().name().orElse(Component.literal(
                AtlasOverviewScreen.getReadableName(ClientMarkers.getPinTextureName(decoration.index()))));
    }

    @Override
    protected void renderDecoration(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        renderStaticMarker(graphics, decoration, getX() + width / 2f, getY() + height / 2f,
                1, ClientMarkers.isClientDecoFocused(mapData, decoration), 255);
    }

    @Override
    public int getBatchGroup() {
        return 1;
    }

    @Override
    public void onClick(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 1 || control) {
            focusMarker();
            return;
        }
        super.onClick(event, doubleClick);
    }

    @Override
    protected boolean canFocusMarker() {
        return true;
    }

    private void focusMarker() {
        ClientMarkers.focusClientDeco(mapData, decoration, !ClientMarkers.isClientDecoFocused(mapData, decoration));
        this.setTooltip(createTooltip());
    }

    public static void renderStaticMarker(GuiGraphicsExtractor graphics, Object decorationType, float x, float y,
                                          float scale, boolean outline, int alpha) {
        int color = withAlpha(alpha);
        if (decorationType instanceof InternalPinDecoration pin) {
            int size = Math.max(1, Math.round(8 * scale));
            int drawX = Math.round(x - size / 2f);
            int drawY = Math.round(y - size / 2f);
            if (outline) {
                graphics.nextStratum();
                graphics.blit(RenderPipelines.GUI_TEXTURED, ClientMarkers.getPinTexture(pin.index(), false),
                        drawX - 1, drawY - 1, 0, 0, size + 2, size + 2, 8, 8, 8, 8, color);
            }
            graphics.nextStratum();
            graphics.blit(RenderPipelines.GUI_TEXTURED, ClientMarkers.getPinTexture(pin.index(), false),
                    drawX, drawY, 0, 0, size, size, 8, 8, 8, 8, color);
        } else if (decorationType instanceof Integer index) {
            int size = Math.max(1, Math.round(8 * scale));
            graphics.nextStratum();
            graphics.blit(RenderPipelines.GUI_TEXTURED, ClientMarkers.getPinTexture(index, false),
                    Math.round(x - size / 2f), Math.round(y - size / 2f), 0, 0,
                    size, size, 8, 8, 8, 8, color);
        } else if (decorationType instanceof MapDecoration decoration) {
            int size = Math.max(1, Math.round(8 * scale));
            graphics.nextStratum();
            graphics.blit(RenderPipelines.GUI_TEXTURED, pepjebs.mapatlases.client.MapAtlasesClient.getDecorationTexture(decoration),
                    Math.round(x - size / 2f), Math.round(y - size / 2f), 0, 0,
                    size, size, 8, 8, 8, 8, color);
        }
    }

    private static int withAlpha(int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | 0x00FFFFFF;
    }
}
