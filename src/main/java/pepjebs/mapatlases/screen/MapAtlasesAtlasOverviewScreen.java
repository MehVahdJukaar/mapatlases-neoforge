package pepjebs.mapatlases.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.client.ui.MapAtlasesHUD;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.*;
import java.util.stream.Collectors;

// TODO: If the atlas world map scaling changes, MAX_TAB_DISP needs to change too
// TODO: Map Icon Selectors don't look right at non-default scaling
public class MapAtlasesAtlasOverviewScreen extends Screen {

    public static final ResourceLocation ATLAS_FOREGROUND =
            MapAtlasesMod.res("textures/gui/screen/atlas_foreground.png");
    public static final ResourceLocation ATLAS_BACKGROUND =
            MapAtlasesMod.res("textures/gui/screen/atlas_background.png");
    public static final ResourceLocation PAGE_SELECTED =
            MapAtlasesMod.res("textures/gui/screen/page_selected.png");
    public static final ResourceLocation PAGE_UNSELECTED =
            MapAtlasesMod.res("textures/gui/screen/page_unselected.png");
    public static final ResourceLocation PAGE_OVERWORLD =
            MapAtlasesMod.res("textures/gui/screen/overworld_atlas_page.png");
    public static final ResourceLocation PAGE_NETHER =
            MapAtlasesMod.res("textures/gui/screen/nether_atlas_page.png");
    public static final ResourceLocation PAGE_END =
            MapAtlasesMod.res("textures/gui/screen/end_atlas_page.png");
    public static final ResourceLocation PAGE_OTHER =
            MapAtlasesMod.res("textures/gui/screen/unknown_atlas_page.png");
    public static final ResourceLocation MAP_ICON_TEXTURE = new ResourceLocation("textures/map/map_icons.png");
    private static final Map<ResourceKey<Level>, ResourceLocation> DIMENSION_TEXTURES = Map.of(
            Level.OVERWORLD, PAGE_OVERWORLD,
            Level.NETHER, PAGE_NETHER,
            Level.END, PAGE_END
    );

    private static final RenderType MAP_ICONS = RenderType.text(MAP_ICON_TEXTURE);
    private static final int ZOOM_BUCKET = 4;
    private static final int PAN_BUCKET = 25;
    private static final int MAX_TAB_DISP = 7;

    private final ItemStack atlas;
    private final Player player;
    private final Level level;
    private final int atlasScale;
    private final List<ResourceKey<Level>> dimensions = new ArrayList<>();

    // height and width can change so can these
    private int atlasBgScaledSize;
    private int leftPos;
    private int topPos;


    //remove?
    private final ResourceKey<Level> initialWorldSelected;
    private final MapItemSavedData initialMapSelected;


    private final Map<ResourceKey<Level>, List<Pair<String, MapItemSavedData>>> dimToData = new HashMap<>();
    private final Map<Pair<Integer, Integer>, Pair<String, MapItemSavedData>> byCenter = new HashMap<>();

    //usually player cant change this data while on the screen. however if it was moved it might

    private ResourceKey<Level> currentWorldSelected;
    private int mouseXOffset = 0;
    private int mouseYOffset = 0;
    private int currentXCenter;
    private int currentZCenter;
    private double rawMouseXMoved = 0;
    private double rawMouseYMoved = 0;
    private int zoomValue = ZOOM_BUCKET;
    private int mapIconSelectorOffset = 0;
    private int dimSelectorOffset = 0;

    public MapAtlasesAtlasOverviewScreen(Component title, ItemStack atlas) {
        super(title);
        this.atlas = atlas;

        this.level = Minecraft.getInstance().level;
        this.player = Minecraft.getInstance().player;

        initialWorldSelected = player.level().dimension();
        currentWorldSelected = initialWorldSelected;

        tick();

        initialMapSelected = MapAtlasesAccessUtils.getClosestMapData(dimToData.get(currentWorldSelected), player)
                .getSecond();

        atlasScale = (1 << initialMapSelected.scale) * 128;
        currentXCenter = initialMapSelected.centerX;
        currentZCenter = initialMapSelected.centerZ;

        // Play open sound
        player.playSound(MapAtlasesMod.ATLAS_OPEN_SOUND_EVENT.get(),
                (float) (double) MapAtlasesClientConfig.soundScalar.get(), 1.0F);

    }

    @Override
    protected void init() {
        super.init();
        tick();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        //recalculate parameters
        atlasBgScaledSize = (int) Math.floor(MapAtlasesClientConfig.forceWorldMapScaling.get() / 100.0 * height);

        dimToData.clear();
        dimToData.putAll(MapAtlasesAccessUtils.getAllMapDataByDimension(level, atlas));
        leftPos = (int) ((width / 2.0) - (atlasBgScaledSize / 2.0));
        topPos = (int) ((height / 2.0) - (atlasBgScaledSize / 2.0));
        dimensions.clear();
        dimensions.addAll(dimToData.keySet());

        byCenter.clear();
        for (var p : dimToData.get(currentWorldSelected)) {
            MapItemSavedData data = p.getSecond();
            byCenter.put(Pair.of(data.centerX, data.centerZ), p);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics);

        PoseStack matrices = graphics.pose();

        // Handle zooming
        int drawnMapBufferSize = (int) (atlasBgScaledSize / 18.0);
        int atlasDataScaledSize = atlasBgScaledSize - (2 * drawnMapBufferSize);
        int zoomLevelDim = getZoomLevelDim();
        MapAtlasesClient.setWorldMapZoomLevel(zoomLevelDim * (float) (double) MapAtlasesClientConfig.worldMapDecorationScale.get());
        float mapTextureScale = (float) (atlasDataScaledSize / (128.0 * zoomLevelDim));

        // Draw map background
        //TODO: check config
        graphics.blit(
                ATLAS_BACKGROUND,
                leftPos,
                topPos,
                0,
                0,
                atlasBgScaledSize,
                atlasBgScaledSize,
                atlasBgScaledSize,
                atlasBgScaledSize
        );

        // Draw selectors
        drawDimensionSelectors(graphics);
        drawMapDecorationSelectors(graphics);


        matrices.pushPose();
        // Draw maps, putting active map in middle of grid
        int mapComponentX = leftPos + drawnMapBufferSize;
        int mapComponentY = topPos + drawnMapBufferSize;

        matrices.translate(mapComponentX, mapComponentY, 0);

        MultiBufferSource.BufferSource vcp = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        graphics.enableScissor(mapComponentX, mapComponentY,
                 (mapComponentX + atlasDataScaledSize),  (mapComponentY + atlasDataScaledSize));
        MapItemSavedData playerMap = getClosestMapToPlayer();
        matrices.scale(mapTextureScale, mapTextureScale, -1);

        if (playerMap != null) {
            matrices.translate(
                    (playerMap.centerX - player.getX()),
                    (playerMap.centerZ - player.getZ()),
                    0);
        }

        for (int i = zoomLevelDim - 1; i >= 0; i--) {
            for (int j = zoomLevelDim - 1; j >= 0; j--) {
                int iXIdx = i - (zoomLevelDim / 2);
                int jYIdx = j - (zoomLevelDim / 2);
                int reqXCenter = currentXCenter + (jYIdx * atlasScale);
                int reqZCenter = currentZCenter + (iXIdx * atlasScale);
                Pair<String, MapItemSavedData> state = findMapEntryForCenter(reqXCenter, reqZCenter);
                if (state == null) {
                    continue;
                }
                MapItemSavedData data = state.getSecond();
                boolean drawPlayerIcons = data.dimension.equals(player.level().dimension());
                drawPlayerIcons = drawPlayerIcons && playerMap == state.getSecond();
                drawMap(matrices, vcp, i, j, state, drawPlayerIcons);
            }
        }

        vcp.endBatch();
        graphics.disableScissor();
        matrices.popPose();

        // Draw foreground
        graphics.blit(
                ATLAS_FOREGROUND,
                leftPos,
                topPos,
                0,
                0,
                atlasBgScaledSize,
                atlasBgScaledSize,
                atlasBgScaledSize,
                atlasBgScaledSize
        );

        // Draw tooltips if necessary
        drawDimensionTooltip(graphics);
        drawMapDecorationTooltip(graphics);

        // Draw world map coords
        if (mouseX < leftPos + drawnMapBufferSize || mouseY < topPos + drawnMapBufferSize
                || mouseX > leftPos + atlasBgScaledSize - drawnMapBufferSize
                || mouseY > topPos + atlasBgScaledSize - drawnMapBufferSize)
            return;
        if (!MapAtlasesClientConfig.drawWorldMapCoords.get()) return;
        BlockPos cursorBlockPos = getBlockPosForCursor(
                mouseX,
                mouseY,
                zoomLevelDim,
                currentXCenter,
                currentZCenter,
                atlasBgScaledSize,
                leftPos,
                topPos,
                drawnMapBufferSize
        );
        int targetHeight = atlasBgScaledSize + 4;
        if (MapAtlasesClientConfig.forceWorldMapScaling.get() >= 95) {
            targetHeight = 8;
        }
        float textScaling = (float) (double) MapAtlasesClientConfig.worldMapCoordsScale.get();
        drawMapComponentXZCoords(graphics, leftPos, topPos, atlasBgScaledSize,
                targetHeight, textScaling, cursorBlockPos);
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

        int scaledWidth = calcScaledWidth(100);
        int targetX = leftPos + (int) (29.5 / 32.0 * atlasBgScaledSize);
        if (mouseX >= targetX && mouseX <= targetX + scaledWidth) {
            dimSelectorOffset =
                    Math.max(0, Math.min(dimensions.size() - MAX_TAB_DISP, dimSelectorOffset + (amount > 0 ? -1 : 1)));
            return true;
        }
        // Handle map icon selector scroll
        List<Pair<MapItemSavedData, MapDecoration>> decorationList = getMapDecorationList();
        targetX = leftPos - (int) (1.0 / 16 * atlasBgScaledSize);
        if (mouseX >= targetX && mouseX <= targetX + scaledWidth) {
            mapIconSelectorOffset =
                    Math.max(0, Math.min(decorationList.size() - MAX_TAB_DISP, mapIconSelectorOffset + (amount > 0 ? -1 : 1)));
            return true;
        }
        // Handle world map zooming
        double drawnMapBufferSize = atlasBgScaledSize / 18.0;
        if (mouseX < leftPos + drawnMapBufferSize || mouseY < topPos + drawnMapBufferSize
                || mouseX > leftPos + atlasBgScaledSize - drawnMapBufferSize
                || mouseY > topPos + atlasBgScaledSize - drawnMapBufferSize)
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
        if (button == 0) {
            int scaledWidth = calcScaledWidth(100);
            for (int i = 0; i < MAX_TAB_DISP; i++) {
                int targetX = leftPos + (int) (29.5 / 32.0 * atlasBgScaledSize);
                int targetY = topPos +
                        (int) (i * (4 / 32.0 * atlasBgScaledSize)) + (int) (1.0 / 16.0 * atlasBgScaledSize);
                if (mouseX >= targetX && mouseX <= targetX + scaledWidth
                        && mouseY >= targetY && mouseY <= targetY + scaledWidth) {
                    int targetIdx = dimSelectorOffset + i;
                    if (targetIdx >= dimensions.size()) {
                        continue;
                    }
                    currentWorldSelected = dimensions.get(targetIdx);
                    // Set center map coords
                    MapItemSavedData center = getCenterMapForSelectedDim();
                    currentXCenter = center.centerX;
                    currentZCenter = center.centerZ;
                    // Reset offset & zoom
                    mouseXOffset = 0;
                    mouseYOffset = 0;
                    zoomValue = ZOOM_BUCKET;
                }
            }
            List<Pair<MapItemSavedData, MapDecoration>> decorationList = getMapDecorationList();
            for (int k = 0; k < MAX_TAB_DISP; k++) {
                int targetX = leftPos - (int) (1.0 / 16 * atlasBgScaledSize);
                int targetY = topPos + (int) (k * (4 / 32.0 * atlasBgScaledSize)) + (int) (1.0 / 16.0 * atlasBgScaledSize);
                if (mouseX >= targetX && mouseX <= targetX + scaledWidth
                        && mouseY >= targetY && mouseY <= targetY + scaledWidth) {
                    int targetIdx = mapIconSelectorOffset + k;
                    if (targetIdx >= decorationList.size()) {
                        continue;
                    }
                    MapItemSavedData targetData = decorationList.get(targetIdx).getFirst();
                    // Set center map coords
                    currentXCenter = targetData.centerX;
                    currentZCenter = targetData.centerZ;
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
            MultiBufferSource.BufferSource vcp,
            int i,
            int j,
            Pair<String, MapItemSavedData> state,
            boolean drawPlayerIcons
    ) {
        // Draw the map
        double curMapComponentX = (128 * j);
        double curMapComponentY = (128 * i);
        matrices.pushPose();
        matrices.translate(curMapComponentX, curMapComponentY, 0.0);

        // Remove the off-map player icons temporarily during render
        MapItemSavedData data = state.getSecond();
        Iterator<Map.Entry<String, MapDecoration>> it = data.decorations.entrySet().iterator();
        List<Map.Entry<String, MapDecoration>> removed = new ArrayList<>();
        // Only remove the off-map icon if it's not the active map, or it's not the active dimension
        while (it.hasNext()) {
            Map.Entry<String, MapDecoration> e = it.next();
            var type = e.getValue().getType();
            if (type == MapDecoration.Type.PLAYER_OFF_MAP || type == MapDecoration.Type.PLAYER_OFF_LIMITS
                    || (type == MapDecoration.Type.PLAYER && !drawPlayerIcons)) {
                it.remove();
                removed.add(e);
            }
        }
        minecraft.gameRenderer.getMapRenderer()
                .render(
                        matrices,
                        vcp,
                        MapAtlasesAccessUtils.getMapIntFromString(state.getFirst()),
                        data,
                        false,
                        0xF000F0
                );
        matrices.popPose();
        // Re-add the off-map player icons after render
        for (Map.Entry<String, MapDecoration> e : removed) {
            data.decorations.put(e.getKey(), e.getValue());
        }
    }

    // ================== Other Util Fns ==================

    private MapItemSavedData getCenterMapForSelectedDim() {
        if (currentWorldSelected.equals(initialWorldSelected)) {
            return initialMapSelected;
        } else {
            //centers to any map that has decoration
            return dimToData.get(currentWorldSelected).stream()
                    .filter(state -> !state.getSecond().decorations.entrySet().stream()
                            .filter(e -> e.getValue().getType().isRenderedOnFrame())
                            .collect(Collectors.toSet())
                            .isEmpty())
                    .findAny().orElseGet(() -> dimToData.get(currentWorldSelected)
                            .stream().findAny().orElseThrow()).getSecond();
        }
    }

    @Nullable
    private MapItemSavedData getClosestMapToPlayer() {
        if (!currentWorldSelected.equals(player.level().dimension())) {
            return null;
        }
        Pair<String, MapItemSavedData> returnVal = null;
        double minDist = Double.MAX_VALUE;
        for (var e : byCenter.entrySet()) {
            var p = e.getKey();
            double dist = Mth.square(p.getFirst() - player.getX()) + Mth.square(p.getSecond() - player.getZ());
            if (dist < minDist) {
                returnVal = e.getValue();
                minDist = dist;
            }
        }
        return returnVal == null ? null : returnVal.getSecond();
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
        double atlasMapsRelativeMouseX = Mth.map(
                mouseX, x + buffer, x + atlasBgScaledSize - buffer, -1.0, 1.0);
        double atlasMapsRelativeMouseZ = Mth.map(
                mouseY, y + buffer, y + atlasBgScaledSize - buffer, -1.0, 1.0);
        return new BlockPos(
                (int) (Math.floor(atlasMapsRelativeMouseX * zoomLevelDim * (atlasScale / 2.0)) + centerScreenXCenter),
                255,
                (int) (Math.floor(atlasMapsRelativeMouseZ * zoomLevelDim * (atlasScale / 2.0)) + centerScreenZCenter));
    }

    @Nullable
    private Pair<String, MapItemSavedData> findMapEntryForCenter(int reqXCenter, int reqZCenter) {
        return byCenter.get(Pair.of(reqXCenter, reqZCenter));
    }

    private int getZoomLevelDim() {
        int zoomLevel = round(zoomValue, ZOOM_BUCKET) / ZOOM_BUCKET;
        zoomLevel = Math.max(zoomLevel, 0);
        return (2 * zoomLevel) + 1;
    }

    private int round(int num, int mod) {
        //return Math.round((float) num / mod) * mod
        int t = num % mod;
        if (t < (int) Math.floor(mod / 2.0))
            return num - t;
        else
            return num + mod - t;
    }

    private int calcScaledWidth(int rawWidth) {
        return rawWidth * height / 1080;
    }

    private String getReadableName(ResourceLocation id) {
        String s = id.toShortLanguageKey();
        return getReadableName(s);
    }

    @NotNull
    private static String getReadableName(String s) {
        s = s.replace(".", " ").replace("_", " ");
        char[] array = s.toCharArray();
        array[0] = Character.toUpperCase(array[0]);
        for (int j = 1; j < array.length; j++) {
            if (Character.isWhitespace(array[j - 1])) {
                array[j] = Character.toUpperCase(array[j]);
            }
        }
        return new String(array);
    }

    // ================== Dimension Selectors ==================

    private void drawDimensionTooltip(GuiGraphics context) {
        int scaledWidth = calcScaledWidth(100);
        for (int i = 0; i < MAX_TAB_DISP; i++) {
            if (rawMouseXMoved >= (leftPos + (int) (29.5 / 32.0 * atlasBgScaledSize))
                    && rawMouseXMoved <= (leftPos + (int) (29.5 / 32.0 * atlasBgScaledSize) + scaledWidth)
                    && rawMouseYMoved >= (topPos + (int) (i * (4 / 32.0 * atlasBgScaledSize)) + (int) (1.0 / 16.0 * atlasBgScaledSize))
                    && rawMouseYMoved <= (topPos + (int) (i * (4 / 32.0 * atlasBgScaledSize)) + (int) (1.0 / 16.0 * atlasBgScaledSize)) + scaledWidth) {
                int targetIdx = dimSelectorOffset + i;
                if (targetIdx >= dimensions.size()) {
                    continue;
                }
                ResourceLocation dimId = dimensions.get(targetIdx).location();
                String dimName = getReadableName(dimId);
                context.renderTooltip(font, Component.literal(dimName), (int) rawMouseXMoved, (int) rawMouseYMoved);
                return;
            }
        }
    }

    private void drawDimensionSelectors(GuiGraphics context) {
        int scaledWidth;
        for (int i = 0; i < MAX_TAB_DISP; i++) {
            int targetIdx = dimSelectorOffset + i;
            if (targetIdx >= dimensions.size()) {
                continue;
            }
            ResourceKey<Level> dim = dimensions.get(targetIdx);
            scaledWidth = calcScaledWidth(100);
            // Draw selector
            ResourceLocation selectionPage = (dim.compareTo(currentWorldSelected) == 0) ? PAGE_SELECTED : PAGE_UNSELECTED;
            context.blit(
                    selectionPage,
                    leftPos + (int) (29.5 / 32.0 * atlasBgScaledSize),
                    topPos + (int) (i * (4 / 32.0 * atlasBgScaledSize)) + (int) (1.0 / 16.0 * atlasBgScaledSize),
                    0,
                    0,
                    scaledWidth,
                    scaledWidth,
                    scaledWidth,
                    scaledWidth
            );
            // Draw Icon
            ResourceLocation dimensionPage = DIMENSION_TEXTURES.getOrDefault(dim, PAGE_OTHER);
            scaledWidth = calcScaledWidth(75);
            context.blit(
                    dimensionPage,
                    leftPos + (int) (30.0 / 32.0 * atlasBgScaledSize),
                    topPos + (int) (i * (4 / 32.0 * atlasBgScaledSize)) + (int) (4.0 / 64.0 * atlasBgScaledSize),
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

    private List<Pair<MapItemSavedData, MapDecoration>> getMapDecorationList() {
        List<Pair<String, MapItemSavedData>> currentData = dimToData.get(currentWorldSelected);
        List<Pair<MapItemSavedData, MapDecoration>> mapIcons = new ArrayList<>();
        for (var p : currentData) {
            MapItemSavedData data = p.getSecond();
            for (var e : data.decorations.entrySet()) {
                if (e.getValue().renderOnFrame()) {
                    mapIcons.add(Pair.of(data, e.getValue()));
                }
            }
        }
        return mapIcons;
    }

    private void drawMapDecorationSelectors(GuiGraphics context) {
        int scaledWidth = calcScaledWidth(100);
        List<Pair<MapItemSavedData, MapDecoration>> mapDecorationList = getMapDecorationList();
        for (int k = 0; k < MAX_TAB_DISP; k++) {
            int targetIdx = mapIconSelectorOffset + k;
            if (targetIdx >= mapDecorationList.size()) {
                continue;
            }
            PoseStack matrices = context.pose();
            Pair<MapItemSavedData, MapDecoration> decorationPair = mapDecorationList.get(targetIdx);
            // Draw selector
            MapItemSavedData decorationData = decorationPair.getFirst();
            if (currentXCenter == decorationData.centerX && currentZCenter == decorationData.centerZ) {
                RenderSystem.setShaderTexture(0, PAGE_SELECTED);
            } else {
                RenderSystem.setShaderTexture(0, PAGE_UNSELECTED);
            }
            drawTextureFlippedX(
                    context,
                    leftPos - (int) (1.0 / 16 * atlasBgScaledSize),
                    topPos + (int) (k * (4 / 32.0 * atlasBgScaledSize)) + (int) (1.0 / 16.0 * atlasBgScaledSize),
                    0,
                    0,
                    scaledWidth,
                    scaledWidth,
                    scaledWidth,
                    scaledWidth
            );

            // Draw map Icon
            MapDecoration mapIcon = decorationPair.getSecond();

            matrices.pushPose();
            matrices.translate(
                    leftPos,
                    topPos + (int) (k * (4 / 32.0 * atlasBgScaledSize)) + (int) (1.75 / 16.0 * atlasBgScaledSize),
                    1
            );

            matrices.mulPose(Axis.ZP.rotationDegrees((mapIcon.getRot() * 360) / 16.0F));
            matrices.scale((0.25f * scaledWidth), (0.25f * scaledWidth), 1);
            matrices.translate(-0.125D, 0.125D, -1.0D);
            byte b = mapIcon.getImage();
            float g = (b % 16 + 0) / 16.0F;
            float h = (b / 16 + 0) / 16.0F;
            float l = (b % 16 + 1) / 16.0F;
            float m = (b / 16 + 1) / 16.0F;
            Matrix4f matrix4f2 = matrices.last().pose();
            int light = 0xF000F0;
            MultiBufferSource.BufferSource vcp = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
            VertexConsumer vertexConsumer2 = vcp.getBuffer(MAP_ICONS);
            vertexConsumer2.vertex(matrix4f2, -1.0F, 1.0F, k * 0.001F)
                    .color(255, 255, 255, 255).uv(g, h).uv2(light).endVertex();
            vertexConsumer2.vertex(matrix4f2, 1.0F, 1.0F, k * 0.002F)
                    .color(255, 255, 255, 255).uv(l, h).uv2(light).endVertex();
            vertexConsumer2.vertex(matrix4f2, 1.0F, -1.0F, k * 0.003F)
                    .color(255, 255, 255, 255).uv(l, m).uv2(light).endVertex();
            vertexConsumer2.vertex(matrix4f2, -1.0F, -1.0F, k * 0.004F)
                    .color(255, 255, 255, 255).uv(g, m).uv2(light).endVertex();
            vcp.endBatch();
            matrices.popPose();
        }
    }

    //TODO:use gui graphics
    private void drawTextureFlippedX(GuiGraphics context, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        var matrices = context.pose();
        matrices.pushPose();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        float u0 = (u + width) / textureWidth;
        float u1 = (u + 0.0F) / textureWidth;
        float v0 = (v + 0.0F) / textureHeight;
        float v1 = (v + height) / textureHeight;
        bufferBuilder.vertex(matrices.last().pose(), x, (float) y + height, 0.00001F).uv(u0, v1).endVertex();
        bufferBuilder.vertex(matrices.last().pose(), (float) x + width, (float) y + height, 0.00002F).uv(u1, v1).endVertex();
        bufferBuilder.vertex(matrices.last().pose(), (float) x + width, y, 0.00003F).uv(u1, v0).endVertex();
        bufferBuilder.vertex(matrices.last().pose(), x, y, 0.00004F).uv(u0, v0).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
        matrices.popPose();
    }

    private void drawMapDecorationTooltip(GuiGraphics context) {
        int scaledWidth = calcScaledWidth(100);
        List<Pair<MapItemSavedData, MapDecoration>> decorationList = getMapDecorationList();
        for (int k = 0; k < MAX_TAB_DISP; k++) {
            int targetIdx = mapIconSelectorOffset + k;
            if (targetIdx >= decorationList.size()) {
                continue;
            }
            Pair<MapItemSavedData, MapDecoration> entry = decorationList.get(targetIdx);
            MapItemSavedData mapData = entry.getFirst();
            MapDecoration mapIcon = entry.getSecond();
            Component mapIconComponent = mapIcon.getName() == null
                    ? MutableComponent.create(new LiteralContents(
                    getReadableName(mapIcon.getType().name().toLowerCase(Locale.ROOT))))
                    : mapIcon.getName();
            if (rawMouseXMoved >= leftPos - (int) (1.0 / 16 * atlasBgScaledSize)
                    && rawMouseXMoved <= leftPos - (int) (1.0 / 16 * atlasBgScaledSize) + scaledWidth
                    && rawMouseYMoved >= topPos + (int) (k * (4 / 32.0 * atlasBgScaledSize)) + (int) (1.0 / 16.0 * atlasBgScaledSize)
                    && rawMouseYMoved <= topPos + (int) (k * (4 / 32.0 * atlasBgScaledSize)) + (int) (1.0 / 16.0 * atlasBgScaledSize) + scaledWidth) {
                // draw text
                LiteralContents coordsComponent = new LiteralContents(
                        "X: " + (mapData.centerX - (atlasScale / 2.0d) + ((atlasScale / 2.0d) * ((mapIcon.getX() + 128) / 128.0d)))
                                + ", Z: "
                                + (mapData.centerZ - (atlasScale / 2.0d) + ((atlasScale / 2.0d) * ((mapIcon.getY() + 128) / 128.0d)))
                );
                MutableComponent formattedCoords = MutableComponent.create(coordsComponent).withStyle(ChatFormatting.GRAY);
                context.renderComponentTooltip(font, List.of(mapIconComponent, formattedCoords), (int) rawMouseXMoved, (int) rawMouseYMoved);
                return;
            }
        }
    }

}