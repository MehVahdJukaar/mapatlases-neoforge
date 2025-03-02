package pepjebs.mapatlases.integration.moonlight;

import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecoration;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecorationType;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PinDecoration extends MLMapDecoration {
    public static final StreamCodec<RegistryFriendlyByteBuf, PinDecoration> STREAM_CODEC = StreamCodec.composite(
            MLMapDecorationType.STREAM_CODEC, MLMapDecoration::getType,
            ByteBufCodecs.BYTE, MLMapDecoration::getX,
            ByteBufCodecs.BYTE, MLMapDecoration::getY,
            ByteBufCodecs.BYTE, MLMapDecoration::getRot,
            ComponentSerialization.OPTIONAL_STREAM_CODEC, (m) -> Optional.ofNullable(m.getDisplayName()),
            ByteBufCodecs.BOOL, PinDecoration::isFocused,
            PinDecoration::new);

    @Nullable
    public final PinMarker marker;
    private boolean focused;

    public PinDecoration(PinMarker marker, byte x, byte y, byte rot, Optional<Component> displayName) {
        super(marker.getType(), x, y, rot, displayName);
        this.marker = marker;
    }

    //client factory i guess. Probably never called
    public PinDecoration(Holder<MLMapDecorationType<?,?>> type, byte x, byte y, byte rot, Optional<Component> displayName, boolean focused) {
        super(type, x, y, rot, displayName);
        this.focused = focused;
        this.marker = null;
    }

    public boolean isFocused() {
        return focused;
    }

    public void forceFocused(boolean focused) {
        this.focused = focused;
        if (this.marker != null) this.marker.setFocused(focused);
    }
}
