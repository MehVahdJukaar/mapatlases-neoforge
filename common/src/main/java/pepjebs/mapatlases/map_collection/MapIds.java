package pepjebs.mapatlases.map_collection;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.MapType;

import java.util.*;

//component stored in the item
public class MapIds {

    public static final Codec<MapIds> CODEC = Codec.simpleMap(
            MapType.CODEC, MapId.CODEC.listOf(), StringRepresentable.keys(MapType.values())
    ).xmap(MapIds::new, m -> m.ids).codec();

    public static final StreamCodec<ByteBuf, MapIds> STREAM_CODEC = ByteBufCodecs.map(
                    i -> new EnumMap<>(MapType.class), MapType.STREAM_CODEC, MapId.STREAM_CODEC.apply(ByteBufCodecs.list()))
            .map(MapIds::new, m -> m.ids);

    public static final MapIds EMPTY = new MapIds(Map.of());

    private final EnumMap<MapType, List<MapId>> ids = new EnumMap<>(MapType.class);
    private final int count;
    @Nullable
    private MapCollection resolved;

    private MapIds(Map<MapType, List<MapId>> ids) {
        int c = 0;
        for (var e : ids.entrySet()) {
            List<MapId> idList = e.getValue();
            if (idList.isEmpty()) continue;
            this.ids.put(e.getKey(), List.copyOf(idList));
            c += idList.size();
        }
        this.count = c;
    }

    // client data can arrive late, thats why this is weird
    public MapCollection resolve(Level level) {
        if (resolved == null){
            resolved = new MapCollection(this, level);
        }
        return resolved;
    }

    public int getCount() {
        return count;
    }

    public boolean contains(MapType type, MapId id) {
        List<MapId> l = ids.get(type);
        return l != null && l.contains(id);
    }

    public Map<MapType, List<MapId>> getAll() {
        return Collections.unmodifiableMap(ids);
    }

    public MapIds plus(Map<MapType, ? extends Collection<MapId>> toAdd) {
        Map<MapType, List<MapId>> copy = copyIds();
        for (var e : toAdd.entrySet()) {
            copy.computeIfAbsent(e.getKey(), t -> new ArrayList<>()).addAll(e.getValue());
        }
        return new MapIds(copy);
    }

    public MapIds minus(Collection<MapDataHolder> holders) {
        Map<MapType, List<MapId>> copy = copyIds();
        for (MapDataHolder h : holders) {
            List<MapId> l = copy.get(h.type);
            if (l != null) l.remove(h.id);
        }
        return new MapIds(copy);
    }

    private Map<MapType, List<MapId>> copyIds() {
        Map<MapType, List<MapId>> copy = new EnumMap<>(MapType.class);
        ids.forEach((t, l) -> copy.put(t, new ArrayList<>(l)));
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MapIds mapIds)) return false;
        return Objects.equals(ids, mapIds.ids);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ids);
    }
}
