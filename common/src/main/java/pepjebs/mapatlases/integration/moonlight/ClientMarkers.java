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
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.quickplay.QuickPlayLog;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
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
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.ui.MapAtlasesHUD;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.integration.XaeroMinimapCompat;
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

    private static final TagKey<MapDecorationType<?, ?>> PINS = TagKey.create(MapDataRegistry.REGISTRY_KEY, MapAtlasesMod.res("pins"));
    private static final WeakHashMap<MapDecorationType<?,?>, ResourceLocation> SMALL_PINS = new WeakHashMap<>();

    private static final Map<String, Set<MapBlockMarker<?>>> markers = new HashMap<>();
    private static final Map<Slice, Set<MapBlockMarker<?>>> markersPerSlice = new HashMap<>();
    private static final Map<MapItemSavedData, String> mapLookup = new IdentityHashMap<>();
    private static String lastFolderName = null;
    private static QuickPlayLog.Type lastType = QuickPlayLog.Type.SINGLEPLAYER;
    private static Path currentPath = null;

    public static void setWorldFolder(String pId, QuickPlayLog.Type type) {
        lastFolderName = pId;
        lastType = type;
    }

    public static void loadClientMarkers(long seed, String levelName) {
        markers.clear();
        markersPerSlice.clear();
        mapLookup.clear();
        //if not in multiplayer we have folder name here
        String fileName = lastFolderName == null ? levelName : lastFolderName;
        currentPath = PlatHelper.getGamePath()
                .resolve("map_atlases/" + lastType.getSerializedName()
                        + "/" + fileName + ".nbt");

        try (InputStream inputStream = new FileInputStream(currentPath.toFile())) {
            load(NbtIo.readCompressed(inputStream));
        } catch (Exception ignored) {
        }

        if (MapAtlasesClientConfig.convertXaero.get()) {
            XaeroMinimapCompat.parseXaeroWaypoints(lastFolderName);
        }

        lastFolderName = null;
        lastType = QuickPlayLog.Type.SINGLEPLAYER;
    }

    public static void saveClientMarkers() {
        if (markers.isEmpty()) return;
        try {
            if (currentPath != null && !Files.exists(currentPath)) {
                Files.createDirectories(currentPath.getParent());
            }
            try (OutputStream outputstream = new FileOutputStream(currentPath.toFile())) {
                NbtIo.writeCompressed(save(), outputstream);
            }

        } catch (Exception ignored) {
        }
        markers.clear();
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
                c.put(marker.getTypeId(), marker.saveToNBT());
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

    public static void addMarker(MapDataHolder holder, ColumnPos pos, String text, int index) {
        MapBlockMarker<?> marker = getPinAt(index).createEmptyMarker();
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

    private static MapDecorationType<?, ?> getPinAt(int index) {
        var pins = MapDataRegistry.getRegistry(Utils.hackyGetRegistryAccess())
                .getTag(PINS).get().stream().sorted(Comparator.comparing(h -> h.unwrapKey().get())).toList();
        return pins.get(Math.floorMod(index, pins.size())).value();
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

    public static void renderDecorationPreview(GuiGraphics pGuiGraphics, float x, float y, int index, boolean outline, int alpha) {
        CustomDecorationButton.renderStaticMarker(pGuiGraphics, getPinAt(index), x, y, 1, outline, alpha);
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
                    ResourceLocation texture = SMALL_PINS.computeIfAbsent(marker.getType(), t->
                            Utils.getID(t).withPath(k -> "map_marker/" + k + "_small"));
                    TextureAtlasSprite sprite = MapDecorationClientManager.getAtlasSprite(texture);
                    RenderUtil.renderSprite(matrixStack, vertexBuilder, LightTexture.FULL_BRIGHT, i++, 255, 255, 255, sprite);
                    matrixStack.popPose();
                }
            }
        }
    }



    //TODO: register custom marker type to allow for fancier renderer on maps when focused

    private static boolean isOffscreen(float maxSize, float playerYRot, Vec3 dist) {
        var c = dist.yRot(playerYRot * Mth.DEG_TO_RAD);
        float l = maxSize / 2 + 5;
        return (c.z <= l) && (c.z >= -l) && (c.x <= l) && (c.x >= -l);
    }

    //TODO: change
    public static void focusMarker(MapDataHolder map, CustomMapDecoration deco, boolean focused) {
        if (deco instanceof PinDecoration mp) {
            mp.forceFocused(focused);
        }
    }

    public static boolean isDecorationFocused(MapDataHolder map, CustomMapDecoration deco) {
        if (deco instanceof PinDecoration mp) {
            return mp.isFocused();
        }
        return false;
    }


}
