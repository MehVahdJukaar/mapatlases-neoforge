package pepjebs.mapatlases.integration.moonlight;

import net.mehvahdjukaar.moonlight.api.map.CustomMapDecoration;
import net.mehvahdjukaar.moonlight.api.map.type.MapDecorationType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class PinDecoration extends CustomMapDecoration {
    public final PinMarker marker;
    private boolean focused;

    public PinDecoration(PinMarker marker, byte x, byte y, byte rot, @Nullable Component displayName) {
        super(marker.getType(), x, y, rot, displayName);
        this.marker = marker;
    }

    //unused TODO:improve
    public PinDecoration(MapDecorationType<?, ?> type, FriendlyByteBuf buffer) {
        super(type, buffer);
        this.marker = null;
    }

    public boolean isFocused() {
        return focused;
    }

    public void forceFocused(boolean focused) {
        this.focused = focused;
        this.marker.setFocused(focused);
    }
}
