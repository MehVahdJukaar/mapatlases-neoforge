package pepjebs.mapatlases.integration.moonlight;

import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecorationType;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapMarker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public class PinMarker extends MLMapMarker<PinDecoration> {

    private boolean focused;

    public PinMarker(Holder<MLMapDecorationType<?, ?>> type, BlockPos pos, float rotation, Optional<Component> component,
                     Optional<Boolean> shouldRefresh, Optional<Boolean> shouldSave, boolean preventsExtending,
                     boolean focused) {
        super(type, pos, rotation, component, shouldRefresh, shouldSave, preventsExtending);
        this.focused = focused;
    }


    @Override
    public PinDecoration doCreateDecoration(byte mapX, byte mapY, byte rot) {
        var p = new PinDecoration(this, mapX, mapY, rot, this.getName());
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