package pepjebs.mapatlases.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

import static pepjebs.mapatlases.item.MapAtlasItem.TYPE_NBT;

// this is a pair of map item type + y levels basically
public record Slice(MapType type, Optional<Integer> height, ResourceKey<Level> dimension) {

    public static final Codec<Slice> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MapType.CODEC.fieldOf("type").forGetter(Slice::type),
            Codec.INT.optionalFieldOf("height").forGetter(Slice::height),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Slice::dimension)
    ).apply(instance, Slice::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Slice> STREAM_CODEC = StreamCodec.composite(
            MapType.STREAM_CODEC, Slice::type,
            ByteBufCodecs.optional(ByteBufCodecs.INT), Slice::height,
            ResourceKey.streamCodec(Registries.DIMENSION), Slice::dimension,
            Slice::new
    );

    public static Slice of(MapType type, @Nullable Integer height, ResourceKey<Level> dimension) {
        if (height != null && height.equals(java.lang.Integer.MAX_VALUE)) {
            height = null;
        }
        return new Slice(type, Optional.ofNullable(height), dimension);
    }

    public int heightOrTop() {
        return height.orElse(Integer.MAX_VALUE);
    }

    public boolean hasMarkers() {
        return type.hasMarkers();
    }

    public int getDiscoveryReach() {
        return type.getDiscoveryReach(height);
    }

    public ItemStack createNewMap(int destX, int destZ, byte scale, Level level, ItemStack atlas) {
        return type.createNewMapItem(destX, destZ, scale, level, height, atlas);
    }

    public boolean isSameGroup(Slice slice) {
        return slice.dimension.equals(this.dimension) && slice.type == this.type;
    }
}