package pepjebs.mapatlases.map_collection;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.MapType;
import pepjebs.mapatlases.utils.Slice;

import java.util.*;

public class EmptyMaps {

    public static final EmptyMaps EMPTY = new EmptyMaps(Map.of());

    private final Map<MapType, Integer> maps;
    private final int size;
    private final boolean hasNonVanilla;

    private EmptyMaps(Map<MapType, Integer> maps) {
        this.maps = maps;
        this.size = maps.values().stream().mapToInt(Integer::intValue).sum();
        this.hasNonVanilla = maps.keySet().stream().anyMatch(type -> type != MapType.VANILLA);
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

    public void addAndAssigns(ItemStack stack, Slice slice, int amount) {
        addAndAssigns(stack, slice.emptyMapType(), amount);
    }

    public void addAndAssigns(ItemStack stack, MapType type, int amount) {
        addAndAssigns(stack, Map.of(type, amount));
    }

    public void addAndAssigns(ItemStack atlas, Map<MapType, Integer> emptyMapCount) {
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


    public List<Component> getTooltips(int fullSize) {
        int maxMapCount = MapAtlasItem.getMaxMapCount();
        if (maxMapCount != -1 && fullSize + size >= maxMapCount) {
            return List.of(Component.translatable("item.map_atlases.atlas.tooltip_full", "", null)
                    .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        }

        if (fullSize + size == 0) {
            int pity = MapAtlasesConfig.pityActivationMapCount.get();
            boolean usesEmptyMaps = MapAtlasesConfig.requireEmptyMapsToExpand.get() && MapAtlasesConfig.enableEmptyMapEntryAndFill.get();
            // If there are no maps & no empty maps, the atlas is "inactive", so display how many empty maps
            // they *would* receive if they activated the atlas
            if (usesEmptyMaps && pity > 0) {
                return List.of(Component.translatable("item.map_atlases.atlas.tooltip_empty", pity).withStyle(ChatFormatting.GRAY));
            }
            return List.of();
        }

        List<Component> tooltips = new ArrayList<>();
        for (var entry : maps.entrySet()) {
            MapType type = entry.getKey();
            int empties = entry.getValue();
            if (hasNonVanilla) {
                tooltips.add(Component.translatable("item.map_atlases.atlas.tooltip_empty_type", type.getName(), empties).withStyle(ChatFormatting.GRAY));
            } else {
                tooltips.add(Component.translatable("item.map_atlases.atlas.tooltip_empty", empties).withStyle(ChatFormatting.GRAY));
            }
        }
        return tooltips;
    }

    public Map<MapType, Integer> getAll() {
        return Collections.unmodifiableMap(maps);
    }

}
