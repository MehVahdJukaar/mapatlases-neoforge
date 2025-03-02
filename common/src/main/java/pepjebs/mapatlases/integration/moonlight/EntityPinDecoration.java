package pepjebs.mapatlases.integration.moonlight;

import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecoration;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecorationType;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public class EntityPinDecoration extends MLMapDecoration {
    private final Entity entity;

    public static final StreamCodec<RegistryFriendlyByteBuf, EntityPinDecoration> STREAM_CODEC = StreamCodec.composite(
            MLMapDecorationType.STREAM_CODEC, MLMapDecoration::getType,
            ByteBufCodecs.BYTE, MLMapDecoration::getX,
            ByteBufCodecs.BYTE, MLMapDecoration::getY,
            EntityPinDecoration::new);

    public EntityPinDecoration(Holder<MLMapDecorationType<?,?>> mlMapDecorationTypeHolder, byte aByte, byte aByte1 ) {
        this(mlMapDecorationTypeHolder, aByte, aByte1, null);
    }

    public EntityPinDecoration(Holder<MLMapDecorationType<?, ?>> type, byte x, byte y, Entity entity) {
        super(type, x, y, (byte) 0, Optional.empty());
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    public void setX(byte x) {
        this.x = x;
    }

    public void setY(byte y) {
        this.y = y;
    }

    public void setRot(byte rot) {
        this.rot = rot;
    }
}
