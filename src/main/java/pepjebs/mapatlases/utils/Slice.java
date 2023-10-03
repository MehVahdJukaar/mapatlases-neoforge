package pepjebs.mapatlases.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ColumnPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static pepjebs.mapatlases.item.MapAtlasItem.HEIGHT_NBT;
import static pepjebs.mapatlases.item.MapAtlasItem.TYPE_NBT;

// this is a pair of map item type + y levels basically
public final class Slice {
    public static final Slice DEFAULT_INSTANCE = new Slice(MapType.VANILLA, null);

    private final MapType type;
    private final @Nullable Integer height;

    private Slice(MapType type, Integer height) {
        this.type = type;
        this.height = height;
    }

    public static Slice of(MapType type, @Nullable Integer height) {
        if (height != null && height.equals(Integer.MAX_VALUE)) {
            height = null;
        }
        if (height == null && type == MapType.VANILLA) return DEFAULT_INSTANCE;
        return new Slice(type, height);
    }

    public static Slice parse(CompoundTag t) {
        int anInt = t.getInt(TYPE_NBT);
        if (anInt >= MapType.values().length) anInt = 0;
        return of(MapType.values()[anInt], t.getInt(HEIGHT_NBT));
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

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Slice) obj;
        return Objects.equals(this.type, that.type) &&
                Objects.equals(this.height, that.height);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, height);
    }

    public int getMapId(String mapKey) {
        return Integer.parseInt(mapKey.substring(this.type.keyPrefix.length()));
    }

    public String getMapString(int id) {
        return type.makeKey(id);
    }


    public boolean hasMarkers() {
        return type.hasMarkers();
    }

}