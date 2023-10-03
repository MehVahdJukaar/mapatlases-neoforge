package pepjebs.mapatlases.utils;

import com.mojang.datafixers.util.Pair;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.SupplementariesCompat;
import pepjebs.mapatlases.integration.TwilightForestCompat;

import java.util.*;
import java.util.stream.Collectors;

import static pepjebs.mapatlases.item.MapAtlasItem.HEIGHT_NBT;
import static pepjebs.mapatlases.item.MapAtlasItem.TYPE_NBT;

// this is a pair of map item type + y level basically
public final class Slice {

    public static final Slice DEFAULT_INSTANCE = new Slice(Type.VANILLA, null);
    private final Type type;
    private final @Nullable Integer height;

    private Slice(Type type, Integer height) {
        this.type = type;
        this.height = height;
    }

    public static Slice of(Type type, @Nullable Integer height) {
        if (height != null && height.equals(Integer.MAX_VALUE)) {
            height = null;
        }
        if (height == null && type == Type.VANILLA) return DEFAULT_INSTANCE;
        return new Slice(type, height);
    }

    public static Slice of(Pair<String, MapItemSavedData> d) {
        Type t = Type.fromKey(d.getFirst());
        return Slice.of(t, t.getHeight(d.getSecond()));
    }

    public static Slice parse(CompoundTag t) {
        int anInt = t.getInt(TYPE_NBT);
        if (anInt >= Type.values().length) anInt = 0;
        return of(Type.values()[anInt], t.getInt(HEIGHT_NBT));
    }

    @Override
    public String toString() {
        return "Slice{" +
                "type=" + type +
                ", height=" + height +
                '}';
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt(TYPE_NBT, type.ordinal());
        t.putInt(HEIGHT_NBT, heightOrTop());
        return t;
    }

    @NotNull
    public int heightOrTop() {
        return height == null ? Integer.MAX_VALUE : height;
    }

    public Item getBlankItem(@Nullable Integer slice) {
        return type.empty;
    }

    public Component getName() {
        return Component.translatable(this.type.translationKey);
    }

    public Type type() {
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

    public ItemStack createNewMap(int destX, int destZ, byte scale, Level level) {
        ItemStack newMap = ItemStack.EMPTY;
        if (this.type == Type.VANILLA) {
            if (height != null && MapAtlasesMod.SUPPLEMENTARIES) {
                newMap = SupplementariesCompat.createSliced(
                        level,
                        destX,
                        destZ,
                        scale,
                        true,
                        false, height);
            } else {
                newMap = MapItem.create(
                        level,
                        destX,
                        destZ,
                        scale,
                        true,
                        false);
            }
        } else if (this.type == Type.MAZE && MapAtlasesMod.TWILIGHTFOREST) {
            if (height == null) return ItemStack.EMPTY;
            newMap = TwilightForestCompat.makeMaze(destX, destZ, scale, level, height);
        } else if (this.type == Type.ORE_MAZE && MapAtlasesMod.TWILIGHTFOREST) {
            if (height == null) return ItemStack.EMPTY;
            newMap = TwilightForestCompat.makeOre(destX, destZ, scale, level, height);
        } else if (this.type == Type.MAGIC && MapAtlasesMod.TWILIGHTFOREST) {
            newMap = TwilightForestCompat.makeMagic(destX, destZ, scale, level);
        }
        return newMap;
    }

    public void updateMap(ServerPlayer player, MapItemSavedData selected) {
        ((MapItem) type.filled).update(player.level(), player, selected);
    }

    public ColumnPos getCenter(double px, double pz, int scale) {
        if (type == Type.MAGIC && MapAtlasesMod.TWILIGHTFOREST) {
            return TwilightForestCompat.getMagicMapCenter((int) px, (int) pz);
        } else {
            //map logic
            int j = Mth.floor((px + 64.0D) / scale);
            int k = Mth.floor((pz + 64.0D) / scale);
            int mapCenterX = j * scale + scale / 2 - 64;
            int mapCenterZ = k * scale + scale / 2 - 64;
            return new ColumnPos(mapCenterX, mapCenterZ);
        }
    }

    public boolean hasMarkers() {
        return this.type != Type.MAGIC;
    }

    public int getDiscoveryReach() {
        return switch (type) {
            case VANILLA -> {
                if (height != null && MapAtlasesMod.SUPPLEMENTARIES) {
                    yield SupplementariesCompat.getSliceReach();
                } else {
                    yield 128; //vanilla
                }
            }
            case MAZE, ORE_MAZE -> 16;
            case MAGIC -> 512;
        };
    }

    public float getDefaultZoomFactor() {
        if (this.type == Type.MAGIC) return 1 / 3f;
        return 1;
    }

    public String getMapString(int id) {
        return type.makeKey(id);
    }


    //item slice
    public enum Type {
        VANILLA("map_", Items.FILLED_MAP, Items.MAP),
        MAGIC("magicmap_", tf("filled_magic_map"), tf("magic_map")),
        MAZE("mazemap_", tf("filled_maze_map"), tf("maze_map")),
        ORE_MAZE("mazemap_", tf("filled_ore_map"), tf("ore_map"));

        private static final Map<Item, Type> FROM_ITEM = Arrays.stream(values())
                .collect(Collectors.toMap(t -> t.filled, c -> c, (existing, replacement) -> existing, IdentityHashMap::new));

        private static final Set<Item> EMPTY = Util.make(() -> {
            var s = new HashSet<Item>();
            for (var v : Type.values()) {
                var t = v.empty;
                if (t != null) s.add(t);
                BuiltInRegistries.ITEM.getOptional(new ResourceLocation("supplementaries:slice_map")).ifPresent(s::add);
            }
            return s;
        });

        private final String keyPrefix;
        public final Item filled;
        public final Item empty;
        public final String translationKey;

        Type(String keyPrefix, Item filled, Item empty) {
            this.keyPrefix = keyPrefix;
            this.filled = filled;
            this.empty = empty;
            this.translationKey = filled == null ? "missing" : filled.getDescriptionId();
        }

        public static Type fromKey(String mapString) {
            for (var t : values()) {
                if (mapString.startsWith(t.keyPrefix)) return t;
            }
            return VANILLA;
        }

        public String makeKey(int id) {
            return keyPrefix + id;
        }

        @Nullable
        public Integer findKey(String s) {
            if (s.startsWith(keyPrefix)) return Integer.parseInt(s.substring(keyPrefix.length()));
            return null;
        }

        public static boolean isEmptyMap(Item i) {
            return EMPTY.contains(i);
        }

        public static Type fromItem(Item item) {
            return FROM_ITEM.get(item);
        }

        private static Item tf(String id) {
            if (MapAtlasesMod.TWILIGHTFOREST) {
                return BuiltInRegistries.ITEM.getOptional(new ResourceLocation("twilightforest", id))
                        .orElse(null);
            }
            return null;
        }

        @Nullable
        public Pair<String, MapItemSavedData> getMapData(Level level, int id) {
            String key = keyPrefix + id;
            MapItemSavedData data = null;
            if (this == VANILLA) {
                data = level.getMapData(key);
            }
            if (this == MAGIC && MapAtlasesMod.TWILIGHTFOREST) {
                data = TwilightForestCompat.getMagic(level, key);
            } else if ((this == MAZE || this == ORE_MAZE) && MapAtlasesMod.TWILIGHTFOREST) {
                data = TwilightForestCompat.getMaze(level, key);
            }
            return data == null ? null : Pair.of(key, data);
        }

        public Integer getHeight(MapItemSavedData data) {
            return switch (this) {
                case VANILLA -> MapAtlasesMod.SUPPLEMENTARIES ? SupplementariesCompat.getSlice(data) : null;
                case MAZE, ORE_MAZE -> MapAtlasesMod.TWILIGHTFOREST ? TwilightForestCompat.getSlice(data) : null;
                case MAGIC -> null;
            };
        }
    }

}