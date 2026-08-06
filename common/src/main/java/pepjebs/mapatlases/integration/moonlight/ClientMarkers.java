package pepjebs.mapatlases.integration.moonlight;

import net.mehvahdjukaar.moonlight.api.map.ExpandedMapData;
import net.mehvahdjukaar.moonlight.api.map.MapDataRegistry;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecoration;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecorationType;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapMarker;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.quickplay.QuickPlayLog;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.integration.XaeroMinimapCompat;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

public class ClientMarkers {

    private static final TagKey<MLMapDecorationType<?, ?>> PINS =
            TagKey.create(MapDataRegistry.MAP_DECORATION_REGISTRY_KEY, MapAtlasesMod.res("pins"));

    protected static final Map<MapId, Set<MarkerHolder>> MARKERS_PER_MAP = new HashMap<>();

    private static String lastFolderNameOrIP = null;
    private static QuickPlayLog.Type lastType = QuickPlayLog.Type.SINGLEPLAYER;
    private static Path currentPath = null;

    @ApiStatus.Internal
    public static void setWorldFolder(String pId, QuickPlayLog.Type type) {
        lastFolderNameOrIP = pId;
        lastType = type;
    }

    @ApiStatus.Internal
    public static void deleteAllMarkersData(String folderName) {
        try {
            var path = getFilePath(folderName, QuickPlayLog.Type.SINGLEPLAYER);
            Files.deleteIfExists(path);
        } catch (Exception e) {
            MapAtlasesMod.LOGGER.error("Could not delete client markers data of world {}", folderName, e);
        }
    }

    @ApiStatus.Internal
    public static synchronized void loadClientMarkers(long seed, String levelName, HolderLookup.Provider registries) {

        MARKERS_PER_MAP.clear();

        if (lastFolderNameOrIP == null) {
            throw new RuntimeException("Could not load client markers data. Folder name is null");
        }

        currentPath = getFilePath(lastFolderNameOrIP, lastType);

        if (Files.exists(currentPath)) {
            try (InputStream inputStream = Files.newInputStream(currentPath)) {
                load(NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap()), registries);
            } catch (Exception e) {
                MapAtlasesMod.LOGGER.error("Corrupt client markers file at {}, deleting", currentPath, e);
                try {
                    Files.deleteIfExists(currentPath);
                } catch (Exception ignored) {
                }
            }
        }

        if (MapAtlasesClientConfig.convertXaero.get()) {
            XaeroMinimapCompat.parseXaeroWaypoints(lastFolderNameOrIP);
        }

        lastFolderNameOrIP = null;
        lastType = QuickPlayLog.Type.SINGLEPLAYER;
    }

    private static String sanitiseServerName(String input) {
        return input.toLowerCase()
                .replaceAll("\\]:\\d+$", "")
                .replaceAll("[\\[\\]]", "")
                .replaceAll("[^a-z0-9 ]", "_");
    }

    @NotNull
    private static Path getFilePath(String id, QuickPlayLog.Type type) {
        String fileName = (type == QuickPlayLog.Type.SINGLEPLAYER)
                ? id
                : sanitiseServerName(id);

        return PlatHelper.getGamePath()
                .resolve("map_atlases/" + type.getSerializedName() + "/" + fileName + ".nbt");
    }

    public static void clearClientMarkers() {
        MARKERS_PER_MAP.clear();
    }

    public static void saveClientMarkers(RegistryAccess registryAccess) {
        Path path = currentPath;
        if (path == null) return;

        CompoundTag snapshot;
        int count;

        // Only lock long enough to snapshot
        synchronized (ClientMarkers.class) {
            if (MARKERS_PER_MAP.isEmpty()) return;
            snapshot = save(registryAccess);
            count = MARKERS_PER_MAP.size();
        }

        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");

        try {
            Files.createDirectories(path.getParent());

            try (OutputStream out = Files.newOutputStream(tmp)) {
                NbtIo.writeCompressed(snapshot, out);
            }

            Files.move(tmp, path,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);

            MapAtlasesMod.LOGGER.info("Saved {} client map waypoints safely", count);

        } catch (Exception e) {
            MapAtlasesMod.LOGGER.error("Failed safe-save of client markers at {}", path, e);
        }
    }

    private static void load(CompoundTag tag, HolderLookup.Provider registries) {
        for (var k : tag.getAllKeys()) {
            MapId id = mapIdFromString(k);
            ListTag listNbt = tag.getList(k, Tag.TAG_COMPOUND);
            for (int j = 0; j < listNbt.size(); ++j) {
                CompoundTag c = listNbt.getCompound(j);
                var marker = MarkerHolder.parse(c, registries);
                if (marker != null) {
                    addMarker(id, marker);
                }
            }

        }
    }

    private static MapId mapIdFromString(String id) {
        return new MapId(Integer.parseInt(id.substring(4)));
    }

    private static CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        for (var v : MARKERS_PER_MAP.entrySet()) {
            ListTag listNBT = new ListTag();
            for (var marker : v.getValue()) {
                Tag markerSaved = marker.savedMarker;
                listNBT.add(markerSaved);
            }
            tag.put(v.getKey().key(), listNBT);
        }
        return tag;
    }

    public static Set<MLMapMarker<?>> getAll(MapId mapId, MapItemSavedData data) {
        var pins = MARKERS_PER_MAP.get(mapId);
        if (pins != null) {
            Set<MLMapMarker<?>> set = pins.stream().map(m -> m.marker).collect(Collectors.toSet());
            HolderOwner<MLMapDecorationType<?, ?>> reg = MapDataRegistry.getMapDecorationRegistry(Utils.hackyGetRegistryAccess()).holderOwner();
            for (var s : set) {
                if (!s.getType().canSerializeIn(reg)) {
                    MapAtlasesMod.LOGGER.error("Found a marker that does not belong to this registry. How? Skipping all client pins");
                    return Set.of();
                }
            }
            return set;
        }
        return Set.of();
    }

    public static synchronized void placePin(MapDataHolder holder, ColumnPos pos, String text, int index) {
        Holder<MLMapDecorationType<?, ?>> type = getPinWithIndex(index);
        Optional<Component> name;
        if (!text.isEmpty()) {
            name = Optional.of(Component.translatable(text));
        } else {
            name = Optional.empty();
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            MapAtlasesMod.LOGGER.error("Tried to place a pin but level was null");
            return;
        }
        Integer h = holder.height;
        if (h == null) h = level.dimension().equals(holder.data.dimension) ?
                level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.z(), pos.z()) : 64;
        //aaa not correct
        var pinMarker = new PinMarker(type, new BlockPos(pos.x(), h, pos.z()), name, false);
        addMarker(holder.id, MarkerHolder.of(pinMarker, level.registryAccess()));
        //add immediately
        ((ExpandedMapData) holder.data).ml$addCustomMarker(pinMarker);
    }

    private static void addMarker(MapId holder, MarkerHolder pinMarker) {
        MARKERS_PER_MAP.computeIfAbsent(holder, k -> new HashSet<>()).add(pinMarker);
    }

    protected static Holder<MLMapDecorationType<?, ?>> getPinWithIndex(int index) {
        Optional<HolderSet.Named<MLMapDecorationType<?, ?>>> tag =
                MapDataRegistry.getMapDecorationRegistry(Utils.hackyGetRegistryAccess()).getTag(PINS);

        if (tag.isEmpty()) throw new AssertionError("map_atlases:pins tag missing");

        var pins = tag.get().stream()
                .sorted(Comparator.comparing(h -> h.unwrapKey().orElseThrow().toString()))
                .toList();

        return pins.get(Math.floorMod(index, pins.size()));
    }


    public static synchronized boolean removeClientDeco(MapId mapId, String key) {
        var mr = MARKERS_PER_MAP.get(mapId);
        if (mr != null) {
            mr.removeIf(m -> m.marker.getMarkerUniqueId().equals(key));
            if (mr.isEmpty()) {
                MARKERS_PER_MAP.remove(mapId);
            }
            return true;
        }
        return false;
    }

    //TODO: register custom marker type to allow for fancier renderer on maps when focused


    //TODO: change
    public static void focusClientDeco(MapDataHolder map, MLMapDecoration deco, boolean focused) {
        if (deco instanceof PinDecoration mp) {
            mp.forceFocused(focused);
        }
    }

    public static boolean isClientDecoFocused(MapDataHolder map, MLMapDecoration deco) {
        if (deco instanceof PinDecoration mp) {
            return mp.isFocused();
        }
        return false;
    }


    protected record MarkerHolder(MLMapMarker<?> marker, Tag savedMarker) {

        public static MarkerHolder of(MLMapMarker<?> marker, HolderLookup.Provider registries) {
            var ops = registries.createSerializationContext(NbtOps.INSTANCE);
            Tag markerSaved = MLMapMarker.CODEC
                    .encodeStart(ops, marker).getOrThrow();
            return new MarkerHolder(marker, markerSaved);
        }


        @Nullable
        public static MarkerHolder parse(Tag tag, HolderLookup.Provider registries) {
            var ops = registries.createSerializationContext(NbtOps.INSTANCE);
            var parsed = MLMapMarker.CODEC.parse(ops, tag);
            if (parsed.result().isEmpty()) {
                MapAtlasesMod.LOGGER.error("Failed to parse a client marker from nbt. Skipping. Tag was: {}", tag);
                return null;
            }
            MLMapMarker<?> marker1 = parsed.result().get();
            if (!marker1.getType().isBound() || marker1.getType().kind() == Holder.Kind.DIRECT) {
                MapAtlasesMod.LOGGER.error("Failed to parse a client marker from nbt because its type was not bound to the registry. Skipping. Tag was: {}", tag);
                return null;
            }

            return new MarkerHolder(marker1, tag);
        }

    }
}
