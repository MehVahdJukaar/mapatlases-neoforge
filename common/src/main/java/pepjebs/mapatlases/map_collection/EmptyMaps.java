package pepjebs.mapatlases.map_collection;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.utils.MapType;
import pepjebs.mapatlases.utils.Slice;

import java.util.*;

public class EmptyMaps {

    public static final EmptyMaps EMPTY = new EmptyMaps(Map.of());

    private final Map<MapType, Integer> maps;
    private final int size;

    private EmptyMaps(Map<MapType, Integer> maps) {
        this.maps = maps;
        this.size = maps.values().stream().mapToInt(Integer::intValue).sum();
    }

    public static final Codec<EmptyMaps> CODEC = Codec.simpleMap(
                    MapType.CODEC, Codec.INT, StringRepresentable.keys(MapType.values()))
            .xmap(EmptyMaps::new, s -> s.maps).codec();

    public static final StreamCodec<ByteBuf, EmptyMaps> STREAM_CODEC = ByteBufCodecs
            .map(EmptyMaps::makeMap, MapType.STREAM_CODEC, ByteBufCodecs.VAR_INT)
            .map(EmptyMaps::new, s -> s.maps);

    private static Map<MapType, Integer> makeMap(int i) {
        return new HashMap<>(i);
    }

    public static EmptyMaps of(Map<MapType, Integer> counts) {
        return new EmptyMaps(new HashMap<>(counts));
    }

    public int getTotalCount() {
        return size;
    }

    public int getCount(MapType type) {
        return this.maps.getOrDefault(type, 0);
    }

    public int getCount(Slice slice) {
        return getCount(slice.emptyMapType());
    }

    public void addAndAssign(ItemStack stack, Slice slice, int amount) {
        addAndAssign(stack, slice.emptyMapType(), amount);
    }

    public void addAndAssign(ItemStack stack, MapType type, int amount) {
        addAndAssign(stack, Map.of(type, amount));
    }

    public void addAndAssign(ItemStack atlas, Map<MapType, Integer> emptyMapCount) {
        Map<MapType, Integer> newMap = new HashMap<>(this.maps);
        for (var entry : emptyMapCount.entrySet()) {
            newMap.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        assign(atlas, newMap);
    }

    public void setAndAssign(ItemStack stack, MapType type, int count) {
        Map<MapType, Integer> newMap = new HashMap<>(this.maps);
        newMap.put(type, count);
        assign(stack, newMap);
    }

    private static void assign(ItemStack atlas, Map<MapType, Integer> newMap) {
        newMap.values().removeIf(count -> count <= 0);
        atlas.set(MapAtlasesMod.EMPTY_MAPS.get(), new EmptyMaps(newMap));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmptyMaps that)) return false;
        return Objects.equals(maps, that.maps);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(maps);
    }

    public Map<MapType, Integer> getAll() {
        return Collections.unmodifiableMap(maps);
    }

}
