package pepjebs.mapatlases.utils;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class SelectedSlice {

    private final Map<ResourceKey<Level>, Slice> map;

    public SelectedSlice(Map<ResourceKey<Level>, Slice> map) {
        this.map = map;
    }

    public static final Codec<SelectedSlice> CODEC = Codec.unboundedMap(
                    ResourceKey.codec(Registries.DIMENSION), Slice.CODEC)
            .xmap(SelectedSlice::new, s -> s.map);

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectedSlice> STREAM_CODEC = ByteBufCodecs.map(
                    SelectedSlice::makeMap, ResourceKey.streamCodec(Registries.DIMENSION), Slice.STREAM_CODEC)
            .map(SelectedSlice::new, s -> s.map);

    private static Map<ResourceKey<Level>, Slice> makeMap(int i) {
        return new HashMap<>(i);
    }

    @Nullable
    public Slice get(ResourceKey<Level> dimension) {
        return this.map.get(dimension);
    }
}
