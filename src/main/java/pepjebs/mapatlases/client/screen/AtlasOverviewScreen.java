package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Vector4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
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
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.MoonlightCompat;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.networking.C2SSelectSlicePacket;
import pepjebs.mapatlases.networking.MapAtlasesNetowrking;
import pepjebs.mapatlases.networking.TakeAtlasPacket;
import pepjebs.mapatlases.utils.Slice;

import java.util.*;

import static pepjebs.mapatlases.client.ui.MapAtlasesHUD.scaleVector;

public class AtlasOverviewScreen extends Screen {


    public static final ResourceLocation ATLAS_OVERLAY =
            MapAtlasesMod.res("textures/gui/screen/atlas_overlay.png");
    public static final ResourceLocation ATLAS_TEXTURE = MapAtlasesMod.res("textures/gui/screen/atlas_background.png");
    public static final ResourceLocation ATLAS_TEXTURE_BIG = MapAtlasesMod.res("textures/gui/screen/atlas_background_big.png");

    private final boolean bigTexture = MapAtlasesClientConfig.worldMapBigTexture.get();

    public final ResourceLocation texture = bigTexture ? ATLAS_TEXTURE_BIG : ATLAS_TEXTURE;

    private final int BOOK_WIDTH = bigTexture ? 290 : 162;
    private final int BOOK_HEIGHT = bigTexture ? 231 : 167;

    private final int H_BOOK_WIDTH = BOOK_WIDTH / 2;
    private final int H_BOOK_HEIGHT = BOOK_HEIGHT / 2;
    private final int MAP_WIDGET_WIDTH = bigTexture ? 256 : 128;
    private final int MAP_WIDGET_HEIGHT = bigTexture ? 192 : 128;
    private final int TEXTURE_W = bigTexture ? 512 : 256;

    private final int OVERLAY_UR = bigTexture ? 304 : 189;
    private final int OVERLAY_UL = bigTexture ? 309 : 194;

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
    private final List<DecorationBookmarkButton> decorationBookmarks = new ArrayList<>();
    private final List<DimensionBookmarkButton> dimensionBookmarks = new ArrayList<>();
    private final float globalScale;
    private ResourceKey<Level> currentWorldSelected;
    private Slice selectedSlice;
    private boolean initialized = false;

    boolean placingPin = false;

    public AtlasOverviewScreen(ItemStack atlas) {
        this(atlas, null);
    }

    public AtlasOverviewScreen(ItemStack atlas, @Nullable LecternBlockEntity lectern) {
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
            //if it has no maps here, grab a random one
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

        this.globalScale = lectern == null ?
                (float) (double) MapAtlasesClientConfig.worldMapScale.get() :
                (float) (double) MapAtlasesClientConfig.lecternWorldMapScale.get();

    }

    public ItemStack getAtlas() {
        return atlas;
    }

    public Slice getSelectedSlice() {
        return selectedSlice;
    }

    public ResourceKey<Level> getSelectedDimension() {
        return currentWorldSelected;
    }

    public void removeBookmark(DecorationBookmarkButton pListener) {
        this.removeWidget(pListener);
        decorationBookmarks.remove(pListener);
    }

    @Override
    protected void init() {
        super.init();

        this.sliceButton = new SliceBookmarkButton((width + BOOK_WIDTH) / 2 - 13,
                (height - BOOK_HEIGHT) / 2 + (BOOK_HEIGHT - 36),
                selectedSlice, this);
        this.addRenderableWidget(sliceButton);
        sliceUp = new SliceArrowButton(false, sliceButton, this);
        this.addRenderableWidget(sliceUp);
        sliceDown = new SliceArrowButton(true, sliceButton, this);
        this.addRenderableWidget(sliceDown);

        int i = 0;
        MapCollectionCap maps = MapAtlasItem.getMaps(atlas, level);
        Collection<ResourceKey<Level>> dimensions = maps.getAvailableDimensions();
        int separation = (int) Math.min(22, (BOOK_HEIGHT - 50f) / dimensions.size());
        for (var d : dimensions.stream().sorted(Comparator.comparingInt(e -> {
                    var s = e.location().toString();
                    if (MapAtlasesClient.DIMENSION_TEXTURE_ORDER.contains(s)) {
                        return MapAtlasesClient.DIMENSION_TEXTURE_ORDER.indexOf(s);
                    }
                    return 999;
                }
        )).toList()) {
            DimensionBookmarkButton pWidget = new DimensionBookmarkButton(
                    (width + BOOK_WIDTH) / 2 - 10,
                    (height - BOOK_HEIGHT) / 2 + 15 + i * separation, d, this);
            this.addRenderableWidget(pWidget);
            this.dimensionBookmarks.add(pWidget);
            i++;
        }

        this.mapWidget = this.addRenderableWidget(new MapWidget(
                (width - MAP_WIDGET_WIDTH) / 2,
                (height - MAP_WIDGET_HEIGHT) / 2 + (bigTexture ? 1 : 5),
                MAP_WIDGET_WIDTH, MAP_WIDGET_HEIGHT, 3,
                this, initialMapSelected));

        this.setFocused(mapWidget);

        if (!MapAtlasesConfig.pinMarkerId.get().isEmpty()) {
            this.addRenderableWidget(new PinButton((width + BOOK_WIDTH) / 2 + 20,
                    (height - BOOK_HEIGHT) / 2 + 16, this));
        }
        this.selectDimension(initialWorldSelected);


        if (lectern != null) {
            int pY = BOOK_HEIGHT + 40;

            if (this.minecraft.player.mayBuild()) {
                this.addRenderableWidget(new Button(this.width / 2 - 100, pY, 98, 20, CommonComponents.GUI_DONE, (p_99033_) -> {
                    this.onClose();
                }));
                this.addRenderableWidget(new Button(this.width / 2 + 2, pY, 98, 20, Component.translatable("lectern.take_book"), (p_99024_) -> {
                    MapAtlasesNetowrking.sendToServer(new TakeAtlasPacket(lectern.getBlockPos()));
                    this.onClose();

                }));
            } else {
                this.addRenderableWidget(new Button(this.width / 2 - 100, pY, 200, 20, CommonComponents.GUI_DONE, (p_98299_) -> {
                    this.minecraft.setScreen((Screen) null);
                }));
            }

        }


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
        //add lectern marker
        if (false && lectern != null && currentWorldSelected.equals(lectern.getLevel().dimension())) {
            var data = MapAtlasItem.getMaps(atlas, level).getClosest(
                    lectern.getBlockPos().getX(), lectern.getBlockPos().getZ(),
                    currentWorldSelected, selectedSlice).getSecond();


        }
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (MapAtlasesClient.OPEN_ATLAS_KEYBIND.matches(pKeyCode, pScanCode)) {
            this.onClose();
            return true;
        }
        for (var v : decorationBookmarks) {
            if (v.keyPressed(pKeyCode, pScanCode, pModifiers)) return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
        for (var v : decorationBookmarks) {
            v.keyReleased(pKeyCode, pScanCode, pModifiers);
        }
        return super.keyReleased(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        renderBackground(poseStack);

        poseStack.pushPose();

        poseStack.translate(width / 2f, height / 2f, 0);
        poseStack.scale(globalScale, globalScale, 1);


        //background
        RenderSystem.setShaderTexture(0, texture);
        this.blit(
                poseStack,
                -H_BOOK_WIDTH,
                -H_BOOK_HEIGHT,
                0,
                0,
                BOOK_WIDTH,
                BOOK_HEIGHT,
                TEXTURE_W,
                256
        );
        RenderSystem.setShaderTexture(0, ATLAS_OVERLAY);

        // Draw foreground
        this.blit(
                poseStack,
                -H_BOOK_WIDTH,
                -H_BOOK_HEIGHT,
                0,
                0,
                BOOK_WIDTH,
                BOOK_HEIGHT,
                TEXTURE_W,
                256
        );

        poseStack.translate(-width / 2f, -height / 2f, 0);


        //render widgets
        poseStack.pushPose();
        var v = transformMousePos(mouseX, mouseY);
        super.render(poseStack, (int) v.x(), (int) v.y(), delta);
        poseStack.popPose();

        RenderSystem.enableDepthTest();

        poseStack.pushPose();

        poseStack.translate(width / 2f, height / 2f, 1);
        RenderSystem.setShaderTexture(0, texture);

        this.blit(
                poseStack,
                H_BOOK_WIDTH - 10,
                -H_BOOK_HEIGHT,
                189,
                0,
                5,
                BOOK_HEIGHT,
                TEXTURE_W,
                256
        );
        this.blit(
                poseStack,
                -H_BOOK_WIDTH + 5,
                -H_BOOK_HEIGHT,
                194,
                0,
                5,
                BOOK_HEIGHT,
                TEXTURE_W,
                256
        );
        poseStack.popPose();


        for (var r : this.renderables) {
            if (r instanceof AbstractWidget aw) {
                aw.renderToolTip(poseStack, (int) v.x(), (int) v.y());
            }
        }
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
    public void mouseMoved(double pMouseX, double pMouseY) {
        var v = transformMousePos(pMouseX, pMouseY);
        super.mouseMoved(v.x(), v.y());
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        var v = transformMousePos(pMouseX, pMouseY);
        return super.mouseClicked(v.x(), v.y(), pButton);
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        var v = transformMousePos(pMouseX, pMouseY);
        return super.mouseDragged(v.x(), v.y(), pButton, pDragX, pDragY);
    }

    public Vector4f transformMousePos(double mouseX, double mouseZ) {
        return scaleVector(mouseX, mouseZ, 1 / globalScale, width, height);
    }

    public Vector4f transformPos(double mouseX, double mouseZ) {
        return scaleVector(mouseX, mouseZ, globalScale, width, height);
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
                int error = 1;
            }
            return closest == null ? null : closest.getSecond();
            //centers to any map that has decoration
        }
    }

    @Nullable
    protected Pair<Integer, MapItemSavedData> findMapEntryForCenter(int reqXCenter, int reqZCenter) {
        var m = MapAtlasItem.getMaps(atlas, level).select(reqXCenter, reqZCenter, currentWorldSelected, selectedSlice);
        if (m == null) return null;
        return Pair.of(selectedSlice.getMapId(m.getFirst()), m.getSecond());
    }

    public static String getReadableName(ResourceLocation id) {
        return getReadableName(id.getPath());
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
        if (center == null) {
            int error = 0;
            return;
        }
        this.mapWidget.resetAndCenter(center.x, center.z, true);
        for (var v : dimensionBookmarks) {
            v.setSelected(v.getDimension().equals(currentWorldSelected));
        }
        mapWidget.setFollowingPlayer(currentWorldSelected.equals(initialWorldSelected));
        for (var v : decorationBookmarks) {
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
        for (var bookmark : decorationBookmarks) {
            double x = bookmark.getWorldX();
            double z = bookmark.getWorldZ();
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
            int separation = Math.min(17, (int) (143f / byDistance.size()));

            byDistance.sort(Comparator.comparingDouble(Pair::getFirst));
            for (var e : byDistance) {
                var d = e.getSecond();
                d.y = ((height - BOOK_HEIGHT) / 2 + 15 + index * separation);
                d.setIndex(index);
                index++;
            }
        }

    }

    private void addDecorationWidgets() {
        if (!this.selectedSlice.hasMarkers()) return;
        List<Pair<Object, Pair<String, MapItemSavedData>>> mapIcons = new ArrayList<>();

        boolean ml = MapAtlasesMod.MOONLIGHT;
        for (var p : MapAtlasItem.getMaps(atlas, level).selectSection(currentWorldSelected, selectedSlice)) {
            MapItemSavedData data = p.getSecond();
            for (var d : data.decorations.entrySet()) {
                MapDecoration deco = d.getValue();
                if (deco.renderOnFrame()) {
                    mapIcons.add(Pair.of(deco, p));
                }
            }
            if (ml) {
                mapIcons.addAll(MoonlightCompat.getCustomDecorations(p));
            }
        }
        int i = 0;

        int separation = Math.min(17, (int) ((BOOK_HEIGHT - 22f) / mapIcons.size()));
        List<DecorationBookmarkButton> widgets = new ArrayList<>();
        for (var e : mapIcons) {
            DecorationBookmarkButton pWidget = DecorationBookmarkButton.of(
                    (width - BOOK_WIDTH) / 2 + 10,
                    (height - BOOK_HEIGHT) / 2 + 15 + i * separation, e.getFirst(), e.getSecond(), this);
            pWidget.setIndex(i);
            widgets.add(pWidget);
            this.decorationBookmarks.add(pWidget);
            i++;
        }
        //add widget in order so they render optimized without unneded texture swaps
        widgets.sort(Comparator.comparingInt(DecorationBookmarkButton::getBatchGroup));
        widgets.forEach(this::addRenderableWidget);

    }

    public void focusDecoration(DecorationBookmarkButton button) {
        int x = (int) button.getWorldX();
        int z = (int) button.getWorldZ();
        this.mapWidget.resetAndCenter(x, z, false);
    }

    public boolean decreaseSlice() {
        MapCollectionCap maps = MapAtlasItem.getMaps(atlas, level);
        int current = selectedSlice.heightOrTop();
        Slice.Type type = selectedSlice.type();
        Integer newHeight = maps.getHeightTree(currentWorldSelected, type).floor(current - 1);
        return updateSlice(Slice.of(type, newHeight));
    }

    public boolean increaseSlice() {
        MapCollectionCap maps = MapAtlasItem.getMaps(atlas, level);
        int current = selectedSlice.heightOrTop();
        Slice.Type type = selectedSlice.type();
        Integer newHeight = maps.getHeightTree(currentWorldSelected, type).ceiling(current + 1);
        return updateSlice(Slice.of(type, newHeight));
    }

    public void cycleSliceType() {
        MapCollectionCap maps = MapAtlasItem.getMaps(atlas, level);
        var slices = new ArrayList<>(maps.getAvailableSlices(currentWorldSelected));
        int index = slices.indexOf(selectedSlice.type());
        index = (index + 1) % slices.size();
        updateSlice(Slice.of(slices.get(index), selectedSlice.height()));
    }

    private boolean updateSlice(Slice newSlice) {
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
        boolean manySlices = maps.getHeightTree(currentWorldSelected, selectedSlice.type()).size() > 1;
        boolean manyTypes = maps.getAvailableSlices(currentWorldSelected).size() != 1;
        sliceButton.setActive(manyTypes || manySlices);
        sliceButton.setHasMultipleSlices(manySlices);
        sliceDown.setActive(manySlices);
        sliceUp.setActive(manySlices);
        return changed;
    }


}