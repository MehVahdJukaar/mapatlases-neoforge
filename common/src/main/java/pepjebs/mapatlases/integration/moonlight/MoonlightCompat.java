package pepjebs.mapatlases.integration.moonlight;

import net.mehvahdjukaar.moonlight.api.map.CustomMapDecoration;
import net.mehvahdjukaar.moonlight.api.map.ExpandedMapData;
import net.mehvahdjukaar.moonlight.api.map.MapDataRegistry;
import net.mehvahdjukaar.moonlight.api.map.MapHelper;
import net.mehvahdjukaar.moonlight.api.map.client.MapDecorationClientManager;
import net.mehvahdjukaar.moonlight.api.map.markers.MapBlockMarker;
import net.mehvahdjukaar.moonlight.api.map.type.CustomDecorationType;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.utils.DecorationHolder;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.util.*;

public class MoonlightCompat {

    private static final ResourceLocation PIN_TYPE_ID = MapAtlasesMod.res("pin");
    private static final ResourceLocation PIN_ENTITY_TYPE_ID = MapAtlasesMod.res("entity_pin");

    public static void init() {
        MapDataRegistry.registerCustomType(PIN_TYPE_ID, () -> CustomDecorationType.simple(PinMarker::new, PinDecoration::new));
        MapDataRegistry.registerCustomType(PIN_ENTITY_TYPE_ID, () -> CustomDecorationType.simple(EntityPinMarker::new, EntityPinDecoration::new));

        if (PlatHelper.getPhysicalSide().isClient()) {
            MapDataRegistry.addDynamicClientMarkersEvent(ClientMarkers::send);
            MapDataRegistry.addDynamicClientMarkersEvent(EntityRadar::send);
            MapDecorationClientManager.registerCustomRenderer(PIN_TYPE_ID, PinDecorationRenderer::new);
            MapDecorationClientManager.registerCustomRenderer(PIN_ENTITY_TYPE_ID, EntityPinDecorationRenderer::new);
        }
    }

    public static Collection<DecorationHolder> getCustomDecorations(MapDataHolder map) {
        return ((ExpandedMapData) map.data).getCustomDecorations().entrySet().stream()
                .filter(e -> !e.getValue().getType().getCustomFactoryID().equals(PIN_ENTITY_TYPE_ID))
                .map(a -> new DecorationHolder(a.getValue(), a.getKey(), map)).toList();
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
            String selected = null;
            for (var v : d.getCustomMarkers().entrySet()) {
                CustomMapDecoration decorationFromMarker = v.getValue().createDecorationFromMarker(data);
                if (decorationFromMarker != null && decorationFromMarker.hashCode() == hash) {
                    selected = v.getKey();
                }
            }
            if (selected == null || !d.removeCustomMarker(selected)) {
                MapAtlasesMod.LOGGER.warn("Tried to delete custom marker but none was found");
            }
        }
    }


    public static boolean maybePlacePinInFront(Player player, ItemStack atlas) {
        var hit = Utils.rayTrace(player, player.level(), ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY);
        if (hit instanceof BlockHitResult bi && hit.getType() == HitResult.Type.BLOCK) {
            return MapHelper.toggleMarkersAtPos(player.level(), bi.getBlockPos(), atlas, player);
        }
        return false;
    }

    public static void updateMarkers(MapItemSavedData data, Player player, int maxRange) {

        ExpandedMapData d = ((ExpandedMapData) data);
        Map<String, MapBlockMarker<?>> markers = new HashMap<>(d.getCustomMarkers());
        if (!markers.isEmpty()) {
            markers.entrySet().removeIf(m -> !m.getValue().shouldRefresh());
            List<String> toRemove = new ArrayList<>();
            List<MapBlockMarker<?>> toAdd = new ArrayList<>();
            Level level = player.level();
            for (var m : markers.entrySet()) {
                var marker = m.getValue();
                BlockPos pos = marker.getPos();
                if (pos.distToCenterSqr(player.position()) < (maxRange * maxRange)) {
                    if (level.isLoaded(pos)) {
                        MapBlockMarker<?> newMarker = marker.getType().getWorldMarkerFromWorld(level, marker.getPos());
                        String id = m.getKey();
                        if (newMarker == null) {
                            toRemove.add(id);
                        } else if (!Objects.equals(marker, newMarker)) {
                            toRemove.add(id);
                            toAdd.add(newMarker);
                        }
                    }
                }
            }
            toRemove.forEach(d::removeCustomMarker);
            toAdd.forEach(d::addCustomMarker);
        }
    }
}
