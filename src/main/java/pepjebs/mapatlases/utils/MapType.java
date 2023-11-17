package pepjebs.mapatlases.utils;

import com.mojang.datafixers.util.Pair;
import net.mehvahdjukaar.moonlight.api.map.CustomMapDecoration;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.SupplementariesCompat;
import pepjebs.mapatlases.integration.TwilightForestCompat;

import java.util.*;
import java.util.stream.Collectors;

public enum MapType {
    VANILLA("map_", Items.FILLED_MAP, Items.MAP),
    MAGIC("magicmap_", tf("filled_magic_map"), tf("magic_map")),
    MAZE("mazemap_", tf("filled_maze_map"), tf("maze_map")),
    ORE_MAZE("mazemap_", tf("filled_ore_map"), tf("ore_map"));

    private static final Map<Item, MapType> FROM_ITEM = Arrays.stream(values())
            .collect(Collectors.toMap(t -> t.filled, c -> c, (existing, replacement) -> existing, IdentityHashMap::new));

    private static final Set<Item> EMPTY = Util.make(() -> {
        var s = new HashSet<Item>();
        for (var v : MapType.values()) {
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

    MapType(String keyPrefix, Item filled, Item empty) {
        this.keyPrefix = keyPrefix;
        this.filled = filled;
        this.empty = empty;
        this.translationKey = filled == null ? "missing" : filled.getDescriptionId();
    }

    public static MapType fromKey(String mapString, MapItemSavedData data) {
        if(MapAtlasesMod.TWILIGHTFOREST && TwilightForestCompat.isMazeOre(data)){
            return ORE_MAZE;
        }
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

    public static MapType fromItem(Item item) {
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

    public ItemStack createNewMapItem(int destX, int destZ, byte scale, Level level, @Nullable Integer height, ItemStack atlas) {
        ItemStack newMap = ItemStack.EMPTY;
        if (this == MapType.VANILLA) {
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
            if(MapAtlasesMod.SUPPLEMENTARIES && SupplementariesCompat.hasAntiqueInk(atlas)){
                SupplementariesCompat.setMapAntique(newMap, level);
            }
        } else if (this == MapType.MAZE && MapAtlasesMod.TWILIGHTFOREST) {
            if (height == null) return ItemStack.EMPTY;
            newMap = TwilightForestCompat.makeMaze(destX, destZ, scale, level, height);
        } else if (this == MapType.ORE_MAZE && MapAtlasesMod.TWILIGHTFOREST) {
            if (height == null) return ItemStack.EMPTY;
            newMap = TwilightForestCompat.makeOre(destX, destZ, scale, level, height);
        } else if (this == MapType.MAGIC && MapAtlasesMod.TWILIGHTFOREST) {
            newMap = TwilightForestCompat.makeMagic(destX, destZ, scale, level);
        }
        return newMap;
    }

    public boolean hasMarkers() {
        return this != MAGIC;
    }

    public int getDiscoveryReach(@Nullable Integer height) {
        return switch (this) {
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
        if (this == MAGIC) return 1 / 3f;
        return 1;
    }


    public Component getName() {
        return Component.translatable(this.translationKey);
    }

}
