package pepjebs.mapatlases.integration.moonlight;

import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecoration;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecorationType;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public class EntityPinDecoration extends MLMapDecoration {
    private final Entity entity;

    public EntityPinDecoration(Holder<MLMapDecorationType<?, ?>> type, byte x, byte y, Entity entity) {
        super(type, x, y, (byte) 0, Optional.empty());
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    public EntityPinDecoration(Holder<MLMapDecorationType<?, ?>> type, FriendlyByteBuf buffer) {
        super(type, buffer);
        this.entity = null;
    }

    @Override
    public byte getX() {
        return super.getX();
    }
}
