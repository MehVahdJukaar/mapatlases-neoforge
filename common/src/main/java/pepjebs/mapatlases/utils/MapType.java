package pepjebs.mapatlases.utils;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.SupplementariesCompat;
import pepjebs.mapatlases.integration.TwilightForestCompat;

import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

//mainly for integration puposes
public enum MapType implements StringRepresentable {
    VANILLA("map_", () -> Items.FILLED_MAP, () -> Items.MAP),
    MAGIC("magicmap_", tf("filled_magic_map"), tf("magic_map")),
    MAZE("mazemap_", tf("filled_maze_map"), tf("maze_map")),
    ORE_MAZE("mazemap_", tf("filled_ore_map"), tf("ore_map")),
    SLICED("map_", () -> Items.FILLED_MAP, sup("slice_map")); //just used for empty maps

    public static final Codec<MapType> CODEC = StringRepresentable.fromEnum(MapType::values);
    public static final StreamCodec<ByteBuf, MapType> STREAM_CODEC =
            ByteBufCodecs.idMapper(value -> MapType.values()[value], Enum::ordinal);

    private static final Supplier<Map<Item, MapType>> FILLED = Suppliers.memoize(() -> {
        var map = new IdentityHashMap<Item, MapType>();
        for (var v : values()) {
            Item filled = v.filled.get();
            if (filled != null && !map.containsKey(filled)) map.put(filled, v);
        }
        return map;
    });

    private static final Supplier<Map<Item, MapType>> EMPTY = Suppliers.memoize(() -> {
        var map = new IdentityHashMap<Item, MapType>();
        for (var v : values()) {
            Item filled = v.empty.get();
            if (filled != null) map.put(filled, v);
        }
        BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse("supplementaries:slice_map")).ifPresent(i -> map.put(i, VANILLA));

        return map;
    });

    @NotNull
    private final Supplier<Item> filled;
    @NotNull
    private final Supplier<Item> empty;
    private final String mapIdPrefix;
    private final String id;


    MapType(String keyPrefix, @NotNull Supplier<Item> filled, @NotNull Supplier<Item> empty) {
        this.filled = filled;
        this.empty = empty;
        this.mapIdPrefix = keyPrefix;
        this.id = this.name().toLowerCase(Locale.ROOT);
    }

    public String getMapIdPrefix() {
        return mapIdPrefix;
    }

    public MapItem getFilled() {
        return (MapItem) filled.get();
    }

    public Item getEmpty() {
        return empty.get();
    }

    @Nullable
    public static MapType fromEmptyMap(Item item) {
        return EMPTY.get().get(item);
    }

    @Nullable
    public static MapType fromFilledMap(Item item) {
        return FILLED.get().get(item);
    }

    private static Supplier<Item> tf(String id) {
        return Suppliers.memoize(() -> BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("twilightforest", id))
                .orElse(null));
    }

    private static Supplier<Item> sup(String id) {
        return Suppliers.memoize(() -> BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("supplementaries", id))
                .orElse(null));
    }

    @Nullable
    public MapItemSavedData getMapData(Level level, MapId id) {
        MapItemSavedData data = null;
        if (this == VANILLA || this == SLICED) {
            data = level.getMapData(id);
        }
        if (this == MAGIC && MapAtlasesMod.TWILIGHTFOREST) {
            data = TwilightForestCompat.getMagic(level, id);
        } else if ((this == MAZE || this == ORE_MAZE) && MapAtlasesMod.TWILIGHTFOREST) {
            data = TwilightForestCompat.getMaze(level, id);
        }
        return data;
    }

    public Integer getHeight(@NotNull MapItemSavedData data) {
        return switch (this) {
            case VANILLA, SLICED -> MapAtlasesMod.SUPPLEMENTARIES ? SupplementariesCompat.getSlice(data).orElse(null) : null;
            case MAZE, ORE_MAZE -> MapAtlasesMod.TWILIGHTFOREST ? TwilightForestCompat.getSlice(data) : null;
            case MAGIC -> null;
        };
    }

    public ColumnPos getCenter(double px, double pz, int scale) {
        if (this == MAGIC && MapAtlasesMod.TWILIGHTFOREST) {
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

    public ItemStack createExistingMapItem(MapId id, Optional<Integer> height) {
        ItemStack map = ItemStack.EMPTY;
        if (this == VANILLA) {
            if (height.isPresent() && MapAtlasesMod.SUPPLEMENTARIES) {
                map = SupplementariesCompat.createExistingSliced(id);
            } else {
                map = new ItemStack(Items.FILLED_MAP);
                map.set(DataComponents.MAP_ID, id);
            }
        } else if (this == MAGIC && MapAtlasesMod.TWILIGHTFOREST) {
            map = TwilightForestCompat.makeExistingMagic(id);
        } else if ((this == MAZE) && MapAtlasesMod.TWILIGHTFOREST) {
            map = TwilightForestCompat.makeExistingMaze(id);
        } else if ((this == ORE_MAZE) && MapAtlasesMod.TWILIGHTFOREST) {
            map = TwilightForestCompat.makeExistingOre(id);
        }
        return map;
    }

    public ItemStack createNewMapItem(int destX, int destZ, byte scale, Level level, Optional<Integer> height, ItemStack atlas) {
        ItemStack newMap = ItemStack.EMPTY;
        if (this == MapType.VANILLA) {
            if (height.isPresent() && MapAtlasesMod.SUPPLEMENTARIES) {
                newMap = SupplementariesCompat.createSliced(
                        level,
                        destX,
                        destZ,
                        scale,
                        true,
                        false, height.get());
            } else {
                newMap = MapItem.create(
                        level,
                        destX,
                        destZ,
                        scale,
                        true,
                        false);
            }
            if (MapAtlasesMod.SUPPLEMENTARIES && SupplementariesCompat.hasAntiqueInk(atlas)) {
                SupplementariesCompat.setMapAntique(newMap, level);
            }
        } else if (this == MapType.MAZE && MapAtlasesMod.TWILIGHTFOREST) {
            if (height.isEmpty()) return ItemStack.EMPTY;
            newMap = TwilightForestCompat.makeMaze(destX, destZ, scale, level, height.get());
        } else if (this == MapType.ORE_MAZE && MapAtlasesMod.TWILIGHTFOREST) {
            if (height.isEmpty()) return ItemStack.EMPTY;
            newMap = TwilightForestCompat.makeOre(destX, destZ, scale, level, height.get());
        } else if (this == MapType.MAGIC && MapAtlasesMod.TWILIGHTFOREST) {
            newMap = TwilightForestCompat.makeMagic(destX, destZ, scale, level);
        }
        return newMap;
    }

    public boolean hasMarkers() {
        return this != MAGIC;
    }

    public int getDiscoveryReach(Optional<Integer> height) {
        return switch (this) {
            case VANILLA, SLICED -> {
                if (height.isPresent() && MapAtlasesMod.SUPPLEMENTARIES) {
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
        if (this == MAGIC) return 1 / 3f;
        return 1;
    }


    public Component getName() {
        Item d = filled.get();
        if (d == null) return Component.empty();
        return Component.translatable(d.getDescriptionId());
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public MapId getMapId(ItemStack item) {
        return item.get(DataComponents.MAP_ID);
    }

}
