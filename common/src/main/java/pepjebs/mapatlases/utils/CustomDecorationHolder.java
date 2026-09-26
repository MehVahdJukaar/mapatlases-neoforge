package pepjebs.mapatlases.utils;

import net.mehvahdjukaar.moonlight.api.map.ExpandedMapData;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecoration;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.GuiGraphics;
import pepjebs.mapatlases.integration.moonlight.ClientMarkers;
import pepjebs.mapatlases.integration.moonlight.CustomDecorationButton;
import pepjebs.mapatlases.integration.moonlight.PinDecoration;
import pepjebs.mapatlases.networking.C2SRemoveMarkerPacket;

import java.util.Optional;

public final class CustomDecorationHolder extends DecorationHolder {
    private final MLMapDecoration deco;

    CustomDecorationHolder(MLMapDecoration deco, String id, MapDataHolder data) {
        super(id, data, deco.getType().unwrapKey().get().location(), deco.getDisplayName());
        this.deco = deco;
    }

    public MLMapDecoration deco() {
        return deco;
    }

    @Override
    protected int decoX() {
        return deco.getX();
    }

    @Override
    protected int decoY() {
        return deco.getY();
    }

    @Override
    public void renderDecoration(GuiGraphics graphics, float centerX, float centerY) {
        CustomDecorationButton.renderStaticMarker(graphics, deco.getType(), centerX, centerY,
                1, deco instanceof PinDecoration p && p.isFocused(), 255);
    }

    @Override
    public void deleteMarker(Optional<BlockPos> lecternPos) {
        var decorations = ((ExpandedMapData) data.data).ml$getCustomDecorations();
        MLMapDecoration d = decorations.get(id);
        if (d != null) {
            if (!ClientMarkers.removeClientDeco(data.id, id)) {
                NetworkHelper.sendToServer(new C2SRemoveMarkerPacket(data.id, data.type, d.hashCode(), true, lecternPos));
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
}