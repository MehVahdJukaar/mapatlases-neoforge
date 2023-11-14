package pepjebs.mapatlases.integration.moonlight;

import net.mehvahdjukaar.moonlight.api.map.CustomMapDecoration;
import net.mehvahdjukaar.moonlight.api.map.type.MapDecorationType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class EntityPinDecoration extends CustomMapDecoration {
    private final Entity entity;

    public EntityPinDecoration(MapDecorationType<?, ?> type, byte x, byte y, Entity entity) {
        super(type, x, y, (byte) 0, null);
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    public EntityPinDecoration(MapDecorationType<?, ?> type, FriendlyByteBuf buffer) {
        super(type, buffer);
        this.entity = null;
    }

    @Override
    public byte getX() {
        return super.getX();
    }
}
