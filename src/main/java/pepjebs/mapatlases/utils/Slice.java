package pepjebs.mapatlases.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static pepjebs.mapatlases.item.MapAtlasItem.HEIGHT_NBT;
import static pepjebs.mapatlases.item.MapAtlasItem.TYPE_NBT;

// this is a pair of map item type + y levels basically
public final class Slice {

    private final MapType type;
    private final @Nullable Integer height;
    private final ResourceKey<Level> dimension;

    private Slice(MapType type, Integer height, ResourceKey<Level> dimension) {
        this.type = type;
        this.height = height;
        this.dimension = dimension;
    }
    public static Slice of(MapType type, @Nullable Integer height, ResourceKey<Level> dimension) {
        if (height != null && height.equals(Integer.MAX_VALUE)) {
            height = null;
        }
        return new Slice(type, height, dimension);
    }

    public static Slice parse(CompoundTag t, ResourceKey<Level> dimension) {
        int anInt = t.getInt(TYPE_NBT);
        if (anInt >= MapType.values().length) anInt = 0;
        return of(MapType.values()[anInt], t.getInt(HEIGHT_NBT), dimension);
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt(TYPE_NBT, type.ordinal());
        t.putInt(HEIGHT_NBT, heightOrTop());
        return t;
    }

    @Override
    public String toString() {
        return "Slice{" +
                "type=" + type +
                ", height=" + height +
                '}';
    }

    @NotNull
    public int heightOrTop() {
        return height == null ? Integer.MAX_VALUE : height;
    }

    public MapType type() {
        return type;
    }

    public @Nullable Integer height() {
        return height;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Slice slice = (Slice) o;
        return type == slice.type && Objects.equals(height, slice.height) && Objects.equals(dimension, slice.dimension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, height, dimension);
    }

    public String getMapString(int id) {
        return type.makeKey(id);
    }

    public boolean hasMarkers() {
        return type.hasMarkers();
    }

    public int getDiscoveryReach() {
        return type.getDiscoveryReach(height);
    }

    public ItemStack createNewMap(int destX, int destZ, byte scale, Level level) {
        return type.createNewMapItem(destX, destZ, scale, level, height);
    }
}