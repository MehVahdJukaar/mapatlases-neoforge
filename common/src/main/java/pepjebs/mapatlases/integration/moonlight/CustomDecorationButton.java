package pepjebs.mapatlases.integration.moonlight;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import pepjebs.mapatlases.client.screen.AtlasOverviewScreen;
import pepjebs.mapatlases.client.screen.DecorationBookmarkButton;
import pepjebs.mapatlases.utils.MapDataHolder;

public class CustomDecorationButton extends DecorationBookmarkButton {

    public static DecorationBookmarkButton create(int px, int py, AtlasOverviewScreen screen, MapDataHolder data,
                                                  Object mapDecoration, String id) {
        return new CustomDecorationButton(px, py, screen, data, id);
    }

    private CustomDecorationButton(int px, int py, AtlasOverviewScreen screen, MapDataHolder data, String id) {
        super(px, py, screen, data, id);
        this.setTooltip(createTooltip());
    }

    @Override
    protected void deleteMarker() {
    }

    @Override
    public double getWorldX() {
        return mapData.data.centerX;
    }

    @Override
    public double getWorldZ() {
        return mapData.data.centerZ;
    }

    @Override
    public Component getDecorationName() {
        return Component.empty();
    }

    @Override
    protected void renderDecoration(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    }

    public static void renderStaticMarker(GuiGraphicsExtractor graphics, Object decorationType, float x, float y,
                                          float scale, boolean outline, int alpha) {
    }
}
