package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
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
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.*;
import java.util.stream.Collectors;

import static pepjebs.mapatlases.client.AbstractAtlasWidget.MAP_DIMENSION;

public class MapAtlasesAtlasOverviewScreen extends Screen {

    public static final ResourceLocation ATLAS_OVERLAY =
            MapAtlasesMod.res("textures/gui/screen/atlas_overlay.png");
    public static final ResourceLocation ATLAS_TEXTURE =
            MapAtlasesMod.res("textures/gui/screen/atlas_background.png");
    public static final ResourceLocation ATLAS_TEXTURE_1 =
            MapAtlasesMod.res("textures/gui/screen/atlas_background1.png");


    public static final int IMAGE_WIDTH = 162;//226;
    public static final int IMAGE_HEIGHT = 167;//231;
    private static final int H_IMAGE_WIDTH = IMAGE_WIDTH / 2;
    private static final int H_IMAGE_HEIGHT = IMAGE_HEIGHT / 2;

    private final ItemStack atlas;
    private final Player player;
    private final Level level;
    private final List<ResourceKey<Level>> dimensions = new ArrayList<>();
    private final ResourceKey<Level> initialWorldSelected;
    private final MapItemSavedData initialMapSelected;

    private final Map<ResourceKey<Level>, List<Pair<String, MapItemSavedData>>> dimToData = new HashMap<>();
    private final Map<Pair<Integer, Integer>, Pair<String, MapItemSavedData>> byCenter = new HashMap<>();

    private MapWidget mapWidget;
    private final Map<DecorationBookmarkButton, MapItemSavedData> decorationBookmarks = new HashMap<>();
    private final List<DimensionBookmarkButton> dimensionBookmarks = new ArrayList<>();

    private ResourceKey<Level> currentWorldSelected;

    private int mapIconSelectorScroll = 0;
    private int dimSelectorScroll = 0;

    public MapAtlasesAtlasOverviewScreen(Component title, ItemStack atlas) {
        super(title);
        this.atlas = atlas;

        this.level = Minecraft.getInstance().level;
        this.player = Minecraft.getInstance().player;

        initialWorldSelected = player.level().dimension();
        currentWorldSelected = initialWorldSelected;

        tick();

        initialMapSelected = MapAtlasesAccessUtils.getClosestMapData(dimToData.get(currentWorldSelected), player).getSecond();


        // Play open sound
        player.playSound(MapAtlasesMod.ATLAS_OPEN_SOUND_EVENT.get(),
                (float) (double) MapAtlasesClientConfig.soundScalar.get(), 1.0F);
    }

    @Override
    protected void init() {
        super.init();

        tick();
        int i = 0;
        for (var d : dimensions) {
            DimensionBookmarkButton pWidget = new DimensionBookmarkButton(
                    (width + IMAGE_WIDTH) / 2 - 10,
                    (height - IMAGE_HEIGHT) / 2 + 15 + i * 22, d, this);
            this.addRenderableWidget(pWidget);
            this.dimensionBookmarks.add(pWidget);
            i++;
        }

        int mapSize = 128;
        MapItemSavedData originalCenterMap = this.getClosestMapToPlayer();
        this.mapWidget = this.addRenderableWidget(new MapWidget((width - mapSize) / 2,
                (height - mapSize) / 2 + 5, mapSize, mapSize, 3,
                this, originalCenterMap));

        this.setFocused(mapWidget);

        this.selectDimension(initialWorldSelected);
        if (originalCenterMap != null) {
            this.updateVisibleDecoration(originalCenterMap.centerX, originalCenterMap.centerZ, 3 / 2f * MAP_DIMENSION,
                    true);
        }
    }


    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if(mapWidget != null) mapWidget.tick();
        //TODO: update widgets
        //recalculate parameters
        dimToData.clear();
        dimToData.putAll(MapAtlasesAccessUtils.getAllMapDataByDimension(level, atlas));
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

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        //center view so we can easily scale up
        poseStack.translate(width / 2f, height / 2f, 0);

        //background

        graphics.blit(
                ATLAS_TEXTURE,
                -H_IMAGE_WIDTH,
                -H_IMAGE_HEIGHT,
                0,
                0,
                IMAGE_WIDTH,
                IMAGE_HEIGHT
        );

        poseStack.popPose();

        // Draw foreground
        graphics.blit(
                ATLAS_OVERLAY,
                -H_IMAGE_WIDTH,
                -H_IMAGE_WIDTH,
                0,
                0,
                IMAGE_WIDTH,
                IMAGE_HEIGHT
        );

        //render widgets
        poseStack.pushPose();
        super.render(graphics, mouseX, mouseY, delta);
        poseStack.popPose();

        RenderSystem.enableDepthTest();

        poseStack.pushPose();
        poseStack.translate(width / 2f, height / 2f, 1);

        graphics.blit(
                ATLAS_TEXTURE,
                H_IMAGE_WIDTH - 10,
                -H_IMAGE_HEIGHT,
                189,
                0,
                5,
                IMAGE_HEIGHT
        );

        graphics.blit(
                ATLAS_TEXTURE,
                -H_IMAGE_WIDTH + 5,
                -H_IMAGE_HEIGHT,
                194,
                0,
                5,
                IMAGE_HEIGHT
        );

        poseStack.popPose();
    }

    // ================== Mouse Functions ==================

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        // Handle dim selector scroll
/*
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

        */
        return super.mouseScrolled(mouseX, mouseY, amount);
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        /*
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
        }*/
        return false;
    }


    // ================== Other Util Fns ==================

    public MapItemSavedData getCenterMapForSelectedDim() {
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
    protected MapItemSavedData getClosestMapToPlayer() {
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


    @Nullable
    protected Pair<String, MapItemSavedData> findMapEntryForCenter(int reqXCenter, int reqZCenter) {
        return byCenter.get(Pair.of(reqXCenter, reqZCenter));
    }


    public static String getReadableName(ResourceLocation id) {
        String s = id.toShortLanguageKey();
        return getReadableName(s);
    }

    @NotNull
    public static String getReadableName(String s) {
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

    public void selectDimension(ResourceKey<Level> dimension) {
        this.currentWorldSelected = dimension;
        MapItemSavedData center = this.getCenterMapForSelectedDim();

        this.mapWidget.resetAndCenter(center.centerX, center.centerZ, true);
        for (var v : dimensionBookmarks) {
            v.setSelected(v.getDimension().equals(currentWorldSelected));
        }
        mapWidget.setFollowingPlayer(currentWorldSelected.equals(initialWorldSelected));
        for (var v : decorationBookmarks.keySet()) {
            this.removeWidget(v);
        }
        decorationBookmarks.clear();
        addDecorationWidgets();
    }

    public void updateVisibleDecoration(int currentXCenter, int currentZCenter, float r, boolean followingPlayer) {
        float minX = currentXCenter - r;
        float maxX = currentXCenter + r;
        float minZ = currentZCenter - r;
        float maxZ = currentZCenter + r;
        float mapScale = mapWidget.getMapAtlasScale();
        // Create a list to store selected decorations
        // Create a TreeMap to store selected decorations sorted by distance
        TreeMap<Double, DecorationBookmarkButton> byDistance = new TreeMap<>();
        for (var e : decorationBookmarks.entrySet()) {
            DecorationBookmarkButton bookmark = e.getKey();
            MapDecoration deco = bookmark.getDecoration();
            MapItemSavedData data = e.getValue();
            double x = getDecorationX(deco, data, mapScale);
            double z = getDecorationZ(deco, data, mapScale);
            // Check if the decoration is within the specified range
            bookmark.setSelected(x >= minX && x <= maxX && z >= minZ && z <= maxZ);
            if (followingPlayer) {
                double distance = Mth.square(x - currentXCenter) + Mth.square(z - currentZCenter);
                // Store the decoration in the TreeMap with distance as the key
                byDistance.put(distance, bookmark);
            }
        }
        //TODO: maybe this isnt needed
        if (followingPlayer) {
            int inxed = 0;
            //TODO: make it into a list
            for (var d : byDistance.values()) {
                d.setY((height - IMAGE_HEIGHT) / 2 + 15 + inxed * 17);
                inxed++;
            }
        }

    }

    private static double getDecorationZ(MapDecoration deco, MapItemSavedData data, float mapScale) {
        return data.centerZ - (mapScale / 2.0d) + ((mapScale / 2.0d) * ((deco.getY() + MAP_DIMENSION) / (float) MAP_DIMENSION));
    }

    private static double getDecorationX(MapDecoration deco, MapItemSavedData data, float mapScale) {
        return data.centerX - (mapScale / 2.0d) + ((mapScale / 2.0d) * ((deco.getX() + MAP_DIMENSION) / (float) MAP_DIMENSION));
    }

    private void addDecorationWidgets() {
        int i = 0;
        for (var d : getMapDecorationList()) {
            DecorationBookmarkButton pWidget = new DecorationBookmarkButton(
                    (width - IMAGE_WIDTH) / 2 + 10,
                    (height - IMAGE_HEIGHT) / 2 + 15 + i * 17, d.getSecond(), this);
            this.addRenderableWidget(pWidget);
            this.decorationBookmarks.put(pWidget, d.getFirst());
            i++;
        }
    }

    public void focusDecoration(DecorationBookmarkButton button) {
        MapItemSavedData targetData = decorationBookmarks.get(button);
        MapDecoration decoration = button.getDecoration();
        float scale = mapWidget.getMapAtlasScale();
        int x = (int) getDecorationX(decoration, targetData, scale);
        int z = (int) getDecorationZ(decoration, targetData, scale);
        this.mapWidget.resetAndCenter(x, z, false);
    }
}