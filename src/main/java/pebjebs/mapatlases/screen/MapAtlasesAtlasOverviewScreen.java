package pebjebs.mapatlases.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.joml.Matrix4f;
import pebjebs.mapatlases.MapAtlasesMod;
import pebjebs.mapatlases.client.MapAtlasesClient;
import pebjebs.mapatlases.client.ui.MapAtlasesHUD;
import pebjebs.mapatlases.config.MapAtlasesClientConfig;
import pebjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.*;
import java.util.stream.Collectors;

// TODO: If the atlas world map scaling changes, MAX_TAB_DISP needs to change too
// TODO: Map Icon Selectors don't look right at non-default scaling
public class MapAtlasesAtlasOverviewScreen extends AbstractContainerScreen<MapAtlasesAtlasOverviewContainerMenu> {

    public static final ResourceLocation ATLAS_FOREGROUND =
            new ResourceLocation("map_atlases:textures/gui/screen/atlas_foreground.png");
    public static final ResourceLocation ATLAS_BACKGROUND =
            new ResourceLocation("map_atlases:textures/gui/screen/atlas_background.png");
    public static final ResourceLocation PAGE_SELECTED =
            new ResourceLocation("map_atlases:textures/gui/screen/page_selected.png");
    public static final ResourceLocation PAGE_UNSELECTED =
            new ResourceLocation("map_atlases:textures/gui/screen/page_unselected.png");
    public static final ResourceLocation PAGE_OVERWORLD =
            new ResourceLocation("map_atlases:textures/gui/screen/overworld_atlas_page.png");
    public static final ResourceLocation PAGE_NETHER =
            new ResourceLocation("map_atlases:textures/gui/screen/nether_atlas_page.png");
    public static final ResourceLocation PAGE_END =
            new ResourceLocation("map_atlases:textures/gui/screen/end_atlas_page.png");
    public static final ResourceLocation PAGE_OTHER =
            new ResourceLocation("map_atlases:textures/gui/screen/unknown_atlas_page.png");
    public static final ResourceLocation MAP_ICON_TEXTURE = new ResourceLocation("textures/map/map_icons.png");
    private static final RenderType MAP_ICONS = RenderType.entityCutout(MAP_ICON_TEXTURE);
    private static final int ZOOM_BUCKET = 4;
    private static final int PAN_BUCKET = 25;
    private static final int MAX_TAB_DISP = 7;

    private final ItemStack atlas;
    public final String centerMapId;
    public Map<Integer, Pair<String, List<Integer>>> idsToCenters;
    private int mouseXOffset = 0;
    private int mouseYOffset = 0;
    private int currentXCenter;
    private int currentZCenter;
    private double rawMouseXMoved = 0;
    private double rawMouseYMoved = 0;
    private int zoomValue = ZOOM_BUCKET;
    private String currentWorldSelected;
    private final String initialWorldSelected;
    private final int atlasScale;
    private int mapIconSelectorOffset = 0;
    private int dimSelectorOffset = 0;

    public MapAtlasesAtlasOverviewScreen(MapAtlasesAtlasOverviewContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        atlas = menu.atlas;
        idsToCenters = menu.idsToCenters;
        centerMapId = menu.centerMapId;
        atlasScale = menu.atlasScale;
        var coords = idsToCenters.get(MapAtlasesAccessUtils.getMapIntFromString(centerMapId)).getSecond();
        currentXCenter = coords.get(0);
        currentZCenter = coords.get(1);
        currentWorldSelected = MapAtlasesAccessUtils.getPlayerDimKey(inventory.player);
        initialWorldSelected = MapAtlasesAccessUtils.getPlayerDimKey(inventory.player);
        // Play open sound
        inventory.player.playSound(MapAtlasesMod.ATLAS_OPEN_SOUND_EVENT.get(),
                (float) (double) MapAtlasesClientConfig.soundScalar.get(), 1.0F);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        renderBg(context, delta, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        if (minecraft == null || minecraft.player == null || minecraft.level == null) return;
        var matrices = context.pose();

        // Handle zooming
        int atlasBgScaledSize = getAtlasBgScaledSize();
        double drawnMapBufferSize = atlasBgScaledSize / 18.0;
        int atlasDataScaledSize = (int) (atlasBgScaledSize - (2 * drawnMapBufferSize));
        int zoomLevelDim = getZoomLevelDim();
        MapAtlasesClient.setWorldMapZoomLevel(zoomLevelDim * (float) (double) MapAtlasesClientConfig.worldMapDecorationScale.get());
        float mapTextureScale = (float) (atlasDataScaledSize / (128.0 * zoomLevelDim));

        // Draw map background
        double y = (height / 2.0) - (atlasBgScaledSize / 2.0);
        double x = (width / 2.0) - (atlasBgScaledSize / 2.0);
        context.blit(
                ATLAS_BACKGROUND,
                (int) x,
                (int) y,
                0,
                0,
                atlasBgScaledSize,
                atlasBgScaledSize,
                atlasBgScaledSize,
                atlasBgScaledSize
        );

        // Draw selectors
        drawDimensionSelectors(context, x, y, atlasBgScaledSize);
        drawMapDecorationSelectors(context, x, y, atlasBgScaledSize);

        // Draw maps, putting active map in middle of grid
        if (atlas == null) {
            return;
        }
        Map<String, MapItemSavedData> mapInfos = MapAtlasesAccessUtils.getAllMapInfoFromAtlas(minecraft.level, atlas);
        double mapComponentX = x + drawnMapBufferSize;
        double mapComponentY = y + drawnMapBufferSize;
        for (int i = zoomLevelDim - 1; i >= 0; i--) {
            for (int j = zoomLevelDim - 1; j >= 0; j--) {
                int iXIdx = i - (zoomLevelDim / 2);
                int jYIdx = j - (zoomLevelDim / 2);
                int reqXCenter = currentXCenter + (jYIdx * atlasScale);
                int reqZCenter = currentZCenter + (iXIdx * atlasScale);
                var state = findMapEntryForCenters(
                        mapInfos, currentWorldSelected, reqXCenter, reqZCenter);
                if (state == null) {
                    continue;
                }
                String stateDimStr = MapAtlasesAccessUtils.getMapItemSavedDataDimKey(state.getValue());

                boolean drawPlayerIcons = stateDimStr.compareTo(initialWorldSelected) == 0;
                if (!mapContainsMeaningfulIcons(state)) {
                    drawMap(matrices, i, j, state, mapComponentX, mapComponentY, mapTextureScale, drawPlayerIcons);
                }
            }
        }
        // draw maps without icons first
        // and then draw maps with icons (to avoid drawing over icons)
        for (int i = zoomLevelDim - 1; i >= 0; i--) {
            for (int j = zoomLevelDim - 1; j >= 0; j--) {
                int iXIdx = i - (zoomLevelDim / 2);
                int jYIdx = j - (zoomLevelDim / 2);
                int reqXCenter = currentXCenter + (jYIdx * atlasScale);
                int reqZCenter = currentZCenter + (iXIdx * atlasScale);
                var state = findMapEntryForCenters(
                        mapInfos, currentWorldSelected, reqXCenter, reqZCenter);
                if (state == null) {
                    continue;
                }
                String stateDimStr = MapAtlasesAccessUtils.getMapItemSavedDataDimKey(state.getValue());
                boolean drawPlayerIcons = stateDimStr.compareTo(initialWorldSelected) == 0;
                if (mapContainsMeaningfulIcons(state)) {
                    drawMap(matrices, i, j, state, mapComponentX, mapComponentY, mapTextureScale, drawPlayerIcons);
                }
            }
        }

        // Draw foreground
        context.blit(
                ATLAS_FOREGROUND,
                (int) x,
                (int) y,
                0,
                0,
                atlasBgScaledSize,
                atlasBgScaledSize,
                atlasBgScaledSize,
                atlasBgScaledSize
        );

        // Draw tooltips if necessary
        drawDimensionTooltip(context, x, y, atlasBgScaledSize);
        drawMapDecorationTooltip(context, x, y, atlasBgScaledSize);

        // Draw world map coords
        if (mouseX < x + drawnMapBufferSize || mouseY < y + drawnMapBufferSize
                || mouseX > x + atlasBgScaledSize - drawnMapBufferSize
                || mouseY > y + atlasBgScaledSize - drawnMapBufferSize)
            return;
        if (!MapAtlasesClientConfig.drawWorldMapCoords.get()) return;
        BlockPos cursorBlockPos = getBlockPosForCursor(
                mouseX,
                mouseY,
                zoomLevelDim,
                currentXCenter,
                currentZCenter,
                atlasBgScaledSize,
                x,
                y,
                drawnMapBufferSize
        );
        int targetHeight = atlasBgScaledSize + 4;
        if (MapAtlasesClientConfig.forceWorldMapScaling.get() >= 95) {
            targetHeight = 8;
        }
        float textScaling = (float) (double) MapAtlasesClientConfig.worldMapCoordsScale.get();
        drawMapComponentXZCoords(context, (int) x, (int) y, atlasBgScaledSize, targetHeight, textScaling, cursorBlockPos);
    }

    // ================== Mouse Functions ==================

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0) {
            mouseXOffset += deltaX;
            mouseYOffset += deltaY;
            int targetXCenter = currentXCenter + (round(mouseXOffset, PAN_BUCKET) / PAN_BUCKET * atlasScale * -1);
            int targetZCenter = currentZCenter + (round(mouseYOffset, PAN_BUCKET) / PAN_BUCKET * atlasScale * -1);
            if (targetXCenter != currentXCenter || targetZCenter != currentZCenter) {
                currentXCenter = targetXCenter;
                currentZCenter = targetZCenter;
                mouseXOffset = 0;
                mouseYOffset = 0;
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        // Handle dim selector scroll
        var dims =
                idsToCenters.values().stream().map(Pair::getFirst).collect(Collectors.toSet()).stream().toList();
        int atlasBgScaledSize = getAtlasBgScaledSize();
        double x = (width / 2.0) - (atlasBgScaledSize / 2.0);
        double y = (height / 2.0) - (atlasBgScaledSize / 2.0);
        int scaledWidth = calcScaledWidth(100);
        int targetX = (int) x + (int) (29.5 / 32.0 * atlasBgScaledSize);
        if (mouseX >= targetX && mouseX <= targetX + scaledWidth) {
            dimSelectorOffset =
                    Math.max(0, Math.min(dims.size() - MAX_TAB_DISP, dimSelectorOffset + (amount > 0 ? -1 : 1)));
            return true;
        }
        // Handle map icon selector scroll
        var mapList = getMapDecorationList();
        targetX = (int) x - (int) (1.0 / 16 * atlasBgScaledSize);
        if (mouseX >= targetX && mouseX <= targetX + scaledWidth) {
            mapIconSelectorOffset =
                    Math.max(0, Math.min(mapList.size() - MAX_TAB_DISP, mapIconSelectorOffset + (amount > 0 ? -1 : 1)));
            return true;
        }
        // Handle world map zooming
        double drawnMapBufferSize = atlasBgScaledSize / 18.0;
        if (mouseX < x + drawnMapBufferSize || mouseY < y + drawnMapBufferSize
                || mouseX > x + atlasBgScaledSize - drawnMapBufferSize
                || mouseY > y + atlasBgScaledSize - drawnMapBufferSize)
            return true;
        zoomValue += -1 * amount;
        zoomValue = Math.max(zoomValue, -1 * ZOOM_BUCKET);
        return true;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        rawMouseXMoved = mouseX;
        rawMouseYMoved = mouseY;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (minecraft == null || minecraft.player == null) return false;
        if (button == 0) {
            var dims =
                    idsToCenters.values().stream().map(Pair::getFirst).collect(Collectors.toSet()).stream().toList();
            int atlasBgScaledSize = getAtlasBgScaledSize();
            double x = (width / 2.0) - (atlasBgScaledSize / 2.0);
            double y = (height / 2.0) - (atlasBgScaledSize / 2.0);
            int scaledWidth = calcScaledWidth(100);
            for (int i = 0; i < MAX_TAB_DISP; i++) {
                int targetX = (int) x + (int) (29.5 / 32.0 * atlasBgScaledSize);
                int targetY = (int) y +
                        (int) (i * (4 / 32.0 * atlasBgScaledSize)) + (int) (1.0 / 16.0 * atlasBgScaledSize);
                if (mouseX >= targetX && mouseX <= targetX + scaledWidth
                        && mouseY >= targetY && mouseY <= targetY + scaledWidth) {
                    int targetIdx = dimSelectorOffset + i;
                    if (targetIdx >= dims.size()) {
                        continue;
                    }
                    String newDim = dims.get(targetIdx);
                    currentWorldSelected = newDim;
                    // Set center map coords
                    int[] coords = getCenterMapCoordsForDimension(newDim);
                    currentXCenter = coords[0];
                    currentZCenter = coords[1];
                    // Reset offset & zoom
                    mouseXOffset = 0;
                    mouseYOffset = 0;
                    zoomValue = ZOOM_BUCKET;
                }
            }
            var mapList = getMapDecorationList();
            for (int k = 0; k < MAX_TAB_DISP; k++) {
                int targetX = (int) x - (int) (1.0 / 16 * atlasBgScaledSize);
                int targetY = (int) y + (int) (k * (4 / 32.0 * atlasBgScaledSize)) + (int) (1.0 / 16.0 * atlasBgScaledSize);
                if (mouseX >= targetX && mouseX <= targetX + scaledWidth
                        && mouseY >= targetY && mouseY <= targetY + scaledWidth) {
                    int targetIdx = mapIconSelectorOffset + k;
                    if (targetIdx >= mapList.size()) {
                        continue;
                    }
                    var key = mapList.get(targetIdx).getKey();
                    var stateIdStr = key.split("/")[0];
                    var centers =
                            idsToCenters.get(MapAtlasesAccessUtils.getMapIntFromString(stateIdStr)).getSecond();
                    // Set center map coords
                    currentXCenter = centers.get(0);
                    currentZCenter = centers.get(1);
                    // Reset offset & zoom
                    mouseXOffset = 0;
                    mouseYOffset = 0;
                    zoomValue = ZOOM_BUCKET;
                }
            }
        }
        return true;
    }

    // ================== Drawing Utils ==================

    public void drawMapComponentXZCoords(
            GuiGraphics context,
            int x,
            int y,
            int originOffsetWidth,
            int originOffsetHeight,
            float textScaling,
            BlockPos blockPos
    ) {
        String coordsToDisplay = "X: " + blockPos.getX() + ", Z: " + blockPos.getZ();
        MapAtlasesHUD.drawScaledComponent(
                context, x, y, coordsToDisplay, textScaling, originOffsetWidth, originOffsetHeight);
    }

    private void drawMap(
            PoseStack matrices,
            int i,
            int j,
            Map.Entry<String, MapItemSavedData> state,
            double mapComponentX,
            double mapComponentY,
            float mapTextureScale,
            boolean drawPlayerIcons
    ) {
        if (state == null || minecraft == null) return;
        int zoomLevelDim = getZoomLevelDim();
        boolean isCenterMap = (i == (zoomLevelDim / 2) && j == (zoomLevelDim / 2));
        // Draw the map
        double curMapComponentX = mapComponentX + (mapTextureScale * 128 * j);
        double curMapComponentY = mapComponentY + (mapTextureScale * 128 * i);
        MultiBufferSource.BufferSource vcp;
        vcp = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        matrices.pushPose();
        matrices.translate(curMapComponentX, curMapComponentY, 0.0);
        matrices.scale(mapTextureScale, mapTextureScale, -1);
        // Remove the off-map player icons temporarily during render
        Iterator<Map.Entry<String, MapDecoration>> it = state.getValue().decorations.entrySet().iterator();
        List<Map.Entry<String, MapDecoration>> removed = new ArrayList<>();
        Optional<String> playerIconMapId = getPlayerIconMapId();
        if (playerIconMapId.isEmpty() || state.getKey().compareTo(playerIconMapId.get()) != 0) {
            // Only remove the off-map icon if it's not the active map or its not the active dimension
            while (it.hasNext()) {
                Map.Entry<String, MapDecoration> e = it.next();
                if (!isMeaningfulMapDecoration(e.getValue().getType())
                        || (e.getValue().getType() == MapDecoration.Type.PLAYER && !drawPlayerIcons)) {
                    it.remove();
                    removed.add(e);
                }
            }
        }
        minecraft.gameRenderer.getMapRenderer()
                .render(
                        matrices,
                        vcp,
                        MapAtlasesAccessUtils.getMapIntFromString(state.getKey()),
                        state.getValue(),
                        false,
                        Integer.parseInt("F000F0", 16)
                );
        vcp.endBatch();
        matrices.popPose();
        // Re-add the off-map player icons after render
        for (Map.Entry<String, MapDecoration> e : removed) {
            state.getValue().decorations.put(e.getKey(), e.getValue());
        }
    }

    // ================== Other Util Fns ==================

    private int[] getCenterMapCoordsForDimension(String dim) {
        var dimIdsToCenters = getDimIdsToCenters(dim);
        int centerMap;
        if (dim.compareTo(initialWorldSelected) == 0) {
            centerMap = MapAtlasesAccessUtils.getMapIntFromString(centerMapId);
        } else {
            centerMap = dimIdsToCenters.keySet().stream()
                    .filter(mapId -> {
                        if (minecraft == null || minecraft.level == null) return false;
                        var state = minecraft.level.getMapData(MapAtlasesAccessUtils.getMapStringFromInt(mapId));
                        if (state == null) {
                            return false;
                        }
                        return !state.decorations.entrySet().stream()
                                .filter(e -> e.getValue().getType().isRenderedOnFrame())
                                .collect(Collectors.toSet())
                                .isEmpty();
                    })
                    .findAny().orElseGet(() -> dimIdsToCenters.keySet().stream().findAny().orElseThrow());
        }
        var entry = dimIdsToCenters.get(centerMap).getSecond();
        return new int[]{entry.get(0), entry.get(1)};
    }

    private int getAtlasBgScaledSize() {
        if (minecraft == null) return 16;

        return (int) Math.floor(
                MapAtlasesClientConfig.forceWorldMapScaling.get() / 100.0 * minecraft.getWindow().getGuiScaledHeight());
    }

    private Optional<String> getPlayerIconMapId() {
        if (currentWorldSelected.compareTo(initialWorldSelected) != 0) {
            return Optional.empty();
        }
        Map<String, MapItemSavedData> mapInfos = MapAtlasesAccessUtils.getCurrentDimMapInfoFromAtlas(minecraft.level, atlas);
        int zoomLevelDim = getZoomLevelDim();
        Optional<String> returnVal = Optional.empty();
        double minDist = Double.MAX_VALUE;
        for (int i = zoomLevelDim - 1; i >= 0; i--) {
            for (int j = zoomLevelDim - 1; j >= 0; j--) {
                int iXIdx = i - (zoomLevelDim / 2);
                int jYIdx = j - (zoomLevelDim / 2);
                int reqXCenter = currentXCenter + (jYIdx * atlasScale);
                int reqZCenter = currentZCenter + (iXIdx * atlasScale);
                var state = findMapEntryForCenters(
                        mapInfos, currentWorldSelected, reqXCenter, reqZCenter);
                if (state == null) {
                    continue;
                }
                double dist = Math.hypot(Math.abs(reqXCenter - minecraft.player.getX()), Math.abs(reqZCenter - minecraft.player.getZ()));
                if (dist < minDist) {
                    returnVal = Optional.of(state.getKey());
                    minDist = dist;
                }
            }
        }
        return returnVal;
    }

    private BlockPos getBlockPosForCursor(
            int mouseX,
            int mouseY,
            int zoomLevelDim,
            int centerScreenXCenter,
            int centerScreenZCenter,
            int atlasBgScaledSize,
            double x,
            double y,
            double buffer
    ) {
        double atlasMapsRelativeMouseX = mapRangeValueToAnother(
                mouseX, x + buffer, x + atlasBgScaledSize - buffer, -1.0, 1.0);
        double atlasMapsRelativeMouseZ = mapRangeValueToAnother(
                mouseY, y + buffer, y + atlasBgScaledSize - buffer, -1.0, 1.0);
        return new BlockPos(
                (int) (Math.floor(atlasMapsRelativeMouseX * zoomLevelDim * (atlasScale / 2.0)) + centerScreenXCenter),
                255,
                (int) (Math.floor(atlasMapsRelativeMouseZ * zoomLevelDim * (atlasScale / 2.0)) + centerScreenZCenter));
    }

    private boolean mapContainsMeaningfulIcons(Map.Entry<String, MapItemSavedData> state) {
        return state.getValue().decorations.values().stream()
                .anyMatch(i -> this.isMeaningfulMapDecoration(i.getType()));
    }

    private boolean isMeaningfulMapDecoration(MapDecoration.Type type) {
        return type != MapDecoration.Type.PLAYER_OFF_MAP && type != MapDecoration.Type.PLAYER_OFF_LIMITS;
    }

    private Map.Entry<String, MapItemSavedData> findMapEntryForCenters(
            Map<String, MapItemSavedData> mapInfos,
            String reqDimension,
            int reqXCenter,
            int reqZCenter
    ) {
        return mapInfos.entrySet().stream()
                .filter(infoEntry -> {
                            var mapId = MapAtlasesAccessUtils.getMapIntFromString(infoEntry.getKey());
                            var dimAndCenters = idsToCenters.get(mapId);
                            return idsToCenters.containsKey(mapId)
                                    && dimAndCenters.getFirst().compareTo(reqDimension) == 0
                                    && dimAndCenters.getSecond().get(0) == reqXCenter
                                    && dimAndCenters.getSecond().get(1) == reqZCenter;
                        }
                )
                .findFirst().orElse(null);
    }

    private int getZoomLevelDim() {
        int zoomLevel = round(zoomValue, ZOOM_BUCKET) / ZOOM_BUCKET;
        zoomLevel = Math.max(zoomLevel, 0);
        return (2 * zoomLevel) + 1;
    }

    private Map<Integer, Pair<String, List<Integer>>> getDimIdsToCenters(String worldKey) {
        return idsToCenters.entrySet().stream()
                .filter(t -> t.getValue().getFirst().compareTo(worldKey) == 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (m1, m2) -> m1));
    }

    private double mapRangeValueToAnother(
            double input, double inputStart, double inputEnd, double outputStart, double outputEnd) {
        double slope = (outputEnd - outputStart) / (inputEnd - inputStart);
        return outputStart + slope * (input - inputStart);
    }

    private int round(int num, int mod) {
        int t = num % mod;
        if (t < (int) Math.floor(mod / 2.0))
            return num - t;
        else
            return num + mod - t;
    }

    private int calcScaledWidth(int rawWidth) {
        if (minecraft == null) return 0;
        return rawWidth * minecraft.getWindow().getGuiScaledHeight() / 1080;
    }

    private String firstCharCapitalize(String source) {
        char[] array = source.toLowerCase(Locale.ROOT).toCharArray();
        array[0] = Character.toUpperCase(array[0]);
        for (int j = 1; j < array.length; j++) {
            if (Character.isWhitespace(array[j - 1])) {
                array[j] = Character.toUpperCase(array[j]);
            }
        }
        return new String(array);
    }

    // ================== Dimension Selectors ==================

    private void drawDimensionTooltip(
            GuiGraphics context,
            double x,
            double y,
            int atlasBgScaledSize
    ) {
        var dimensions =
                idsToCenters.values().stream().map(Pair::getFirst).collect(Collectors.toSet()).stream().toList();
        int scaledWidth = calcScaledWidth(100);
        for (int i = 0; i < MAX_TAB_DISP; i++) {
            if (rawMouseXMoved >= (x + (int) (29.5 / 32.0 * atlasBgScaledSize))
                    && rawMouseXMoved <= (x + (int) (29.5 / 32.0 * atlasBgScaledSize) + scaledWidth)
                    && rawMouseYMoved >= (y + (int) (i * (4 / 32.0 * atlasBgScaledSize)) + (int) (1.0 / 16.0 * atlasBgScaledSize))
                    && rawMouseYMoved <= (y + (int) (i * (4 / 32.0 * atlasBgScaledSize)) + (int) (1.0 / 16.0 * atlasBgScaledSize)) + scaledWidth) {
                int targetIdx = dimSelectorOffset + i;
                if (targetIdx >= dimensions.size()) {
                    continue;
                }
                ResourceLocation dimRegistry = new ResourceLocation(dimensions.get(targetIdx));
                String dimName;
                if (dimRegistry.getNamespace().compareTo("minecraft") == 0) {
                    dimName = dimRegistry.getPath().toString().replace("_", " ");
                } else {
                    dimName = dimRegistry.toString().toString().replace("_", " ").replace(":", " ");
                }
                dimName = firstCharCapitalize(dimName);
                assert minecraft != null;
                context.renderTooltip(minecraft.font, Component.literal(dimName), (int) rawMouseXMoved, (int) rawMouseYMoved);
                return;
            }
        }
    }

    private void drawDimensionSelectors(
            GuiGraphics context,
            double x,
            double y,
            int atlasBgScaledSize
    ) {
        var dimensions =
                idsToCenters.values().stream().map(Pair::getFirst).collect(Collectors.toSet()).stream().toList();
        int scaledWidth;
        for (int i = 0; i < MAX_TAB_DISP; i++) {
            int targetIdx = dimSelectorOffset + i;
            if (targetIdx >= dimensions.size()) {
                continue;
            }
            var dim = dimensions.get(targetIdx);
            scaledWidth = calcScaledWidth(100);
            // Draw selector
            ResourceLocation selectionPage = (dim.compareTo(currentWorldSelected) == 0) ? PAGE_SELECTED : PAGE_UNSELECTED;
            context.blit(
                    selectionPage,
                    (int) x + (int) (29.5 / 32.0 * atlasBgScaledSize),
                    (int) y + (int) (i * (4 / 32.0 * atlasBgScaledSize)) + (int) (1.0 / 16.0 * atlasBgScaledSize),
                    0,
                    0,
                    scaledWidth,
                    scaledWidth,
                    scaledWidth,
                    scaledWidth
            );
            // Draw Icon
            ResourceLocation dimensionPage;
            if (dim.compareTo("minecraft:overworld") == 0) {
                dimensionPage = PAGE_OVERWORLD;
            } else if (dim.compareTo("minecraft:the_nether") == 0) {
                dimensionPage = PAGE_NETHER;
            } else if (dim.compareTo("minecraft:the_end") == 0) {
                dimensionPage = PAGE_END;
            } else {
                dimensionPage = PAGE_OTHER;
            }
            scaledWidth = calcScaledWidth(75);
            context.blit(
                    dimensionPage,
                    (int) x + (int) (30.0 / 32.0 * atlasBgScaledSize),
                    (int) y + (int) (i * (4 / 32.0 * atlasBgScaledSize)) + (int) (4.0 / 64.0 * atlasBgScaledSize),
                    0,
                    0,
                    scaledWidth,
                    scaledWidth,
                    scaledWidth,
                    scaledWidth
            );
        }
    }

    // ================== Map Icon Selectors ==================

    private List<Map.Entry<String, MapDecoration>> getMapDecorationList() {
        var currentIdsToCenters = getDimIdsToCenters(currentWorldSelected);
        var mapInfos = MapAtlasesAccessUtils.getAllMapInfoFromAtlas(minecraft.level, atlas);
        // Map of <"MapItemSavedDataId/MapDecorationId": MapDecoration>
        Map<String, MapDecoration> mapIcons = new HashMap<>();
        for (var e : currentIdsToCenters.entrySet()) {
            var centers = e.getValue().getSecond();
            var state = findMapEntryForCenters(
                    mapInfos, currentWorldSelected, centers.get(0), centers.get(1));
            if (state == null) continue;
            var fullIcons = state.getValue().decorations;
            var stateString = state.getKey();
            var keptIcons = fullIcons.entrySet().stream()
                    .filter(t -> t.getValue().getType().isRenderedOnFrame())
                    .map(t -> new AbstractMap.SimpleEntry<>(stateString + "/" + t.getKey(), t.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            mapIcons.putAll(keptIcons);
        }
        return mapIcons.entrySet().stream().toList();
    }

    private void drawMapDecorationSelectors(GuiGraphics context, double x, double y, int atlasBgScaledSize) {
        if (minecraft == null) return;
        int scaledWidth = calcScaledWidth(100);
        var mapList = getMapDecorationList();
        for (int k = 0; k < MAX_TAB_DISP; k++) {
            int targetIdx = mapIconSelectorOffset + k;
            if (targetIdx >= mapList.size()) {
                continue;
            }
            var matrices = context.pose();
            var mapIconE = mapList.get(targetIdx);
            // Draw selector
            var mapIdStr = mapIconE.getKey().split("/")[0];
            var centers = idsToCenters.get(MapAtlasesAccessUtils.getMapIntFromString(mapIdStr)).getSecond();
            if (currentXCenter == centers.get(0) && currentZCenter == centers.get(1)) {
                RenderSystem.setShaderTexture(0, PAGE_SELECTED);
            } else {
                RenderSystem.setShaderTexture(0, PAGE_UNSELECTED);
            }
            drawTextureFlippedX(
                    context,
                    (int) x - (int) (1.0 / 16 * atlasBgScaledSize),
                    (int) y + (int) (k * (4 / 32.0 * atlasBgScaledSize)) + (int) (1.0 / 16.0 * atlasBgScaledSize),
                    0,
                    0,
                    scaledWidth,
                    scaledWidth,
                    scaledWidth,
                    scaledWidth
            );

            // Draw map Icon
            var mapIcon = mapIconE.getValue();
            matrices.pushPose();
            matrices.translate(
                    x,
                    (int) y + (int) (k * (4 / 32.0 * atlasBgScaledSize)) + (int) (1.75 / 16.0 * atlasBgScaledSize),
                    1
            );
            matrices.mulPose(Axis.ZP.rotationDegrees((float) (mapIcon.getRot() * 360) / 16.0F));
            matrices.scale((0.25f * scaledWidth), (0.25f * scaledWidth), 1);
            matrices.translate(-0.125D, 0.125D, -1.0D);
            byte b = mapIcon.getImage();
            float g = (float) (b % 16 + 0) / 16.0F;
            float h = (float) (b / 16 + 0) / 16.0F;
            float l = (float) (b % 16 + 1) / 16.0F;
            float m = (float) (b / 16 + 1) / 16.0F;
            Matrix4f matrix4f2 = matrices.last().pose();
            int light = 0xF000F0;
            MultiBufferSource.BufferSource vcp = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
            VertexConsumer vertexConsumer2 = vcp.getBuffer(MAP_ICONS);
            vertexConsumer2.vertex(matrix4f2, -1.0F, 1.0F, (float) k * 0.001F)
                    .color(255, 255, 255, 255).uv(g, h).uv2(light).endVertex();
            vertexConsumer2.vertex(matrix4f2, 1.0F, 1.0F, (float) k * 0.002F)
                    .color(255, 255, 255, 255).uv(l, h).uv2(light).endVertex();
            vertexConsumer2.vertex(matrix4f2, 1.0F, -1.0F, (float) k * 0.003F)
                    .color(255, 255, 255, 255).uv(l, m).uv2(light).endVertex();
            vertexConsumer2.vertex(matrix4f2, -1.0F, -1.0F, (float) k * 0.004F)
                    .color(255, 255, 255, 255).uv(g, m).uv2(light).endVertex();
            vcp.endBatch();
            matrices.popPose();
        }
    }

    private void drawTextureFlippedX(GuiGraphics context, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        var matrices = context.pose();
        matrices.pushPose();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        float u0 = (u + (float) width) / (float) textureWidth;
        float u1 = (u + 0.0F) / (float) textureWidth;
        float v0 = (v + 0.0F) / (float) textureHeight;
        float v1 = (v + (float) height) / (float) textureHeight;
        bufferBuilder.vertex(matrices.last().pose(), (float) x, (float) y + height, 0.00001F).uv(u0, v1).endVertex();
        bufferBuilder.vertex(matrices.last().pose(), (float) x + width, (float) y + height, 0.00002F).uv(u1, v1).endVertex();
        bufferBuilder.vertex(matrices.last().pose(), (float) x + width, (float) y, 0.00003F).uv(u1, v0).endVertex();
        bufferBuilder.vertex(matrices.last().pose(), (float) x, (float) y, 0.00004F).uv(u0, v0).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
        matrices.popPose();
    }

    private void drawMapDecorationTooltip(
            GuiGraphics context,
            double x,
            double y,
            int atlasBgScaledSize
    ) {
        int scaledWidth = calcScaledWidth(100);
        var mapList = getMapDecorationList();
        for (int k = 0; k < MAX_TAB_DISP; k++) {
            int targetIdx = mapIconSelectorOffset + k;
            if (targetIdx >= mapList.size()) {
                continue;
            }
            var entry = mapList.get(targetIdx);
            var stateIdStr = entry.getKey().split("/")[0];
            var stateId = MapAtlasesAccessUtils.getMapIntFromString(stateIdStr);
            var dimAndCenters = idsToCenters.get(stateId);
            var mapState = minecraft.level.getMapData(stateIdStr);
            if (mapState == null) continue;
            var mapIcon = entry.getValue();
            var mapIconComponent = mapIcon.getName() == null
                    ? MutableComponent.create(new LiteralContents(
                    firstCharCapitalize(mapIcon.getType().name().replace("_", " "))))
                    : mapIcon.getName();
            if (rawMouseXMoved >= (int) x - (int) (1.0 / 16 * atlasBgScaledSize)
                    && rawMouseXMoved <= (int) x - (int) (1.0 / 16 * atlasBgScaledSize) + scaledWidth
                    && rawMouseYMoved >= (int) y + (int) (k * (4 / 32.0 * atlasBgScaledSize)) + (int) (1.0 / 16.0 * atlasBgScaledSize)
                    && rawMouseYMoved <= (int) y + (int) (k * (4 / 32.0 * atlasBgScaledSize)) + (int) (1.0 / 16.0 * atlasBgScaledSize) + scaledWidth) {
                // draw text
                LiteralContents coordsComponent = new LiteralContents(
                        "X: " + (int) (dimAndCenters.getSecond().get(0) - (atlasScale / 2.0d) + ((atlasScale / 2.0d) * ((mapIcon.getX() + 128) / 128.0d)))
                                + ", Z: "
                                + (int) (dimAndCenters.getSecond().get(1) - (atlasScale / 2.0d) + ((atlasScale / 2.0d) * ((mapIcon.getY() + 128) / 128.0d)))
                );
                MutableComponent formattedCoords = MutableComponent.create(coordsComponent).withStyle(ChatFormatting.GRAY);
                context.renderComponentTooltip(minecraft.font, List.of(mapIconComponent, formattedCoords), (int) rawMouseXMoved, (int) rawMouseYMoved);
                return;
            }
        }
    }
}