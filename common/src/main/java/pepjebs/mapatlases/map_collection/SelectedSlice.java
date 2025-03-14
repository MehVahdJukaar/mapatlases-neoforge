package pepjebs.mapatlases.map_collection;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.utils.Slice;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SelectedSlice {

    public static final SelectedSlice EMPTY = new SelectedSlice(Map.of());

    private final Map<ResourceKey<Level>, Slice> map;

    private SelectedSlice(Map<ResourceKey<Level>, Slice> map) {
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

    public void removeAndAssigns(ItemStack stack, ResourceKey<Level> location) {
        //copy map,remove and assign new comp
        Map<ResourceKey<Level>, Slice> newMap = new HashMap<>(this.map);
        newMap.remove(location);
        SelectedSlice newSlice = new SelectedSlice(newMap);
        stack.set(MapAtlasesMod.SELECTED_SLICE.get(), newSlice);
    }

    public void addAndAssigns(ItemStack stack, ResourceKey<Level> location, Slice slice) {
        Map<ResourceKey<Level>, Slice> newMap = new HashMap<>(this.map);
        newMap.put(location, slice);
        SelectedSlice newSlice = new SelectedSlice(newMap);
        stack.set(MapAtlasesMod.SELECTED_SLICE.get(), newSlice);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SelectedSlice that)) return false;
        return Objects.equals(map, that.map);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(map);
    }
}
