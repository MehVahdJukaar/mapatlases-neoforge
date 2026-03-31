package pepjebs.mapatlases.integration.moonlight;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.utils.DecorationHolder;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.util.Collection;
import java.util.List;

public class MoonlightCompat {

    public static void init() {
    }

    public static Collection<DecorationHolder> getCustomDecorations(MapDataHolder map) {
        return List.of();
    }

    public static void addDecoration(MapItemSavedData data, BlockPos pos, Identifier id, @Nullable Component name) {
    }

    public static void removeCustomDecoration(MapItemSavedData data, int hash) {
    }

    public static boolean maybePlaceMarkerInFront(Player player, ItemStack atlas) {
        return false;
    }

    public static void updateMarkers(MapItemSavedData data, Player player, int maxRange) {
    }
}
