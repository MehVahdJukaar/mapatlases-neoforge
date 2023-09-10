package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MapAtlasesAtlasOverviewScreen extends Screen {

    public static final ResourceLocation ATLAS_OVERLAY =
            MapAtlasesMod.res("textures/gui/screen/atlas_overlay.png");
    public static final ResourceLocation ATLAS_TEXTURE =
            MapAtlasesMod.res("textures/gui/screen/atlas_background.png");
    public static final ResourceLocation ATLAS_TEXTURE_1 =
            MapAtlasesMod.res("textures/gui/screen/atlas_background1.png");
    public static final ResourceLocation MAP_ICON_TEXTURE = new ResourceLocation("textures/map/map_icons.png");


    public static final int MAP_SIZE = 128;
    public static final RenderType MAP_ICONS = RenderType.text(MAP_ICON_TEXTURE);
    public static final int ZOOM_BUCKET = 4;
    private static final int MAX_TAB_DISP = 7;

    public static final int IMAGE_WIDTH = 162;//226;
    public static final int IMAGE_HEIGHT = 167;//231;
    private static final int H_IMAGE_WIDTH = IMAGE_WIDTH / 2;
    private static final int H_IMAGE_HEIGHT = IMAGE_HEIGHT / 2;
    private static final int BOOKMARK_W = 20;
    private static final int BOOKMARK_H = 16;
    private static final int BOOKMARK_SEP = 4;

    private final ItemStack atlas;
    private final Player player;
    private final Level level;
    private final int atlasScale;
    private final List<ResourceKey<Level>> dimensions = new ArrayList<>();
    private final ResourceKey<Level> initialWorldSelected;
    private final MapItemSavedData initialMapSelected;

    private final Map<ResourceKey<Level>, List<Pair<String, MapItemSavedData>>> dimToData = new HashMap<>();
    private final Map<Pair<Integer, Integer>, Pair<String, MapItemSavedData>> byCenter = new HashMap<>();

    private MapWidget mapWidget;

    private ResourceKey<Level> currentWorldSelected;
    private int mouseXOffset = 0;
    private int mouseYOffset = 0;
    private int currentXCenter;
    private int currentZCenter;


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

        initialMapSelected = MapAtlasesAccessUtils.getClosestMapData(dimToData.get(currentWorldSelected), player).getSecond();

        atlasScale = (1 << initialMapSelected.scale) * MAP_SIZE;
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
        int i = 0;
        for (var d : dimensions) {
            this.addRenderableWidget(new DimensionBookmarkButton(
                    (width + IMAGE_WIDTH) / 2 - 10,
                    (height - IMAGE_HEIGHT) / 2 + 15 + i * 22, d));
            i++;
        }
        i = 0;
        for (var d : getMapDecorationList()) {
            this.addRenderableWidget(new DecorationBookmarkButton(
                    (width - IMAGE_WIDTH) / 2 + 10,
                    (height - IMAGE_HEIGHT) / 2 + 15 + i * 22, d.getSecond()));
            i++;
        }

        int mapSize = 128;
        this.mapWidget = this.addRenderableWidget(new MapWidget((width - mapSize) / 2,
                (height - mapSize) / 2 + 5, mapSize, mapSize, 3, atlasScale,
                this));

        this.setFocused(mapWidget);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
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
        super.render(graphics, mouseX, mouseY, delta);
poseStack.pushPose();
        poseStack.translate(width / 2f, height / 2f, 0);

        graphics.blit(
                ATLAS_TEXTURE,
                H_IMAGE_WIDTH-10,
                -H_IMAGE_HEIGHT,
                186,
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
        // Handle world map zooming
        double drawnMapBufferSize = atlasBgScaledSize / 18.0;
        if (mouseX < leftPos + drawnMapBufferSize || mouseY < topPos + drawnMapBufferSize
                || mouseX > leftPos + atlasBgScaledSize - drawnMapBufferSize
                || mouseY > topPos + atlasBgScaledSize - drawnMapBufferSize)
            return true;
        zoomValue += -1 * amount;
        zoomValue = Math.max(zoomValue, -1 * ZOOM_BUCKET);
        */
        return true;
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        /*
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
        }*/
        return false;
    }

    // ================== Drawing Utils ==================


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

}