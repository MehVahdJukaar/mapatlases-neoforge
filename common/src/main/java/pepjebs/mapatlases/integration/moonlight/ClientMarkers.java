package pepjebs.mapatlases.integration.moonlight;

import net.mehvahdjukaar.moonlight.api.map.CustomMapDecoration;
import net.mehvahdjukaar.moonlight.api.map.ExpandedMapData;
import net.mehvahdjukaar.moonlight.api.map.MapDataRegistry;
import net.mehvahdjukaar.moonlight.api.map.markers.MapBlockMarker;
import net.mehvahdjukaar.moonlight.api.map.type.MapDecorationType;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.quickplay.QuickPlayLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.integration.XaeroMinimapCompat;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
public class ClientMarkers {

    private static final TagKey<MapDecorationType<?, ?>> PINS =
            TagKey.create(MapDataRegistry.REGISTRY_KEY, MapAtlasesMod.res("pins"));

    protected static final Map<Integer, Set<MapBlockMarker<?>>> markersPerMap = new HashMap<>();
    private static final Map<Integer, String> mapIdToStringLookup = new IdentityHashMap<>();

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
    public static synchronized void loadClientMarkers(long seed, String levelName) {
        markersPerMap.clear();
        mapIdToStringLookup.clear();

        if (lastFolderNameOrIP == null) {
            throw new RuntimeException("Could not load client markers data. Folder name is null");
        }

        currentPath = getFilePath(lastFolderNameOrIP, lastType);

        if (Files.exists(currentPath)) {
            try (InputStream inputStream = Files.newInputStream(currentPath)) {
                load(NbtIo.readCompressed(inputStream));
            } catch (Exception e) {
                MapAtlasesMod.LOGGER.error("Corrupt client markers file at {}, deleting", currentPath, e);
                try { Files.deleteIfExists(currentPath); } catch (Exception ignored) {}
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

    public static void saveClientMarkers() {
        Path path = currentPath;
        if (path == null) return;

        CompoundTag snapshot;
        int count;

        // Only lock long enough to snapshot
        synchronized (ClientMarkers.class) {
            if (markersPerMap.isEmpty()) return;
            snapshot = save();
            count = markersPerMap.size();
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

    public static synchronized void unloadWorld() {
        saveClientMarkers();
        markersPerMap.clear();
        mapIdToStringLookup.clear();
    }

    private static void load(CompoundTag tag) {
        for (var k : tag.getAllKeys()) {
            Set<MapBlockMarker<?>> l = new HashSet<>();
            ListTag listNbt = tag.getList(k, Tag.TAG_COMPOUND);
            for (int j = 0; j < listNbt.size(); ++j) {
                var c = listNbt.getCompound(j);
                MapBlockMarker<?> marker = MapDataRegistry.readMarker(c);
                if (marker != null) l.add(marker);
            }
            markersPerMap.put(MapAtlasesAccessUtils.findMapIntFromString(k), l);
        }
    }

    private static CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        for (var v : markersPerMap.entrySet()) {
            ListTag listNBT = new ListTag();
            for (var marker : v.getValue()) {
                CompoundTag c = new CompoundTag();
                c.put(marker.getTypeId(), marker.saveToNBT());
                listNBT.add(c);
            }
            tag.put(mapIdToStringLookup.get(v.getKey()), listNBT);
        }
        return tag;
    }

    // READ — no lock, very cheap
    public static Set<MapBlockMarker<?>> send(Integer integer, MapItemSavedData data) {
        mapIdToStringLookup.computeIfAbsent(integer, g -> {
            MapDataHolder holder = MapDataHolder.findFromId(Minecraft.getInstance().level, integer);
            return Objects.requireNonNull(holder).stringId;
        });
        return markersPerMap.getOrDefault(integer, Set.of());
    }

    // WRITE — synchronized
    public static synchronized void addPin(MapDataHolder holder, ColumnPos pos, String text, int index) {
        MapBlockMarker<?> marker = getPinWithIndex(index).createEmptyMarker();
        if (!text.isEmpty()) marker.setName(Component.translatable(text));

        ClientLevel level = Minecraft.getInstance().level;
        Integer h = holder.height;
        if (h == null) {
            h = level.dimension().equals(holder.data.dimension)
                    ? level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.x(), pos.z())
                    : 64;
        }

        marker.setPos(new BlockPos(pos.x(), h, pos.z()));
        markersPerMap.computeIfAbsent(holder.id, k -> new HashSet<>()).add(marker);
        ((ExpandedMapData) holder.data).addCustomMarker(marker);
    }

    protected static MapDecorationType<?, ?> getPinWithIndex(int index) {
        Optional<HolderSet.Named<MapDecorationType<?, ?>>> tag =
                MapDataRegistry.getRegistry(Utils.hackyGetRegistryAccess()).getTag(PINS);

        if (tag.isEmpty()) throw new AssertionError("map_atlases:pins tag missing");

        var pins = tag.get().stream()
                .sorted(Comparator.comparing(h -> h.unwrapKey().orElseThrow().toString()))
                .toList();

        return pins.get(Math.floorMod(index, pins.size())).value();
    }


    public static synchronized boolean removeClientDeco(int mapId, String key) {
        var mr = markersPerMap.get(mapId);
        if (mr != null) {
            mr.removeIf(m -> m.getMarkerId().equals(key));
            if (mr.isEmpty()) {
                markersPerMap.remove(mapId);
            }
            return true;
        }
        return false;
    }


    //TODO: register custom marker type to allow for fancier renderer on maps when focused


    //TODO: change
    public static void focusClientDeco(MapDataHolder map, CustomMapDecoration deco, boolean focused) {
        if (deco instanceof PinDecoration mp) {
            mp.forceFocused(focused);
        }
    }

    public static boolean isClientDecoFocused(MapDataHolder map, CustomMapDecoration deco) {
        if (deco instanceof PinDecoration mp) {
            return mp.isFocused();
        }
        return false;
    }


}
