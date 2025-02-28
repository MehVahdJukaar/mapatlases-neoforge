package pepjebs.mapatlases.integration.moonlight;

import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecorationType;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapMarker;
import net.minecraft.nbt.CompoundTag;

public class PinMarker extends MLMapMarker<PinDecoration> {

    private boolean focused;

    protected PinMarker(MLMapDecorationType<PinDecoration, ?> type) {
        super(type);
    }

    @Override
    public PinDecoration doCreateDecoration(byte mapX, byte mapY, byte rot) {
        var p = new PinDecoration(this, mapX, mapY, rot, this.getName());
        p.forceFocused(focused);
        return p;
    }

    @Override
    public CompoundTag saveToNBT() {
        var tag = super.saveToNBT();
        if (this.focused) tag.putBoolean("Focused", true);

        return tag;
    }

    @Override
    public void loadFromNBT(CompoundTag compound) {
        this.focused = compound.getBoolean("Focused");
        super.loadFromNBT(compound);
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }
}