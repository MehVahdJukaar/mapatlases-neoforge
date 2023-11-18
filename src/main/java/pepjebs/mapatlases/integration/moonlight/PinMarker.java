package pepjebs.mapatlases.integration.moonlight;

import net.mehvahdjukaar.moonlight.api.map.markers.MapBlockMarker;
import net.mehvahdjukaar.moonlight.api.map.type.MapDecorationType;
import net.minecraft.nbt.CompoundTag;

public class PinMarker extends MapBlockMarker<PinDecoration> {

    private boolean focused;

    protected PinMarker(MapDecorationType<PinDecoration, ?> type) {
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