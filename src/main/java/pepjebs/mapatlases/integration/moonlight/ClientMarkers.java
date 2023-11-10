package pepjebs.mapatlases.integration.moonlight;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.moonlight.api.client.util.RenderUtil;
import net.mehvahdjukaar.moonlight.api.map.CustomMapDecoration;
import net.mehvahdjukaar.moonlight.api.map.ExpandedMapData;
import net.mehvahdjukaar.moonlight.api.map.MapDataRegistry;
import net.mehvahdjukaar.moonlight.api.map.client.MapDecorationClientManager;
import net.mehvahdjukaar.moonlight.api.map.markers.MapBlockMarker;
import net.mehvahdjukaar.moonlight.api.map.type.MapDecorationType;
import net.mehvahdjukaar.moonlight.api.misc.DataObjectReference;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.MipmapGenerator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.ui.MapAtlasesHUD;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.Slice;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ClientMarkers {

    public static final TagKey<MapDecorationType<?, ?>> PINS = TagKey.create(MapDataRegistry.REGISTRY_KEY, MapAtlasesMod.res("pins"));
    private static int currentWorldHash = 0;
    private static final Map<String, Set<MapBlockMarker<?>>> markers = new HashMap<>();
    private static final Map<Slice, Set<MapBlockMarker<?>>> markersPerSlice = new HashMap<>();
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

        } catch (Exception ignored) {
        }
        markers.clear();
    }

    public static void loadClientMarkers(int hash) {
        markers.clear();
        mapLookup.clear();
        currentWorldHash = hash;

        Path path = getPath();
        try (InputStream inputStream = new FileInputStream(path.toFile())) {
            load(NbtIo.readCompressed(inputStream));
        } catch (Exception ignored) {
        }
    }

    @NotNull
    private static Path getPath() {
        return FMLPaths.GAMEDIR.get().resolve("map_atlases/" + currentWorldHash + ".nbt");
    }

    private static final DataObjectReference<MapDecorationType<?, ?>> PIN = new DataObjectReference<>(
            MapAtlasesMod.res("pin"), MapDataRegistry.REGISTRY_KEY);


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
        markersPerSlice.computeIfAbsent(holder.slice, a -> new HashSet<>()).add(marker);
        //add immediately
        ((ExpandedMapData) holder.data).addCustomMarker(marker);
    }

    @NotNull
    private static List<Holder<MapDecorationType<?, ?>>> getPins() {
        return MapDataRegistry.getRegistry(Utils.hackyGetRegistryAccess())
                .getTag(PINS).get().stream().toList();
    }

    private static void load(CompoundTag tag) {
        for (var k : tag.getAllKeys()) {
            Set<MapBlockMarker<?>> l = new HashSet<>();
            ListTag listNbt = tag.getList(k, Tag.TAG_COMPOUND);
            for (int j = 0; j < listNbt.size(); ++j) {
                var c = listNbt.getCompound(j);
                MapBlockMarker<?> marker = MapDataRegistry.readMarker(c);
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
            tag.put(v.getKey(), listNBT);
        }
        return tag;
    }

    public static Set<MapBlockMarker<?>> send(Integer integer, MapItemSavedData data) {
        String stringId = mapLookup.computeIfAbsent(data, g -> {
            MapDataHolder holder = MapDataHolder.findFromId(Minecraft.getInstance().level, integer);
            String st = Objects.requireNonNull(holder).stringId;
            var markersSet = markers.get(st);
            if (markersSet != null) {
                for (var m : markersSet) {
                    //just adding once..
                    //   ((ExpandedMapData) data).addCustomMarker(m);
                }
                markersPerSlice.computeIfAbsent(holder.slice, a -> new HashSet<>())
                        .addAll(markersSet);
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
        //iterate over all to find ones to remove
        for (var v : markersPerSlice.values()) {
            v.removeIf(m -> m.getMarkerId().equals(key));
        }
    }

    public static void renderDecorationPreview(GuiGraphics pGuiGraphics, float x, float y, int index, boolean outline) {
        var p = getPins();
        var t = p.get(index % p.size());
        CustomMapDecoration d;

        if (MapDecorationClientManager.getRenderer(t.get()) instanceof PinDecorationRenderer) {
            d = new PinDecoration(t.value(), (byte) 0, (byte) 0, (byte) 0, null);
        } else d = new CustomMapDecoration(t.value(), (byte) 0, (byte) 0, (byte) 0, null);
        try {
            //this will fail with custom types... TODO: fix
            CustomDecorationButton.renderStaticMarker(pGuiGraphics, d, null, x, y, 1, outline);
        } catch (Exception ignored) {
        }
    }

    public static void drawSmallPins(GuiGraphics graphics, Font font, double mapCenterX, double mapCenterZ, Slice slice,
                                     float widgetWorldLen, Player player, boolean rotateWithPlayer) {

        var pins = markersPerSlice.get(slice);

        if (pins != null) {
            PoseStack matrixStack = graphics.pose();
            int i = 0;
            VertexConsumer vertexBuilder = graphics.bufferSource().getBuffer(MapDecorationClientManager.MAP_MARKERS_RENDER_TYPE);
            float yRot = rotateWithPlayer ? player.getYRot() : 180;
            BlockPos playerPos = rotateWithPlayer ? player.blockPosition() : BlockPos.containing(mapCenterX, 0, mapCenterZ);
            for (var marker : pins) {
                BlockPos pos = marker.getPos();
                Vec3 dist = playerPos.getCenter().subtract(pos.getCenter());
                if (marker instanceof PinMarker mp && mp.isFocused() && !isOffscreen(widgetWorldLen, yRot, dist)) {
                    matrixStack.pushPose();
                    double angle = Mth.RAD_TO_DEG * (Math.atan2(dist.x, dist.z)) + yRot;
                    var pp = MapAtlasesHUD.getDirectionPos(29F, (float) angle);
                    float a = pp.getFirst();
                    float b = pp.getSecond();

                    matrixStack.translate(a, b, 5);
                    matrixStack.scale(4, 4, 0);
                    matrixStack.translate(-0.25, -0.25, 0);
                    ResourceLocation id = Utils.getID(marker.getType());
                    ResourceLocation texture = id.withPath(k -> "map_marker/" + k + "_small");
                    TextureAtlasSprite sprite = MapDecorationClientManager.getAtlasSprite(texture);
                    RenderUtil.renderSprite(matrixStack, vertexBuilder, LightTexture.FULL_BRIGHT, i++, 255, 255, 255, sprite);
                    matrixStack.popPose();
                }
            }
        }
        //so we can use local coordinates
        //idk wy wrap doesnt work, it does the same as here
        //vertexBuilder = sprite.wrap(vertexBuilder);

    }

    //TODO: register custom marker type to allow for fancier renderer on maps when focused

    private static boolean isOffscreen( float maxSize, float playerYRot, Vec3 dist) {
        var c =  dist.yRot(playerYRot * Mth.DEG_TO_RAD);
        float l = maxSize / 2 + 5;
        return (c.z <= l) && (c.z >= -l) && (c.x <= l) && (c.x >= -l);
    }

    public static void focusMarker(MapDataHolder map, CustomMapDecoration deco, boolean focused) {
        MapBlockMarker<?> found = decoToMarker(map, deco);
        if (found instanceof PinMarker mp) {
            //hack. this is used for somehting else...
            mp.setFocused(focused);
            //Bad code
            if (deco instanceof PinDecoration pd) pd.focused = focused;
        }
    }

    @Nullable
    private static MapBlockMarker<?> decoToMarker(MapDataHolder map, CustomMapDecoration deco) {
        return DECO_TO_MARKER.computeIfAbsent(deco, d -> {
            Set<MapBlockMarker<?>> mar = markers.get(map.stringId);
            if (mar != null) {
                for (var m : mar) {
                    if (d.equals(m.createDecorationFromMarker(map.data))) {
                        return m;
                    }
                }
            }
            return null;
        });
    }

    public static boolean isDecorationFocused(MapDataHolder map, CustomMapDecoration deco) {
        MapBlockMarker<?> found = decoToMarker(map, deco);
        if (found instanceof PinMarker mp) {
            return mp.isFocused();
        }
        return false;
    }

    private static final WeakHashMap<CustomMapDecoration, MapBlockMarker<?>> DECO_TO_MARKER = new WeakHashMap<>();

}
