package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.capabilities.MapCollectionCap;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.integration.MoonlightCompat;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.networking.C2SSelectSlicePacket;
import pepjebs.mapatlases.networking.MapAtlasesNetowrking;

import java.util.*;

import static pepjebs.mapatlases.client.AbstractAtlasWidget.MAP_DIMENSION;

public class AtlasOverviewScreen extends Screen {

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
    private static final int MAP_WIDGET_SIZE = 128;

    private final ItemStack atlas;
    private final Player player;
    private final Level level;
    private final ResourceKey<Level> initialWorldSelected;
    private final MapItemSavedData initialMapSelected;
    @Nullable
    private final LecternBlockEntity lectern;

    private MapWidget mapWidget;
    private SliceBookmarkButton sliceButton;
    private SliceArrowButton sliceUp;
    private SliceArrowButton sliceDown;
    private final Map<DecorationBookmarkButton, MapItemSavedData> decorationBookmarks = new HashMap<>();
    private final List<DimensionBookmarkButton> dimensionBookmarks = new ArrayList<>();

    private ResourceKey<Level> currentWorldSelected;
    private Integer selectedSlice = null;
    private boolean initialized = false;

    private int mapIconSelectorScroll = 0;
    private int dimSelectorScroll = 0;


    public AtlasOverviewScreen(ItemStack atlas) {
        this(atlas, null);
    }

    public AtlasOverviewScreen(ItemStack atlas, LecternBlockEntity lectern) {
        super(Component.translatable(MapAtlasesMod.MAP_ATLAS.get().getDescriptionId()));
        this.atlas = atlas;
        this.level = Minecraft.getInstance().level;
        this.player = Minecraft.getInstance().player;


        ResourceKey<Level> dim = level.dimension();

        MapCollectionCap maps = MapAtlasItem.getMaps(atlas, level);
        this.selectedSlice = MapAtlasItem.getSelectedSlice(atlas, dim);
        Pair<String, MapItemSavedData> closest = maps.getClosest(player, selectedSlice);
        if (closest != null) {
            this.initialMapSelected = closest.getSecond();
        } else {
            //if has no maps here grab a random one
            this.initialMapSelected = maps.getAll().stream().findFirst().get().getSecond();
            dim = initialMapSelected.dimension;
        }
        //improve for wrong dimension atlas
        this.initialWorldSelected = dim;
        this.currentWorldSelected = dim;

        // Play open sound
        this.player.playSound(MapAtlasesMod.ATLAS_OPEN_SOUND_EVENT.get(),
                (float) (double) MapAtlasesClientConfig.soundScalar.get(), 1.0F);

        this.lectern = lectern;
    }

    @Override
    protected void init() {
        super.init();

        this.sliceButton = new SliceBookmarkButton((width + IMAGE_WIDTH) / 2 - 13,
                (height - IMAGE_HEIGHT) / 2 + 131,
                selectedSlice, this);
        this.addRenderableWidget(sliceButton);
        sliceUp = new SliceArrowButton(false, sliceButton, this);
        this.addRenderableWidget(sliceUp);
        sliceDown = new SliceArrowButton(true, sliceButton, this);
        this.addRenderableWidget(sliceDown);

        int i = 0;
        MapCollectionCap maps = MapAtlasItem.getMaps(atlas, level);
        for (var d : maps.getAvailableDimensions().stream().sorted(Comparator.comparingInt(e ->
                MapAtlasesClient.DIMENSION_TEXTURE_ORDER.indexOf(e.location().toString()))).toList()) {
            DimensionBookmarkButton pWidget = new DimensionBookmarkButton(
                    (width + IMAGE_WIDTH) / 2 - 10,
                    (height - IMAGE_HEIGHT) / 2 + 15 + i * 22, d, this);
            this.addRenderableWidget(pWidget);
            this.dimensionBookmarks.add(pWidget);
            i++;
        }

        this.mapWidget = this.addRenderableWidget(new MapWidget((width - MAP_WIDGET_SIZE) / 2,
                (height - MAP_WIDGET_SIZE) / 2 + 5, MAP_WIDGET_SIZE, MAP_WIDGET_SIZE, 3,
                this, initialMapSelected));

        this.setFocused(mapWidget);

        this.selectDimension(initialWorldSelected);
        this.initialized = true;
    }


    protected boolean isValid() {
        return this.minecraft != null && this.minecraft.player != null &&
                (this.lectern == null || (
                        !this.lectern.isRemoved() && this.lectern.getBook().is(MapAtlasesMod.MAP_ATLAS.get())
                                && !playerIsTooFarAwayToEdit(this.minecraft.player, this.lectern)));
    }

    protected static boolean playerIsTooFarAwayToEdit(Player player, LecternBlockEntity tile) {
        return player.distanceToSqr(tile.getBlockPos().getX(), tile.getBlockPos().getY(),
                tile.getBlockPos().getZ()) > 64.0D;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (mapWidget != null) mapWidget.tick();
        //TODO: update widgets
        //recalculate parameters
        if (!isValid()) {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        renderBackground(poseStack);

        poseStack.pushPose();

        //center view so we can easily scale up
        poseStack.translate(width / 2f, height / 2f, 0);

        //background

        RenderSystem.setShaderTexture(0,ATLAS_TEXTURE);
        this.blit(poseStack,
               -H_IMAGE_WIDTH,
                -H_IMAGE_HEIGHT,
                0,
                0,
                IMAGE_WIDTH,
                IMAGE_HEIGHT
        );

        poseStack.popPose();

        // Draw foreground
        RenderSystem.setShaderTexture(0,ATLAS_OVERLAY);
        this.blit(poseStack,
                -H_IMAGE_WIDTH,
                -H_IMAGE_WIDTH,
                0,
                0,
                IMAGE_WIDTH,
                IMAGE_HEIGHT
        );

        //render widgets
        poseStack.pushPose();
        super.render(poseStack, mouseX, mouseY, delta);
        poseStack.popPose();

        RenderSystem.enableDepthTest();

        poseStack.pushPose();
        poseStack.translate(width / 2f, height / 2f, 1);

        RenderSystem.setShaderTexture(0,ATLAS_TEXTURE);

        this.blit(poseStack,
                H_IMAGE_WIDTH - 10,
                -H_IMAGE_HEIGHT,
                189,
                0,
                5,
                IMAGE_HEIGHT
        );

        this.blit(poseStack,
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

    // ================== Other Util Fns ==================

    public MapItemSavedData getCenterMapForSelectedDim() {
        if (currentWorldSelected.equals(initialWorldSelected)) {
            return initialMapSelected;
        } else {
            MapCollectionCap maps = MapAtlasItem.getMaps(atlas, level);
            MapItemSavedData best = null;
            float averageX = 0;
            float averageZ = 0;
            int count = 0;
            var slice = selectedSlice;
            for (var m : maps.selectSection(currentWorldSelected, slice)) {
                MapItemSavedData d = m.getSecond();
                averageX += d.x;
                averageZ += d.z;
                count++;
                if (d.decorations.values().stream().anyMatch(e -> e.getType().isRenderedOnFrame())) {
                    if (best != null) {
                        if (Mth.lengthSquared(best.x, best.z) > Mth.lengthSquared(d.x, d.z)) {
                            best = d;
                        }
                    } else best = d;
                }
            }
            if (best != null) return best;
            if (count == 0) {
                return null;
            }
            averageX /= count;
            averageZ /= count;
            Pair<String, MapItemSavedData> closest = maps.getClosest(averageX, averageZ, currentWorldSelected, slice);
            if (closest == null) {
                int aa = 1;
            }
            return closest == null ? null : closest.getSecond();
            //centers to any map that has decoration
        }
    }

    @Nullable
    protected Pair<String, MapItemSavedData> findMapEntryForCenter(int reqXCenter, int reqZCenter) {
        return MapAtlasItem.getMaps(atlas, level).select(reqXCenter, reqZCenter, currentWorldSelected, selectedSlice);
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

    public void selectDimension(ResourceKey<Level> dimension) {
        this.currentWorldSelected = dimension;
        //TODO: edge case with just slices
        //we dont change slice when calling this from init as we want to use the atlas initial slice
        updateSlice(!initialized ? selectedSlice : MapAtlasItem.getSelectedSlice(atlas, dimension));

        MapItemSavedData center = this.getCenterMapForSelectedDim();

        this.mapWidget.resetAndCenter(center.x, center.z, true);
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
        // Create a list to store selected decorations
        // Create a TreeMap to store selected decorations sorted by distance
        List<Pair<Double, DecorationBookmarkButton>> byDistance = new ArrayList<>();
        for (var e : decorationBookmarks.entrySet()) {
            DecorationBookmarkButton bookmark = e.getKey();
            MapItemSavedData data = e.getValue();
            double x = bookmark.getWorldX( data);
            double z = bookmark.getWorldZ( data);
            // Check if the decoration is within the specified range
            bookmark.setSelected(x >= minX && x <= maxX && z >= minZ && z <= maxZ);
            if (followingPlayer) {
                double distance = Mth.square(x - currentXCenter) + Mth.square(z - currentZCenter);
                // Store the decoration in the TreeMap with distance as the key
                byDistance.add(Pair.of(distance, bookmark));
            }
        }
        //TODO: maybe this isnt needed
        if (followingPlayer) {
            int index = 0;
            int separation = Math.min(17,(int) (143f / byDistance.size()));

            byDistance.sort(Comparator.comparingDouble(Pair::getFirst));
            for (var e : byDistance) {
                var d = e.getSecond();
                d.y =((height - IMAGE_HEIGHT) / 2 + 15 + index * separation);
                d.setIndex(index);
                index++;
            }
        }

    }

    private void addDecorationWidgets() {
        List<Pair<Object, MapItemSavedData>> mapIcons = new ArrayList<>();

        boolean ml = MapAtlasesMod.MOONLIGHT;
        for (var p : MapAtlasItem.getMaps(atlas, level).selectSection(currentWorldSelected, selectedSlice)) {
            MapItemSavedData data = p.getSecond();
            for (var d : data.decorations.entrySet()) {
                MapDecoration deco = d.getValue();
                if (deco.renderOnFrame()) {
                    mapIcons.add(Pair.of(deco, data));
                }
            }
            if (ml) {
                mapIcons.addAll(MoonlightCompat.getCustomDecorations(data));
            }
        }
        int i = 0;

        int separation = Math.min(17,(int) (143f / mapIcons.size()));
        List<DecorationBookmarkButton> widgets = new ArrayList<>();
        for (var e : mapIcons) {
            DecorationBookmarkButton pWidget = DecorationBookmarkButton.of(
                    (width - IMAGE_WIDTH) / 2 + 10,
                    (height - IMAGE_HEIGHT) / 2 + 15 + i * separation, e.getFirst(), this);
            pWidget.setIndex(i);
            widgets.add(pWidget);
            this.decorationBookmarks.put(pWidget, e.getSecond());
            i++;
        }
        //add widget in order so they render optimized without unneded texture swaps
        widgets.sort(Comparator.comparingInt(DecorationBookmarkButton::getBatchGroup));
        widgets.forEach(this::addRenderableWidget);

    }

    public void focusDecoration(DecorationBookmarkButton button) {
        MapItemSavedData targetData = decorationBookmarks.get(button);
        int x = (int) button.getWorldX(targetData);
        int z = (int) button.getWorldZ(targetData);
        this.mapWidget.resetAndCenter(x, z, false);
    }

    public boolean decreaseSlice() {
        MapCollectionCap maps = MapAtlasItem.getMaps(atlas, level);
        int current = selectedSlice == null ? Integer.MAX_VALUE : selectedSlice;
        Integer newSlice = maps.getAvailableSlices(currentWorldSelected).floor(current - 1);
        return updateSlice(newSlice);
    }

    public boolean increaseSlice() {
        MapCollectionCap maps = MapAtlasItem.getMaps(atlas, level);
        int current = selectedSlice == null ? Integer.MAX_VALUE : selectedSlice;
        Integer newSlice = maps.getAvailableSlices(currentWorldSelected).ceiling(current + 1);
        return updateSlice(newSlice);
    }

    private boolean updateSlice(Integer newSlice) {
        if (Objects.equals(newSlice, Integer.MAX_VALUE)) newSlice = null;
        boolean changed = false;
        if (!Objects.equals(selectedSlice, newSlice)) {
            selectedSlice = newSlice;
            sliceButton.setSlice(selectedSlice);
            //notify server
            MapAtlasesNetowrking.sendToServer(new C2SSelectSlicePacket(selectedSlice,
                    lectern == null ? null : lectern.getBlockPos(), currentWorldSelected));
            //update client immediately
            MapAtlasItem.setSelectedSlice(atlas, selectedSlice, currentWorldSelected);

            changed = true;
        }
        //update button regardless
        MapCollectionCap maps = MapAtlasItem.getMaps(atlas, level);
        boolean active = maps.getAvailableSlices(currentWorldSelected).size() > 1;
        sliceButton.setActive(active);
        sliceDown.setActive(active);
        sliceUp.setActive(active);
        return changed;
    }
}