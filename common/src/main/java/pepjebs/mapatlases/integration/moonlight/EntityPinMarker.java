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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Optional;

public class EntityPinMarker extends MLMapMarker<EntityPinDecoration> {

    public static final MapCodec<EntityPinMarker> DIRECT_CODEC =RecordCodecBuilder.mapCodec((i) ->
            baseCodecGroup(i).apply(i, EntityPinMarker::new));

    private WeakReference<Entity> entity;

    public EntityPinMarker(Holder<MLMapDecorationType<?, ?>> type, BlockPos pos, float rotation, Optional<Component> component, Optional<Boolean> shouldRefresh, Optional<Boolean> shouldSave, boolean preventsExtending) {
        super(type, pos, rotation, component, shouldRefresh, shouldSave, preventsExtending);
    }

    public EntityPinMarker(Holder<MLMapDecorationType<?, ?>> type, Entity entity) {
        this(type, entity.blockPosition(), 0f, Optional.ofNullable(entity.getCustomName()), Optional.empty(), Optional.empty(), false);
        this.entity = new WeakReference<>(entity);
    }

    @Override
    public boolean shouldRefreshFromWorld() {
        return false;
    }

    @Override
    public boolean shouldSave() {
        return false;
    }

    @Override
    public @Nullable EntityPinDecoration createDecorationFromMarker(MapItemSavedData data) {
        if (this.entity == null) return null;
        return super.createDecorationFromMarker(data);
    }

    @Override
    public EntityPinDecoration doCreateDecoration(byte mapX, byte mapY, byte rot) {
        return new EntityPinDecoration(this.getType(), mapX, mapY, entity.get());
    }

}