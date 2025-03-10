package pepjebs.mapatlases.integration.moonlight;

import net.mehvahdjukaar.moonlight.api.map.ExpandedMapData;
import net.mehvahdjukaar.moonlight.api.map.MapDataRegistry;
import net.mehvahdjukaar.moonlight.api.map.MapHelper;
import net.mehvahdjukaar.moonlight.api.map.client.MapDecorationClientManager;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecoration;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecorationType;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapMarker;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLSpecialMapDecorationType;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
    private static final TagKey<MLMapDecorationType<?, ?>> NOT_ON_ATLAS = TagKey.create(MapDataRegistry.REGISTRY_KEY,
            MapAtlasesMod.res("no_button_on_atlas"));

    private static final ResourceLocation PIN_TYPE_ID = MapAtlasesMod.res("pin");
    private static final ResourceLocation PIN_ENTITY_TYPE_ID = MapAtlasesMod.res("entity_pin");

    public static void init() {
        MapDataRegistry.registerSpecialMapDecorationTypeFactory(PIN_TYPE_ID, () -> MLSpecialMapDecorationType.standaloneCustomMarker(PinMarker.DIRECT_CODEC, PinDecoration.STREAM_CODEC));
        MapDataRegistry.registerSpecialMapDecorationTypeFactory(PIN_ENTITY_TYPE_ID, () -> MLSpecialMapDecorationType.standaloneCustomMarker(EntityPinMarker.DIRECT_CODEC, EntityPinDecoration.STREAM_CODEC));

        if (PlatHelper.getPhysicalSide().isClient()) {
            MapDataRegistry.addDynamicClientMarkersEvent(ClientMarkers::send); //just works with vanilla maps
            MapDataRegistry.addDynamicClientMarkersEvent(EntityRadar::send);
            MapDecorationClientManager.registerCustomRenderer(PIN_TYPE_ID, PinDecorationRenderer::new);
            MapDecorationClientManager.registerCustomRenderer(PIN_ENTITY_TYPE_ID, EntityPinDecorationRenderer::new);
        }
    }

    public static Collection<DecorationHolder> getCustomDecorations(MapDataHolder map) {
        return ((ExpandedMapData) map.data).ml$getCustomDecorations().entrySet().stream()
                .filter(e -> !e.getValue().getType().is(NOT_ON_ATLAS))
                .map(a -> new DecorationHolder(a.getValue(), a.getKey(), map)).toList();
    }

    public static void addDecoration(Level level, MapItemSavedData data, BlockPos pos, ResourceLocation id, @Nullable Component name) {
        var type = MapDataRegistry.getRegistry(level.registryAccess()).getHolder(id);
        if (type.isPresent()) {
            MLMapMarker<?> defaultMarker = new PinMarker(type.get(), pos, Optional.ofNullable(name), false);
            ((ExpandedMapData) data).ml$addCustomMarker(defaultMarker);
        }
    }


    public static void removeCustomDecoration(MapItemSavedData data, int hash) {
        if (data instanceof ExpandedMapData d) {
            String selected = null;
            for (var v : d.ml$getCustomMarkers().entrySet()) {
                MLMapDecoration decorationFromMarker = v.getValue().createDecorationFromMarker(data);
                if (decorationFromMarker != null && decorationFromMarker.hashCode() == hash) {
                    selected = v.getKey();
                }
            }
            if (selected == null || !d.ml$removeCustomMarker(selected)) {
                MapAtlasesMod.LOGGER.warn("Tried to delete custom marker but none was found");
            }
        }
    }


    public static boolean maybePlaceMarkerInFront(Player player, ItemStack atlas) {
        HitResult hit = player.pick(player.getAttributeBaseValue(Attributes.BLOCK_INTERACTION_RANGE), 1, true);
        if (hit instanceof BlockHitResult bh) {
            var resoult = MapHelper.toggleMarkersAtPos(player.level(), bh.getBlockPos(), atlas, player);
            if (!resoult) {
                //check for vanilla banners
                MapItemSavedData data = MapHelper.getMapData(atlas, player.level(), player);
                if (data != null) {
                    resoult = data.toggleBanner(player.level(), bh.getBlockPos());
                }
            }
            return resoult;
        }

        return false;
    }

    public static void updateMarkers(MapItemSavedData data, Player player, int maxRange) {

        ExpandedMapData d = ((ExpandedMapData) data);
        Map<String, MLMapMarker<?>> markers = new HashMap<>(d.ml$getCustomMarkers());
        if (!markers.isEmpty()) {
            markers.entrySet().removeIf(m -> !m.getValue().shouldRefreshFromWorld());
            List<String> toRemove = new ArrayList<>();
            List<MLMapMarker<?>> toAdd = new ArrayList<>();
            Level level = player.level();
            for (var m : markers.entrySet()) {
                var marker = m.getValue();
                BlockPos pos = marker.getPos();
                if (pos.distToCenterSqr(player.position()) < (maxRange * maxRange)) {
                    if (level.isLoaded(pos)) {
                        MLMapMarker<?> newMarker = marker.getType().value().createMarkerFromWorld(level, marker.getPos());
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
            toRemove.forEach(d::ml$removeCustomMarker);
            toAdd.forEach(d::ml$addCustomMarker);
        }
    }
}
