package pepjebs.mapatlases.integration;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.moonlight.api.map.CustomMapDecoration;
import net.mehvahdjukaar.moonlight.api.map.ExpandedMapData;
import net.mehvahdjukaar.moonlight.api.map.MapDecorationRegistry;
import net.mehvahdjukaar.moonlight.api.map.markers.MapBlockMarker;
import net.mehvahdjukaar.moonlight.api.map.type.MapDecorationType;
import net.mehvahdjukaar.moonlight.api.misc.DataObjectReference;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.mehvahdjukaar.moonlight.core.map.MapDataInternal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.NotNull;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ClientMarker {

    private static final TagKey<MapDecorationType<?,?>> PINS = TagKey.create(MapDecorationRegistry.REGISTRY_KEY, MapAtlasesMod.res("pins"));
    private static int name = 0;
    private static final Map<String, Set<MapBlockMarker<?>>> markers = new HashMap<>();
    private static final Map<MapItemSavedData, String> mapLookup = new IdentityHashMap<>();


    public static void saveClientMarkers() {
        if (markers.isEmpty()) return;
        try {
            Path path = getPath();
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
            }
            try (OutputStream outputstream = new FileOutputStream(path.toFile())) {
                NbtIo.writeCompressed(save(), outputstream);
            }
            int aa = 1;

        } catch (Exception ignored) {
        }
        markers.clear();
    }

    public static void loadClientMarkers(int hash) {
        markers.clear();
        mapLookup.clear();
        name = hash;

        Path path = getPath();
        try (InputStream inputStream = new FileInputStream(path.toFile())) {
            load(NbtIo.readCompressed(inputStream));
        } catch (Exception ignored) {
        }
    }

    @NotNull
    private static Path getPath() {
        return FMLPaths.GAMEDIR.get().resolve("map_atlases/" + name + ".nbt");
    }

    private static final DataObjectReference<MapDecorationType<?, ?>> PIN = new DataObjectReference<>(
            MapAtlasesMod.res("pin"), MapDecorationRegistry.REGISTRY_KEY);


    public static void addMarker(MapDataHolder holder, ColumnPos pos, String text, int index) {
        List<Holder<MapDecorationType<?, ?>>> pins = getPins();
        MapBlockMarker<?> marker = pins.get(index % pins.size()).get().createEmptyMarker();
        if (!text.isEmpty()) marker.setName(Component.translatable(text));
        ClientLevel level = Minecraft.getInstance().level;
        Integer h = holder.height;
        if (h == null) h = level.dimension().equals(holder.data.dimension) ?
                level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.z(), pos.z()) : 64;
        marker.setPos(new BlockPos(pos.x(), h, pos.z()));
        markers.computeIfAbsent(holder.stringId, k -> new HashSet<>()).add(marker);
        //add immediately
        ((ExpandedMapData) holder.data).addCustomMarker(marker);
    }

    @NotNull
    private static List<Holder<MapDecorationType<?, ?>>> getPins() {
        return MapDecorationRegistry.getRegistry(Utils.hackyGetRegistryAccess())
                .getTag(PINS).get().stream().toList();
    }

    private static void load(CompoundTag tag) {
        for (var k : tag.getAllKeys()) {
            Set<MapBlockMarker<?>> l = new HashSet<>();
            ListTag listNbt = tag.getList(k, Tag.TAG_COMPOUND);
            for (int j = 0; j < listNbt.size(); ++j) {
                var c = listNbt.getCompound(j);
                MapBlockMarker<?> marker = MapDataInternal.readWorldMarker(c);
                if (marker != null) {
                    l.add(marker);
                }
            }
            markers.put(k, l);
        }
    }

    private static CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        for (var v : markers.entrySet()) {
            ListTag listNBT = new ListTag();
            for (var marker : v.getValue()) {
                CompoundTag c = new CompoundTag();
                c.put(marker.getTypeId(), marker.saveToNBT(new CompoundTag()));
                listNBT.add(c);
            }
            tag.put(v.getKey().toString(), listNBT);
        }
        return tag;
    }

    public static Set<MapBlockMarker<?>> send(Integer integer, MapItemSavedData data) {
        String stringId = mapLookup.computeIfAbsent(data, g -> {
            String st = Objects.requireNonNull(MapDataHolder.findFromId(Minecraft.getInstance().level, integer)).stringId;
            var mr = markers.get(st);
            if (mr != null) {
                for (var m : mr) {
                    //just adding once..
                    //   ((ExpandedMapData) data).addCustomMarker(m);
                }
            }
            return st;
        });

        var m = markers.get(stringId);
        if (m != null) {
            return m;
        }
        return Set.of();
    }

    public static void removeDeco(String mapId, String key) {
        var mr = markers.get(mapId);
        if (mr != null) {
            mr.removeIf(m -> m.getMarkerId().equals(key));
        }
    }

    public static void renderPin(PoseStack poseStack, float x, float y, int index, boolean outline) {
        var p = getPins();
        var t = p.get(index%p.size());
        var d = new CustomMapDecoration(t.value(), (byte) 0,(byte)0,(byte)0, null);
        CustomDecorationButton.renderStaticMarker(poseStack, d, null, x, y, 1, outline);
    }
}
