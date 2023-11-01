package pepjebs.mapatlases.integration.moonlight;

import net.mehvahdjukaar.moonlight.api.map.CustomMapDecoration;
import net.mehvahdjukaar.moonlight.api.map.type.MapDecorationType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class PinDecoration extends CustomMapDecoration {
    boolean focused;
    public PinDecoration(MapDecorationType<?, ?> type, byte x, byte y, byte rot, @Nullable Component displayName) {
        super(type, x, y, rot, displayName);
    }

    public PinDecoration(MapDecorationType<?, ?> type, FriendlyByteBuf buffer) {
        super(type, buffer);
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }
}
