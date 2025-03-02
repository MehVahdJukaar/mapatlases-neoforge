package pepjebs.mapatlases.integration.moonlight;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecorationType;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapMarker;
import net.mehvahdjukaar.moonlight.api.map.decoration.SimpleMapMarker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public class PinMarker extends MLMapMarker<PinDecoration> {

    public static final MapCodec<PinMarker> DIRECT_CODEC = RecordCodecBuilder.mapCodec(i ->
            baseCodecGroup(i).and(Codec.BOOL.fieldOf("focused").forGetter(m -> m.focused)
            ).apply(i, PinMarker::new));

    private boolean focused;

    public PinMarker(Holder<MLMapDecorationType<?, ?>> type, BlockPos pos, float rotation,
                     Optional<Component> component, Optional<Boolean> shouldRefresh,
                     Optional<Boolean> shouldSave, boolean preventsExtending, boolean focused) {
        super(type, pos, rotation, component, shouldRefresh, shouldSave, preventsExtending);
        this.focused = focused;
    }
    public PinMarker(Holder<MLMapDecorationType<?, ?>> type, BlockPos pos, Optional<Component> name, boolean focused) {
        this(type, pos, 0f, name, Optional.empty(), Optional.empty(), false, focused);
    }

    @Override
    public PinDecoration doCreateDecoration(byte mapX, byte mapY, byte rot) {
        var p = new PinDecoration(this, mapX, mapY, rot, this.name);
        p.forceFocused(focused);
        return p;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }
}