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

    private final MapDecoration decoration;

    public static DecorationBookmarkButton create(int px, int py, AtlasOverviewScreen screen, MapDataHolder data,
                                                  Object mapDecoration, String id) {
        return new CustomDecorationButton(px, py, screen, data, (MapDecoration) mapDecoration, id);
    }

    private CustomDecorationButton(int px, int py, AtlasOverviewScreen screen, MapDataHolder data, MapDecoration decoration, String id) {
        super(px, py, screen, data, id);
        this.decoration = decoration;
        this.setTooltip(createTooltip());
    }

    @Override
    protected void deleteMarker() {
        MapAtlasesNetworking.CHANNEL.sendToServer(new C2SRemoveMarkerPacket(mapData.stringId, decoration.hashCode(), true));
        var decorations = pepjebs.mapatlases.client.MapAtlasesClient.getMutableDecorations(mapData.data);
        decorations.remove(decorationId);
        pepjebs.mapatlases.client.MapAtlasesClient.markDecorationsDirty(mapData.data);
    }

    @Override
    public double getWorldX() {
        return mapData.data.centerX - getDecorationPos(decoration.x(), mapData.data);
    }

    @Override
    public double getWorldZ() {
        return mapData.data.centerZ - getDecorationPos(decoration.y(), mapData.data);
    }

    @Override
    public Component getDecorationName() {
        return decoration.name().orElse(Component.translatable("message.map_atlases.pin"));
    }

    @Override
    protected void renderDecoration(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.nextStratum();
        graphics.blit(RenderPipelines.GUI_TEXTURED, decoration.getSpriteLocation(),
                getX() + width / 2 - 4, getY() + height / 2 - 4,
                0, 0, 8, 8, 8, 8);
    }

    public static void renderStaticMarker(GuiGraphicsExtractor graphics, Object decorationType, float x, float y,
                                          float scale, boolean outline, int alpha) {
        if (decorationType instanceof MapDecoration decoration) {
            graphics.nextStratum();
            graphics.blit(RenderPipelines.GUI_TEXTURED, decoration.getSpriteLocation(),
                    Math.round(x - 4 * scale), Math.round(y - 4 * scale), 0, 0,
                    Math.round(8 * scale), Math.round(8 * scale), 8, 8);
        }
    }
}
