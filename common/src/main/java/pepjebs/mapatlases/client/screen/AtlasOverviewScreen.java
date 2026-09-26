package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4d;
import org.lwjgl.glfw.GLFW;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.AtlasGuiUtils;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.moonlight.ClientMarkers;
import pepjebs.mapatlases.integration.moonlight.MoonlightCompat;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.map_collection.MapGridKey;
import pepjebs.mapatlases.networking.C2SRemoveMapPacket;
import pepjebs.mapatlases.networking.C2SRemoveSlicePacket;
import pepjebs.mapatlases.networking.C2SSelectSlicePacket;
import pepjebs.mapatlases.networking.C2STakeAtlasPacket;
import pepjebs.mapatlases.utils.*;

import java.util.*;

import static pepjebs.mapatlases.client.MapAtlasesClient.GUI_ICONS_TEXTURE;

public class AtlasOverviewScreen extends Screen {

    private static final int MODAL_W = 100;
    private static final int MODAL_H = 20;

    private final BookTexture bookBackground = MapAtlasesClientConfig.worldMapBigTexture.get() ? BookTexture.BIG : BookTexture.SMALL;

    private ItemStack atlas;
    private final Player player;
    private final Level level;
    @Nullable
    private final LecternBlockEntity lectern;

    private MapWidget mapWidget;
    private PinNameBox pinNameBox;
    private EditBox filterBox;
    @Nullable
    private EditBox activeTextBox;
    private SliceBookmarkButton sliceButton;
    private SliceArrowButton sliceUp;
    private SliceArrowButton sliceDown;
    private DimensionListPanel dimensionPanel;
    private DecorationListPanel decorationPanel;
    @Nullable
    private CursorActionButton pinButton;
    final float globalScale;
    private final boolean isPinOnly;
    private Slice selectedSlice;
    private boolean initialized = false;
    private CursorAction selectedCursorAction;
    private boolean cursorActionHasTarget;

    @Nullable
    private Pair<AtlasMap, ColumnPos> partialPin = null;

    @NotNull
    private MapCollection currentMaps;

    // for fancy menu or something
    public AtlasOverviewScreen() {
        this(AtlasLookup.getAtlasFromPlayerByConfig(Minecraft.getInstance().player), null, false);
    }

    public AtlasOverviewScreen(ItemStack atlas, @Nullable LecternBlockEntity lectern, boolean placingPin) {
        super(Component.translatable(MapAtlasesMod.MAP_ATLAS.get().getDescriptionId()));
        this.atlas = atlas;
        this.level = Objects.requireNonNull(Minecraft.getInstance().level);
        this.player = Objects.requireNonNull(Minecraft.getInstance().player);
        this.lectern = lectern;
        this.globalScale = lectern == null ?
                (float) (double) MapAtlasesClientConfig.worldMapScale.get() :
                (float) (double) MapAtlasesClientConfig.lecternWorldMapScale.get();

        this.currentMaps = MapAtlasItem.getMaps(atlas, level);
        this.selectedSlice = MapAtlasItem.getSelectedSlice(atlas, level.dimension());
        AtlasMap startingMap = findStartingMap();
        if (startingMap != null) this.selectedSlice = startingMap.slice;

        this.isPinOnly = placingPin;
        this.selectedCursorAction = placingPin ? CursorAction.PLACING_PIN : CursorAction.NONE;
        if (!isPinOnly) {
            this.player.playSound(MapAtlasesMod.ATLAS_OPEN_SOUND_EVENT.get(),
                    (float) (double) MapAtlasesClientConfig.soundScalar.get(), 1.0F);
        } else if (startingMap != null) {
            partialPin = Pair.of(startingMap, new ColumnPos(player.getBlockX(), player.getBlockZ()));
        }
    }

    @Nullable
    private AtlasMap findMapClosestToPlayer() {
        return currentMaps.getClosest(player, selectedSlice);
    }

    @Nullable
    private AtlasMap findStartingMap() {
        AtlasMap closest = findMapClosestToPlayer();
        if (closest != null) return closest;
        var all = currentMaps.getAllFound();
        return all.isEmpty() ? null : all.getFirst();
    }

    public ItemStack getAtlas() {
        return atlas;
    }

    public Slice getSelectedSlice() {
        return selectedSlice;
    }

    public Optional<BlockPos> lecternPos() {
        return Optional.ofNullable(lectern).map(BlockEntity::getBlockPos);
    }

    @Override
    protected void init() {
        super.init();
        if (currentMaps.isEmpty()) {
            this.onClose();
            return;
        }
        int bookLeft = (width - bookBackground.width()) / 2;
        int bookRight = (width + bookBackground.width()) / 2;
        int bookTop = (height - bookBackground.height()) / 2;

        // text boxes render above the scaled book so they arent added as widgets
        this.activeTextBox = null;
        this.pinNameBox = new PinNameBox(this.font, modalX(), modalY(), MODAL_W, MODAL_H,
                Component.translatable("message.map_atlases.marker_name"), this::addNewPin);
        this.filterBox = new EditBox(this.font, modalX(), modalY(), MODAL_W, MODAL_H, Component.empty());
        filterBox.setMaxLength(50);
        filterBox.active = false;
        filterBox.visible = false;

        this.sliceButton = addRenderableWidget(new SliceBookmarkButton(bookRight - 13, bookTop + bookBackground.height() - 36, selectedSlice, this));
        this.sliceUp = addRenderableWidget(new SliceArrowButton(false, sliceButton, this));
        this.sliceDown = addRenderableWidget(new SliceArrowButton(true, sliceButton, this));

        this.dimensionPanel = new DimensionListPanel(this, bookRight, bookTop, bookBackground.height(),
                this::addRenderableWidget, this::removeWidget);
        dimensionPanel.build(currentMaps.getAvailableDimensions());
        this.decorationPanel = new DecorationListPanel(this, bookLeft, bookTop, bookBackground.height(),
                this::addRenderableWidget, this::removeWidget);

        this.mapWidget = addRenderableWidget(new MapWidget(
                (width - bookBackground.mapWidth()) / 2, (height - bookBackground.mapHeight()) / 2 + bookBackground.mapYOffset(),
                bookBackground.mapWidth(), bookBackground.mapHeight(), 3, this, findStartingMap()));
        this.setFocused(mapWidget);

        initSideButtons(bookLeft, bookRight, bookTop + 16);
        initLecternButtons();

        selectDimension(level.dimension());
        applyPendingPanelRefreshes();

        if (isPinOnly) openPinNameBox();
        this.initialized = true;
    }

    private int modalX() {
        return (width - MODAL_W) / 2;
    }

    private int modalY() {
        return (height - MODAL_H) / 2;
    }

    private void initSideButtons(int bookLeft, int bookRight, int topY) {
        int rightX = bookRight + 20;
        int rightY = topY;
        if (!MapAtlasesConfig.pinMarkerId.get().isEmpty() && MapAtlasesClientConfig.moonlightCompat.get()) {
            this.pinButton = addRenderableWidget(CursorActionButton.pin(rightX, rightY, this));
            rightY += 20;
        }
        if (MapAtlasesClientConfig.shearButton.get()) {
            addRenderableWidget(CursorActionButton.shear(rightX, rightY, this));
        }

        int leftX = bookLeft - 20 - 16;
        int leftY = topY;
        if (MapAtlasesClientConfig.compass.get()) {
            addRenderableWidget(new ItemWidget(leftX, leftY, Items.COMPASS.getDefaultInstance()));
            leftY += 20;
        }
        if (MapAtlasesClientConfig.clock.get()) {
            addRenderableWidget(new ItemWidget(leftX, leftY, Items.CLOCK.getDefaultInstance()));
        }
    }

    private void initLecternButtons() {
        if (lectern == null) return;
        int y = (int) (globalScale * (height + bookBackground.height() + 4) / 2);
        boolean canTakeBook = player.mayBuild();
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                .bounds(this.width / 2 - 100, y, canTakeBook ? 98 : 200, 20).build());
        if (canTakeBook) {
            addRenderableWidget(Button.builder(Component.translatable("lectern.take_book"), b -> {
                NetworkHelper.sendToServer(new C2STakeAtlasPacket(lectern.getBlockPos()));
                this.onClose();
            }).bounds(this.width / 2 + 2, y, 98, 20).build());
        }
    }

    private boolean isValid() {
        if (this.minecraft == null || this.minecraft.player == null) return false;
        if (lectern == null) return true;
        return Container.stillValidBlockEntity(lectern, this.minecraft.player)
                && lectern.getBook().is(MapAtlasesMod.MAP_ATLAS.get());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (!isValid()) {
            this.minecraft.setScreen(null);
            return;
        }
        if (!initialized) return;
        // the inventory stack gets replaced on every server sync, so the one we opened with goes stale
        ItemStack syncedAtlas = lectern == null ? AtlasLookup.getAtlasFromPlayerByConfig(player) : lectern.getBook();
        if (syncedAtlas.isEmpty()) {
            this.onClose();
            return;
        }
        this.atlas = syncedAtlas;
        MapCollection maps = MapAtlasItem.getMaps(atlas, level);
        maps.resolvePendingMaps(level);
        boolean mapsChanged = !maps.getIds().equals(currentMaps.getIds()) || maps.getAllFound().size() != currentMaps.getAllFound().size();
        this.currentMaps = maps;
        if (mapsChanged) onMapsChanged();

        mapWidget.tick();
        if (activeTextBox == pinNameBox) pinNameBox.tick();
        applyPendingPanelRefreshes();
    }

    private void applyPendingPanelRefreshes() {
        decorationPanel.applyPendingRefresh();
        dimensionPanel.applyPendingRefresh();
    }

    private void onMapsChanged() {
        if (currentMaps.isEmpty()) {
            this.onClose();
            return;
        }
        var dimensions = currentMaps.getAvailableDimensions();
        dimensionPanel.build(dimensions);
        ResourceKey<Level> dim = selectedSlice.dimension();
        if (!dimensions.contains(dim)) {
            selectDimension(dimensions.iterator().next());
            return;
        }
        dimensionPanel.setSelectedDimension(dim);
        if (!selectSlice(currentMaps.closestAvailableSlice(dim, selectedSlice))) recalculateDecorationWidgets();
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == GLFW.GLFW_KEY_ESCAPE && cancelCurrentKeyPressed()) return true;
        if (activeTextBox != null) {
            boolean enter = pKeyCode == GLFW.GLFW_KEY_ENTER || pKeyCode == GLFW.GLFW_KEY_KP_ENTER;
            if (enter && activeTextBox == filterBox) {
                decorationPanel.applyFilter(filterBox.getValue());
                closeTextBox();
            } else {
                activeTextBox.keyPressed(pKeyCode, pScanCode, pModifiers);
            }
            return true;
        }
        if (!MapAtlasesClient.PLACE_PIN_KEYBIND.isUnbound()
                && MapAtlasesClient.PLACE_PIN_KEYBIND.matches(pKeyCode, pScanCode)) {
            if (!isPinOnly && pinButton != null) toggleCursorAction(CursorAction.PLACING_PIN);
            return true;
        }
        if (super.keyPressed(pKeyCode, pScanCode, pModifiers)) return true;
        if (MapAtlasesClient.OPEN_ATLAS_KEYBIND.matches(pKeyCode, pScanCode)) {
            this.onClose();
            return true;
        }
        for (var v : decorationPanel.getVisibleButtons()) {
            if (v.keyPressed(pKeyCode, pScanCode, pModifiers)) return true;
        }
        return false;
    }

    private boolean cancelCurrentKeyPressed() {
        if (activeTextBox == pinNameBox) {
            closeTextBox();
            partialPin = null;
            if (isPinOnly) this.onClose();
            return true;
        }
        if (activeTextBox == filterBox) {
            closeTextBox();
            return true;
        }
        if (selectedCursorAction != CursorAction.NONE) {
            clearCursorAction();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
        for (var v : decorationPanel.getVisibleButtons()) {
            v.keyReleased(pKeyCode, pScanCode, pModifiers);
        }
        return super.keyReleased(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(graphics);
        bookBackground.render(graphics, width, height);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        PoseStack poseStack = graphics.pose();

        if (!isPinOnly) {
            poseStack.pushPose();
            poseStack.translate(width / 2f, height / 2f, 0);
            poseStack.scale(globalScale, globalScale, 1);
            poseStack.translate(-width / 2f, -height / 2f, 0.2);
            RenderSystem.enableDepthTest();
            var v = screenToBookCoords(mouseX, mouseY);
            boolean editing = isEditingText();
            super.render(graphics, editing ? -1 : (int) v.x, editing ? -1 : (int) v.y, delta);
            poseStack.popPose();
        }

        if (activeTextBox != null) {
            poseStack.pushPose();
            poseStack.translate(0, 0, 30);
            activeTextBox.render(graphics, mouseX, mouseY, delta);
            poseStack.popPose();
        } else if (MapAtlasesClientConfig.worldMapCrossair.get()) {
            renderCrosshair(graphics);
        }

        ResourceLocation cursorIcon = selectedCursorAction.getIcon(cursorActionHasTarget);
        if (cursorIcon != null) {
            poseStack.pushPose();
            poseStack.translate(mouseX - 2.5f, mouseY - 2.5f, 10);
            graphics.blitSprite(cursorIcon, 0, 0, 8, 8);
            poseStack.popPose();
        }
        this.cursorActionHasTarget = false;
    }

    private void renderCrosshair(GuiGraphics graphics) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 5);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
                GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        graphics.blit(GUI_ICONS_TEXTURE, (width - 15) / 2, (height - 15) / 2, 0, 0, 15, 15);
        RenderSystem.defaultBlendFunc();
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isEditingText()) return false;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void mouseMoved(double pMouseX, double pMouseY) {
        if (isEditingText()) return;
        var v = screenToBookCoords(pMouseX, pMouseY);
        super.mouseMoved(v.x, v.y);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (activeTextBox != null) return activeTextBox.mouseClicked(pMouseX, pMouseY, pButton);
        var v = screenToBookCoords(pMouseX, pMouseY);
        boolean result = super.mouseClicked(v.x, v.y, pButton);
        applyPendingPanelRefreshes();
        return result;
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        if (activeTextBox != null) return activeTextBox.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
        var v = screenToBookCoords(pMouseX, pMouseY);
        return super.mouseDragged(v.x, v.y, pButton, pDragX, pDragY);
    }

    public Vector4d screenToBookCoords(double mouseX, double mouseY) {
        return AtlasGuiUtils.scaleVector(mouseX, mouseY, 1 / globalScale, width, height);
    }

    public Vector4d bookToScreenCoords(double x, double y) {
        return AtlasGuiUtils.scaleVector(x, y, globalScale, width, height);
    }

    @Nullable
    private MapItemSavedData getCenterMapForSelectedDim() {
        AtlasMap center = selectedSlice.dimension().equals(level.dimension())
                ? findMapClosestToPlayer()
                : findMarkedOrMiddleMap();
        return center == null ? null : center.data;
    }

    @Nullable
    private AtlasMap findMarkedOrMiddleMap() {
        List<AtlasMap> section = currentMaps.getMapsInSlice(selectedSlice);
        if (section.isEmpty()) return null;
        AtlasMap best = null;
        double sumX = 0;
        double sumZ = 0;
        for (AtlasMap holder : section) {
            MapItemSavedData d = holder.data;
            sumX += d.centerX;
            sumZ += d.centerZ;
            boolean hasMarkers = d.decorations.values().stream()
                    .anyMatch(e -> e.type().value().showOnItemFrame());
            if (hasMarkers && (best == null || distFromOriginSq(best.data) > distFromOriginSq(d))) {
                best = holder;
            }
        }
        if (best != null) return best;
        return currentMaps.getClosest(sumX / section.size(), sumZ / section.size(), selectedSlice);
    }

    private static double distFromOriginSq(MapItemSavedData data) {
        return Mth.lengthSquared(data.centerX, data.centerZ);
    }

    @Nullable
    protected AtlasMap findMapWithCenter(int reqXCenter, int reqZCenter) {
        return currentMaps.getMapAt(reqXCenter, reqZCenter, selectedSlice);
    }

    @Nullable
    protected AtlasMap findMapContaining(int x, int z) {
        return currentMaps.getMapAt(MapGridKey.at(currentMaps.getScale(), selectedSlice, x, z));
    }

    public void selectDimension(ResourceKey<Level> dimension) {
        boolean sameDim = selectedSlice.dimension().equals(dimension);
        // On first call from init we keep the atlas's saved slice; afterwards use the per-dim saved slice.
        Slice saved = !initialized ? selectedSlice : MapAtlasItem.getSelectedSlice(atlas, dimension);
        boolean sliceChanged = selectSlice(currentMaps.closestAvailableSlice(dimension, saved));
        dimensionPanel.setSelectedDimension(dimension);
        if (!sliceChanged) recalculateDecorationWidgets();

        MapItemSavedData center = this.getCenterMapForSelectedDim();
        if (center == null) return;
        boolean isWherePlayerIs = level.dimension().equals(dimension);
        this.mapWidget.resetAndCenter(center.centerX, center.centerZ, isWherePlayerIs, sameDim);
    }

    protected void recalculateDecorationWidgets() {
        List<DecorationHolder> mapIcons = new ArrayList<>();
        for (AtlasMap holder : currentMaps.getMapsInSlice(selectedSlice)) {
            for (var d : holder.data.decorations.entrySet()) {
                MapDecoration deco = d.getValue();
                if (deco.renderOnFrame() && !deco.type().is(MapAtlasesMod.NON_REMOVABLE_DECORATIONS)) {
                    mapIcons.add(DecorationHolder.vanilla(deco, d.getKey(), holder));
                }
            }
            mapIcons.addAll(MoonlightCompat.getCustomDecorations(holder));
        }
        decorationPanel.rebuild(mapIcons);
    }

    public void onMapViewMoved(int currentXCenter, int currentZCenter, float radius) {
        decorationPanel.onMapViewMoved(currentXCenter, currentZCenter, radius);
    }

    public void centerOnDecoration(DecorationBookmarkButton button) {
        this.mapWidget.resetAndCenter((int) button.getWorldX(), (int) button.getWorldZ(), false, true);
    }

    public void stepSlice(boolean up) {
        Slice next = currentMaps.adjacentSlice(selectedSlice, up);
        if (next != null) selectSlice(next);
    }

    public void cycleSliceType() {
        Slice next = currentMaps.nextTypeSlice(selectedSlice);
        if (next != null) selectSlice(next);
    }

    private boolean selectSlice(Slice newSlice) {
        boolean changed = !Objects.equals(selectedSlice, newSlice);
        if (changed) {
            selectedSlice = newSlice;
            sliceButton.setSlice(selectedSlice);
            NetworkHelper.sendToServer(new C2SSelectSlicePacket(selectedSlice, lecternPos()));
            MapAtlasItem.setSelectedSlice(atlas, selectedSlice, level);
            recalculateDecorationWidgets();
            mapWidget.resetZoom();
        }
        var dim = selectedSlice.dimension();
        NavigableSet<Integer> heights = currentMaps.getHeightTree(dim, selectedSlice.type());
        boolean manyHeights = heights.size() > 1;
        sliceButton.setAvailableChoices(manyHeights, currentMaps.getAvailableTypes(dim).size() != 1);
        sliceDown.setActiveAndVisible(manyHeights);
        sliceUp.setActiveAndVisible(manyHeights);
        sliceDown.setLimitHeight(heights);
        sliceUp.setLimitHeight(heights);
        return changed;
    }

    public boolean isEditingText() {
        return activeTextBox != null;
    }

    public boolean isPlacingPin() {
        return this.selectedCursorAction == CursorAction.PLACING_PIN;
    }

    public boolean isShearing() {
        return this.selectedCursorAction == CursorAction.SHEARING;
    }

    public void clearCursorAction() {
        this.selectedCursorAction = CursorAction.NONE;
    }

    public void toggleCursorAction(CursorAction targetAction) {
        this.selectedCursorAction = (this.selectedCursorAction == targetAction) ? CursorAction.NONE : targetAction;
    }

    public void markCursorActionHasTarget() {
        this.cursorActionHasTarget = true;
    }

    public void shearMapAt(ColumnPos pos) {
        AtlasMap selected = findMapContaining(pos.x(), pos.z());
        if (selected != null) {
            NetworkHelper.sendToServer(new C2SRemoveMapPacket(selected.id, selected.type, lecternPos()));
            removeMapsClientSide(List.of(selected));
        }
        this.clearCursorAction();
    }

    public void shearSlice(Slice slice) {
        NetworkHelper.sendToServer(new C2SRemoveSlicePacket(slice, lecternPos()));
        removeMapsClientSide(currentMaps.getMapsInSlice(slice));
        this.clearCursorAction();
    }

    private void removeMapsClientSide(List<AtlasMap> maps) {
        MapAtlasItem.removeMaps(atlas, level, maps);
        currentMaps = MapAtlasItem.getMaps(atlas, level);
        onMapsChanged();
    }

    public void placePinAt(ColumnPos pos) {
        AtlasMap selected = findMapContaining(pos.x(), pos.z());
        if (selected != null) {
            pinNameBox.setValue("");
            this.partialPin = Pair.of(selected, pos);
            if (hasShiftDown() || hasAltDown()) openPinNameBox();
            else addNewPin();
        }
        this.clearCursorAction();
    }

    private void addNewPin() {
        if (partialPin != null) {
            ClientMarkers.placePin(partialPin.getFirst(), partialPin.getSecond(), pinNameBox.getValue(), pinNameBox.getPinIndex());
            pinNameBox.increasePinIndex();
            closeTextBox();
            partialPin = null;
            this.recalculateDecorationWidgets();
        }
        if (isPinOnly) this.onClose();
    }

    private void openPinNameBox() {
        showTextBox(pinNameBox);
    }

    void openFilterBox() {
        filterBox.setValue("");
        showTextBox(filterBox);
    }

    private void closeTextBox() {
        showTextBox(null);
    }

    private void showTextBox(@Nullable EditBox box) {
        if (activeTextBox != null) setTextBoxOpen(activeTextBox, false);
        activeTextBox = box;
        if (box != null) setTextBoxOpen(box, true);
        this.setFocused(box != null ? box : mapWidget);
    }

    private static void setTextBoxOpen(EditBox box, boolean open) {
        box.active = open;
        box.visible = open;
        box.setCanLoseFocus(!open);
        box.setFocused(open);
    }

    public boolean canTeleport() {
        return hasShiftDown() && MapAtlasItem.canPlayerTeleport(player) &&
                selectedCursorAction == CursorAction.NONE && !isEditingText();
    }

    public Minecraft getMinecraft() {
        return minecraft;
    }

}
