package pepjebs.mapatlases.integration.moonlight;

import net.mehvahdjukaar.moonlight.api.map.markers.MapBlockMarker;
import net.mehvahdjukaar.moonlight.api.map.type.MapDecorationType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

import java.lang.ref.WeakReference;

public class EntityPinMarker extends MapBlockMarker<EntityPinDecoration> {

    private WeakReference<Entity> entity;

    protected EntityPinMarker(MapDecorationType<EntityPinDecoration, ?> type) {
        super(type);
    }

    public void setEntity(Entity entity) {
        this.entity = new WeakReference<>(entity);
    }

    @Override
    public boolean shouldRefresh() {
        return false;
    }

    @Override
    public EntityPinDecoration doCreateDecoration(byte mapX, byte mapY, byte rot) {
        var en = entity.get();
        if(en != null) {
            return new EntityPinDecoration(this.getType(), mapX, mapY, en);
        }
        return null;
    }

    @Override
    public CompoundTag saveToNBT() {
        return new CompoundTag();
    }

    @Override
    public void loadFromNBT(CompoundTag compound) {
    }
}