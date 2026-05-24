package pepjebs.mapatlases.utils;

import net.mehvahdjukaar.moonlight.api.map.ExpandedMapData;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecoration;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.client.screen.AtlasScreenUtils;
import pepjebs.mapatlases.integration.moonlight.ClientMarkers;
import pepjebs.mapatlases.integration.moonlight.CustomDecorationButton;
import pepjebs.mapatlases.integration.moonlight.PinDecoration;
import pepjebs.mapatlases.networking.C2SRemoveMarkerPacket;

import java.util.Locale;

import static pepjebs.mapatlases.client.AbstractAtlasDisplay.MAP_DIMENSION;

public final class CustomDecorationHolder extends DecorationHolder {
    private final MLMapDecoration deco;

    CustomDecorationHolder(MLMapDecoration deco, String id, MapDataHolder data) {
        super(id, data, getSortingString(deco));
        this.deco = deco;
    }

    public MLMapDecoration deco() {
        return deco;
    }

    @Override
    public double decorationDistSq(double px, double pz) {
        var d = this.data.data;
        double wx = d.centerX, wz = d.centerZ;
        int scale = 1 << d.scale;
        wx += scale * deco.getX() / 2.0;
        wz += scale * deco.getY() / 2.0;
        return Mth.square(wx - px) + Mth.square(wz - pz);
    }

    @Override
    public double getWorldX() {
        return data.data.centerX - decoPos(deco.getX(), data.data);
    }

    @Override
    public double getWorldZ() {
        return data.data.centerZ - decoPos(deco.getY(), data.data);
    }

    @Override
    public Component getDecorationName() {
        Component displayName = deco.getDisplayName();
        return displayName == null
                ? Component.literal(AtlasScreenUtils.getReadableName(
                        deco.getType().unwrapKey().get().location().getPath().toLowerCase(Locale.ROOT)))
                : displayName;
    }

    @Override
    public void renderDecoration(GuiGraphics graphics, float centerX, float centerY) {
        CustomDecorationButton.renderStaticMarker(graphics, deco.getType(), centerX, centerY,
                1, deco instanceof PinDecoration p && p.isFocused(), 255);
    }

    @Override
    public void deleteMarker() {
        var decorations = ((ExpandedMapData) data.data).ml$getCustomDecorations();
        MLMapDecoration d = decorations.get(id);
        if (d != null) {
            if (!ClientMarkers.removeClientDeco(data.id, id)) {
                NetworkHelper.sendToServer(new C2SRemoveMarkerPacket(data.id, data.type, d.hashCode(), true));
            }
            decorations.remove(id);
        }
    }

    @Override
    public boolean canFocusMarker() {
        return deco instanceof PinDecoration;
    }

    @Override
    public void focusMarker() {
        if (deco instanceof PinDecoration) {
            ClientMarkers.focusClientDeco(data, deco, !ClientMarkers.isClientDecoFocused(data, deco));
        }
    }

    private static double decoPos(int coord, MapItemSavedData mapData) {
        float s = (1 << mapData.scale) * (float) MAP_DIMENSION;
        return (s / 2.0d) - ((s / 2.0d) * ((coord + MAP_DIMENSION) / (float) MAP_DIMENSION));
    }

    private static String getSortingString(MLMapDecoration mm) {
        StringBuilder sb = new StringBuilder();
        sb.append(mm.getType().unwrapKey().get().location().getPath());
        var name = mm.getDisplayName();
        if (name != null) sb.append(" ").append(name.getString());
        return sb.toString();
    }
}