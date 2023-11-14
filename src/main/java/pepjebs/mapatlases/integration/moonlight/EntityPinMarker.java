package pepjebs.mapatlases.integration.moonlight;

import net.mehvahdjukaar.moonlight.api.map.markers.MapBlockMarker;
import net.mehvahdjukaar.moonlight.api.map.type.MapDecorationType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;

public class EntityPinMarker extends MapBlockMarker<EntityPinDecoration> {

    private Entity entity;

    protected EntityPinMarker(MapDecorationType<EntityPinDecoration, ?> type) {
        super(type);
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    @Override
    public EntityPinDecoration doCreateDecoration(byte mapX, byte mapY, byte rot) {
        return new EntityPinDecoration(this.getType(), mapX, mapY, entity);
    }

    @Override
    public CompoundTag saveToNBT(CompoundTag compound) {
        return compound;
    }

    @Override
    public void loadFromNBT(CompoundTag compound) {
    }
}