package pepjebs.mapatlases.integration.moonlight;

import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecoration;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecorationType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PinDecoration extends MLMapDecoration {
    public final PinMarker marker;
    private boolean focused;

    public PinDecoration(PinMarker marker, byte x, byte y, byte rot, Optional<Component> displayName) {
        super(marker.getType(), x, y, rot, displayName);
        this.marker = marker;
    }

    public boolean isFocused() {
        return focused;
    }

    public void forceFocused(boolean focused) {
        this.focused = focused;
        this.marker.setFocused(focused);
    }
}
