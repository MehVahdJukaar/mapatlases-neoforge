package pepjebs.mapatlases.integration.moonlight;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.mixin.MapItemSavedDataAccessor;
import pepjebs.mapatlases.utils.DecorationHolder;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MoonlightCompat {
    public static final String INTERNAL_PIN_PREFIX = "pin_";

    public static void init() {
    }

    public static Collection<DecorationHolder> getCustomDecorations(MapDataHolder map) {
        if (!(map.data instanceof MapItemSavedDataAccessor accessor)) {
            return List.of();
        }
        ClientMarkers.addMissingPinsToMap(map, accessor.getDecorations().keySet());
        return accessor.getDecorations().entrySet().stream()
                .filter(entry -> isCustomDecoration(entry.getKey(), entry.getValue()))
                .map(entry -> new DecorationHolder(
                        new InternalPinDecoration(entry.getValue(), entry.getKey(), getPinIndex(entry.getKey())),
                        entry.getKey(),
                        map))
                .toList();
    }

    public static void addDecoration(MapItemSavedData data, BlockPos pos, Identifier id, @Nullable Component name, int index) {
        addDecoration(data, pos, id, name, index, getPinId(pos, name, index));
    }

    static void addDecoration(MapItemSavedData data, BlockPos pos, Identifier id, @Nullable Component name, int index, String pinId) {
        if (data instanceof MapItemSavedDataAccessor accessor) {
            Optional<Holder<MapDecorationType>> type = getInternalDecorationType(id);
            type.ifPresent(holder -> accessor.invokeAddDecoration(holder, null,
                    pinId,
                    pos.getX() + 0.5D, pos.getZ() + 0.5D, 180.0D, name));
            accessor.invokeSetDecorationsDirty();
            data.setDirty();
        }
    }

    static String getPinId(BlockPos pos, @Nullable Component name, int index) {
        return INTERNAL_PIN_PREFIX + ClientMarkers.normalizePinIndex(index) + "_" + pos.asLong() + "_" +
                Integer.toHexString(name == null ? 0 : name.getString().hashCode());
    }

    public static void removeCustomDecoration(MapItemSavedData data, int hash) {
        if (data instanceof MapItemSavedDataAccessor accessor) {
            for (Map.Entry<String, MapDecoration> entry : accessor.getDecorations().entrySet()) {
                if (isCustomDecoration(entry.getKey(), entry.getValue()) && entry.getValue().hashCode() == hash) {
                    accessor.invokeRemoveDecoration(entry.getKey());
                    accessor.invokeSetDecorationsDirty();
                    data.setDirty();
                    return;
                }
            }
        }
    }

    public static boolean maybePlaceMarkerInFront(Player player, ItemStack atlas) {
        return false;
    }

    public static void updateMarkers(MapItemSavedData data, Player player, int maxRange) {
    }

    public static boolean isCustomDecoration(String id, Object decoration) {
        return decoration instanceof MapDecoration && id.startsWith(INTERNAL_PIN_PREFIX);
    }

    public static int getPinIndex(String id) {
        if (!id.startsWith(INTERNAL_PIN_PREFIX)) {
            return 0;
        }
        int start = INTERNAL_PIN_PREFIX.length();
        int end = id.indexOf('_', start);
        if (end <= start) {
            return 0;
        }
        try {
            return Integer.parseInt(id.substring(start, end));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static Optional<Holder<MapDecorationType>> getInternalDecorationType(Identifier id) {
        if ("minecraft".equals(id.getNamespace())) {
            return Optional.ofNullable(switch (id.getPath()) {
                case "target_point" -> MapDecorationTypes.TARGET_POINT;
                case "target_x" -> MapDecorationTypes.TARGET_X;
                case "blue_marker" -> MapDecorationTypes.BLUE_MARKER;
                default -> MapDecorationTypes.RED_MARKER;
            });
        }
        return Optional.of(MapDecorationTypes.RED_MARKER);
    }
}
