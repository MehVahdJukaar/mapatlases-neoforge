package pepjebs.mapatlases.integration;

import com.mojang.datafixers.util.Pair;
import net.mehvahdjukaar.moonlight.api.map.ExpandedMapData;
import net.mehvahdjukaar.moonlight.api.map.MapDataRegistry;
import net.mehvahdjukaar.moonlight.api.map.markers.MapBlockMarker;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.util.Collection;

public class MoonlightCompat {

    public static void init() {
        if(PlatHelper.getPhysicalSide().isClient()) {
            MapDataRegistry.addDynamicClientMarkersEvent(ClientMarker::send);
        }
    }

    public static Collection<Pair<Object, MapDataHolder>> getCustomDecorations(MapDataHolder map) {
        return ((ExpandedMapData) map.data).getCustomDecorations().values().stream()
                .map(a -> Pair.of((Object) a, map)).toList();
    }

    public static void addDecoration(MapItemSavedData data, BlockPos pos, ResourceLocation id, @Nullable Component name) {
        var type = MapDataRegistry.get(id);
        if (type != null) {
            MapBlockMarker<?> defaultMarker = type.createEmptyMarker();
            defaultMarker.setPos(pos);
            defaultMarker.setName(name);
            ((ExpandedMapData) data).addCustomMarker(defaultMarker);
        }
    }

    public static void removeCustomDecoration(MapItemSavedData data, int hash) {
        if (data instanceof ExpandedMapData d) {
            d.getCustomDecorations().entrySet().removeIf(e -> e.getValue().hashCode() == hash);
        }
    }


}
