package pepjebs.mapatlases.integration.moonlight;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.mehvahdjukaar.moonlight.api.client.util.RenderUtil;
import net.mehvahdjukaar.moonlight.api.map.ExpandedMapData;
import net.mehvahdjukaar.moonlight.api.map.MapDataRegistry;
import net.mehvahdjukaar.moonlight.api.map.client.MapDecorationClientManager;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecoration;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecorationType;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapMarker;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.quickplay.QuickPlayLog;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.ui.MapAtlasesHUD;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.integration.XaeroMinimapCompat;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.MapType;
import pepjebs.mapatlases.utils.Slice;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ClientMarkers {

    private static final TagKey<MLMapDecorationType<?, ?>> PINS = TagKey.create(MapDataRegistry.REGISTRY_KEY, MapAtlasesMod.res("pins"));
    private static final WeakHashMap<MLMapDecorationType<?, ?>, ResourceLocation> SMALL_PINS = new WeakHashMap<>();

    private static final Map<MapId, Set<MLMapMarker<?>>> markersPerMap = new Object2ObjectOpenHashMap<>();
    private static String lastFolderNameOrIP = null;
    private static QuickPlayLog.Type lastType = QuickPlayLog.Type.SINGLEPLAYER;
    private static Path currentPath = null;

    public static void setWorldFolder(String pId, QuickPlayLog.Type type) {
        lastFolderNameOrIP = pId;
        lastType = type;
    }

    public static void deleteAllMarkersData(String folderName) {
        try {
            var path = getFilePath(folderName, QuickPlayLog.Type.SINGLEPLAYER);
            Files.deleteIfExists(path);
        } catch (Exception e) {
            MapAtlasesMod.LOGGER.error("Could not delete client markers saved data of world {}", folderName, e);
        }
    }

    public static void loadClientMarkers(long seed, String levelName, HolderLookup.Provider registries) {
        markersPerMap.clear();
        //if not in multiplayer we have folder name here. foldername should be null on client
        if (lastFolderNameOrIP == null) {
            throw new RuntimeException("Could not load client markers saved data. Folder name is null");
        }
        currentPath = getFilePath(lastFolderNameOrIP, lastType);

        if (Files.exists(currentPath)) {
            try (InputStream inputStream = new FileInputStream(currentPath.toFile())) {
                load(NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap()), registries);
            } catch (Exception ignored) {
                MapAtlasesMod.LOGGER.error("Could not load client markers saved data at {}", currentPath);
            }
        }
        if (MapAtlasesClientConfig.convertXaero.get()) {
            XaeroMinimapCompat.parseXaeroWaypoints(lastFolderNameOrIP);
        }

        //uhmm why?
        lastFolderNameOrIP = null;
        lastType = QuickPlayLog.Type.SINGLEPLAYER;
    }


    private static String sanitiseServerName(String input) {
        // Convert to lowercase and replace non-alphanumeric characters except spaces with '_'
        return input.toLowerCase()
                .replaceAll("\\]:\\d+$", "")
                .replaceAll("[\\[\\]]", "")
                .replaceAll("[^a-z0-9 ]", "_");
    }

    @NotNull
    private static Path getFilePath(String lastFolderNameOrIP, QuickPlayLog.Type type) {
        String fileName;
        if (lastType == QuickPlayLog.Type.SINGLEPLAYER) {
            fileName = lastFolderNameOrIP;
        } else {
            fileName = sanitiseServerName(lastFolderNameOrIP);
        }
        try {
            return PlatHelper.getGamePath()
                    .resolve("map_atlases/" + type.getSerializedName() + "/" + fileName + ".nbt");
        } catch (Exception e) {
            throw new RuntimeException("Could not get client pins path for world " + fileName, e);
        }
    }

    public static void saveClientMarkers(RegistryAccess registryAccess) {
        if (markersPerMap.isEmpty()) return;
        if (currentPath == null) {
            MapAtlasesMod.LOGGER.error("Could not save client markers saved data. Path is null");
            return;
        }
        try {
            if (!Files.exists(currentPath)) {
                Files.createDirectories(currentPath.getParent());
            }
            try (OutputStream outputstream = new FileOutputStream(currentPath.toFile())) {
                NbtIo.writeCompressed(save(registryAccess), outputstream);
                MapAtlasesMod.LOGGER.info("Saved {} client map waypoints", markersPerMap.size());
            }

        } catch (Exception e) {
            MapAtlasesMod.LOGGER.error("Could not save client markers saved data at {}", currentPath, e);
        }
        markersPerMap.clear();
    }

    private static void load(CompoundTag tag, HolderLookup.Provider registries) {
        RegistryOps<Tag> registryOps = registries.createSerializationContext(NbtOps.INSTANCE);

        for (var k : tag.getAllKeys()) {
            Set<MLMapMarker<?>> l = new HashSet<>();
            ListTag listNbt = tag.getList(k, Tag.TAG_COMPOUND);
            for (int j = 0; j < listNbt.size(); ++j) {
                var c = listNbt.getCompound(j);
                MLMapMarker<?> marker = MLMapMarker.REFERENCE_CODEC.
                        parse(registryOps, c).getOrThrow();
                if (marker != null) {
                    l.add(marker);
                }
            }
            markersPerMap.put(mapIdFromString(k), l);
        }
    }

    private static CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        for (var v : markersPerMap.entrySet()) {
            ListTag listNBT = new ListTag();
            for (var marker : v.getValue()) {
                Tag markerSaved = MLMapMarker.REFERENCE_CODEC
                        .encodeStart(ops, marker).getOrThrow();
                listNBT.add(markerSaved);
            }
            tag.put(v.getKey().key(), listNBT);
        }
        return tag;
    }

    public static Set<MLMapMarker<?>> send(MapId mapId, MapItemSavedData data) {
        var pins = markersPerMap.get(mapId);
        if (pins != null) {
            return pins;
        }
        return Set.of();
    }

    public static void addMarker(MapDataHolder holder, ColumnPos pos, String text, int index) {
        Holder<MLMapDecorationType<?, ?>> type = getPinAt(index);
        Optional<Component> name;
        if (!text.isEmpty()) {
            name = Optional.of(Component.translatable(text));
        } else {
            name = Optional.empty();
        }
        ClientLevel level = Minecraft.getInstance().level;
        Integer h = holder.height;
        if (h == null) h = level.dimension().equals(holder.data.dimension) ?
                level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.z(), pos.z()) : 64;
        //aaa not correct
        var marker = new PinMarker(type, new BlockPos(pos.x(), h, pos.z()), name, false);
        markersPerMap.computeIfAbsent(holder.id, k -> new HashSet<>()).add(marker);
        //add immediately
        ((ExpandedMapData) holder.data).ml$addCustomMarker(marker);
    }

    private static Holder<MLMapDecorationType<?, ?>> getPinAt(int index) {
        Optional<HolderSet.Named<MLMapDecorationType<?, ?>>> tag = MapDataRegistry.getRegistry(Utils.hackyGetRegistryAccess()).getTag(PINS);
        if (tag.isEmpty()) {
            throw new AssertionError("map_atlases:pins tag was empty or not found. How is this possible?");
        }
        var pins = tag.get().stream().sorted(Comparator.comparing(h -> {
            Optional<ResourceKey<MLMapDecorationType<?, ?>>> key = h.unwrapKey();
            if (key.isEmpty()) throw new AssertionError("Registry key for MapDecorationType was null. How?");
            return key.get().toString();
        })).toList();
        return pins.get(Math.floorMod(index, pins.size()));
    }

    public static boolean removeDeco(MapId mapId, String key) {
        var mr = markersPerMap.get(mapId);
        if (mr != null) {
            mr.removeIf(m -> m.getMarkerUniqueId().equals(key));
        }
        return mr != null;
    }

    public static void renderDecorationPreview(GuiGraphics pGuiGraphics, float x, float y, int index, boolean outline, int alpha) {
        CustomDecorationButton.renderStaticMarker(pGuiGraphics, getPinAt(index), x, y, 1, outline, alpha);
    }


    public static void drawSmallPins(GuiGraphics graphics, Font font, double mapCenterX, double mapCenterZ, Slice slice,
                                     float widgetWorldLen, Player player, boolean rotateWithPlayer, MapCollection collection) {

        if (slice.type() != MapType.VANILLA) return;

        Registry<MLMapDecorationType<?, ?>> reg = MapDataRegistry.getRegistry(player.level().registryAccess());
        PoseStack matrixStack = graphics.pose();
        int i = 0;
        VertexConsumer vertexBuilder = graphics.bufferSource().getBuffer(MapDecorationClientManager.MAP_MARKERS_RENDER_TYPE);
        float yRot = rotateWithPlayer ? player.getYRot() : 180;
        BlockPos playerPos = rotateWithPlayer ? player.blockPosition() : BlockPos.containing(mapCenterX, 0, mapCenterZ);
        for (var entry : markersPerMap.entrySet()) {
            MapId mapId = entry.getKey();
            if (!collection.hasMap(mapId, MapType.VANILLA)) continue;
            var pins = entry.getValue();
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

                    ResourceLocation texture = SMALL_PINS.computeIfAbsent(marker.getType().value(), t ->
                            reg.getKey(t).withPath(k -> k + "_small"));
                    TextureAtlasSprite sprite = Minecraft.getInstance().getMapDecorationTextures().getSprite(texture);
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
    public static void focusMarker(MapDataHolder map, MLMapDecoration deco, boolean focused) {
        if (deco instanceof PinDecoration mp) {
            mp.forceFocused(focused);
        }
    }

    public static boolean isDecorationFocused(MapDataHolder map, MLMapDecoration deco) {
        if (deco instanceof PinDecoration mp) {
            return mp.isFocused();
        }
        return false;
    }


    private static MapId mapIdFromString(String id) {
        return new MapId(Integer.parseInt(id.substring(4)));
    }
}
