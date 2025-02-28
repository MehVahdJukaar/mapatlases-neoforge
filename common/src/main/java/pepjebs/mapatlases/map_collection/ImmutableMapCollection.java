package pepjebs.mapatlases.map_collection;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import pepjebs.mapatlases.MapAtlasesMod;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

public class ImmutableMapCollection extends MapCollection {

    public static final Codec<ImmutableMapCollection> CODEC = Codec.INT_STREAM.xmap(
            s -> new ImmutableMapCollection(s.boxed().toList()), c -> c.ids.stream().mapToInt(i -> i)
    );

    public static final StreamCodec<ByteBuf, ImmutableMapCollection> STREAM_CODEC = ByteBufCodecs.VAR_INT
            .apply(ByteBufCodecs.list()).map(ImmutableMapCollection::new, MapCollection::getAllIds);

    private boolean initialized = false;

    protected ImmutableMapCollection(Collection<Integer> integers) {
        this.ids.addAll(integers);
    }

    protected ImmutableMapCollection(Collection<Integer> integers, Level level) {
        this.ids.addAll(integers);
        this.initialize(level);
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    public ImmutableMapCollection removeAndAssigns(ItemStack atlas, Level level, int toRemove) {
        //make id copy
        if (!ids.contains(toRemove)) return this;
        Collection<Integer> newIds = new HashSet<>(ids);
        newIds.remove(toRemove);
        var newColl = new ImmutableMapCollection(newIds, level);
        atlas.set(MapAtlasesMod.MAP_COLLECTION.get(), newColl);
        return newColl;
    }

    public ImmutableMapCollection addAndAssigns(ItemStack atlas, Level level, Collection<Integer> map) {
        if (maps.isEmpty()) return this;
        Collection<Integer> newIds = new HashSet<>(ids);
        var newColl = new ImmutableMapCollection(newIds, level);
        atlas.set(MapAtlasesMod.MAP_COLLECTION.get(), newColl);
        return newColl;
    }

    public ImmutableMapCollection addAndAssigns(ItemStack atlas, Level level, int... maps) {
        if (maps.length == 0) return this;
        Collection<Integer> newIds = new HashSet<>(ids);
        var newColl = new ImmutableMapCollection(newIds, level);
        atlas.set(MapAtlasesMod.MAP_COLLECTION.get(), newColl);
        return newColl;
    }

    public MutableMapCollection toMutable(Level level) {
        return new MutableMapCollection(ids, level);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MapCollection that)) return false;
        return Objects.equals(ids, that.ids);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ids);
    }

    // we need leven context
    public void initialize(Level level) {
        if (!isInitialized()) {

            for (int i : ids) {
                addInternal(i, level);
            }
            initialized = true;
        }
    }

    // if a duplicate exists its likely that its data was not synced yet
    public void updateNotSynced(Level level) {
        notSyncedIds.removeIf(i -> addInternal(i, level));
    }


}
