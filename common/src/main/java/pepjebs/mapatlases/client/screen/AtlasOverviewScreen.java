package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.util.Mth;
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
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.config.MapAtlasesConfig;
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

import static pepjebs.mapatlases.client.MapAtlasesClient.*;

public class AtlasOverviewScreen extends Screen {

    private final boolean bigTexture = MapAtlasesClientConfig.worldMapBigTexture.get();
    private final ResourceLocation texture = bigTexture ? ATLAS_BACKGROUND_TEXTURE_BIG : ATLAS_BACKGROUND_TEXTURE;

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
    @Nullable
    private final LecternBlockEntity lectern;

    private MapWidget mapWidget;
    private PinNameBox editBox;
    private SliceBookmarkButton sliceButton;
    private SliceArrowButton sliceUp;
    private SliceArrowButton sliceDown;
    private DimensionListPanel dimensionPanel;
    private DecorationListPanel decorationPanel;
    public final float globalScale;
    private final boolean isPinOnly;
    private Slice selectedSlice;
    private boolean initialized = false;
    private CursorAction selectedCursorAction;
    private boolean canPerformCursorAction;

    // ── Pin flow state ────────────────────────────────────────────────────
    @Nullable
    private Pair<MapDataHolder, ColumnPos> partialPin = null;
    private PinButton pinButton;

    @NotNull
    private MapCollection currentMaps;

    // for fancy menu or something
    public AtlasOverviewScreen() {
        this(MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(Minecraft.getInstance().player), null, false);
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
        MapDataHolder closest = getMapClosestToPlayer();
        this.selectedSlice = closest.slice;

        this.isPinOnly = placingPin;
        this.selectedCursorAction = placingPin ? CursorAction.PLACING_PIN : CursorAction.NONE;
        if (!isPinOnly) {
            this.player.playSound(MapAtlasesMod.ATLAS_OPEN_SOUND_EVENT.get(),
                    (float) (double) MapAtlasesClientConfig.soundScalar.get(), 1.0F);
        } else {
            partialPin = Pair.of(closest, new ColumnPos(player.blockPosition().getX(), player.blockPosition().getZ()));
        }
    }

    @NotNull
    private MapDataHolder getMapClosestToPlayer() {
        this.selectedSlice = MapAtlasItem.getSelectedSlice(atlas, player.level().dimension());
        MapDataHolder closest = currentMaps.getClosest(player, selectedSlice);
        if (closest == null) {
            closest = currentMaps.getAllFound().stream().findFirst().get();
        }
        return closest;
    }

    public ItemStack getAtlas() {
        return atlas;
    }

    public Slice getSelectedSlice() {
        return selectedSlice;
    }

    // ── Initialisation ────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();
        initEditBox();
        initSliceWidgets();
        initDimensionPanel();
        initDecorationPanel();
        initMapWidget();
        initActionButtons();
        initLecternButtons();

        selectDimension(level.dimension());

        if (isPinOnly) focusEditBox(true);
        this.initialized = true;
    }

    private void initEditBox() {
        this.editBox = new PinNameBox(this.font,
                (width - 100) / 2,
                (height - 20) / 2,
                100, 20,
                Component.translatable("message.map_atlases.marker_name"), this::addNewPin);
        // Managed separately; not added as a renderable widget here
    }

    private void initSliceWidgets() {
        this.sliceButton = new SliceBookmarkButton(
                (width + BOOK_WIDTH) / 2 - 13,
                (height - BOOK_HEIGHT) / 2 + (BOOK_HEIGHT - 36),
                selectedSlice, this);
        this.addRenderableWidget(sliceButton);
        sliceUp = new SliceArrowButton(false, sliceButton, this);
        this.addRenderableWidget(sliceUp);
        sliceDown = new SliceArrowButton(true, sliceButton, this);
        this.addRenderableWidget(sliceDown);
    }

    private void initDimensionPanel() {
        dimensionPanel = new DimensionListPanel(
                this,
                (width + BOOK_WIDTH) / 2,
                (height - BOOK_HEIGHT) / 2,
                BOOK_HEIGHT,
                w -> addRenderableWidget(w),
                w -> removeWidget(w));
        dimensionPanel.build(currentMaps.getAvailableDimensions());
    }

    private void initDecorationPanel() {
        decorationPanel = new DecorationListPanel(
                this,
                (width - BOOK_WIDTH) / 2,
                (height - BOOK_HEIGHT) / 2,
                BOOK_HEIGHT,
                w -> addRenderableWidget(w),
                w -> removeWidget(w));
    }

    private void initMapWidget() {
        this.mapWidget = this.addRenderableWidget(new MapWidget(
                (width - MAP_WIDGET_WIDTH) / 2,
                (height - MAP_WIDGET_HEIGHT) / 2 + (bigTexture ? 2 : 5),
                MAP_WIDGET_WIDTH, MAP_WIDGET_HEIGHT, 3,
                this, getMapClosestToPlayer()));
        this.setFocused(mapWidget);
    }

    private void initActionButtons() {
        int rightX = (width + BOOK_WIDTH) / 2 + 20;
        int topY = (height - BOOK_HEIGHT) / 2 + 16;
        int rightOffset = 0;

        if (!MapAtlasesConfig.pinMarkerId.get().isEmpty() && MapAtlasesMod.MOONLIGHT
                && MapAtlasesClientConfig.moonlightCompat.get()) {
            this.pinButton = new PinButton(rightX, topY, this);
            this.addRenderableWidget(pinButton);
            rightOffset += 20;
        }
        if (MapAtlasesClientConfig.shearButton.get()) {
            this.addRenderableWidget(new ShearButton(rightX, topY + rightOffset, this));
        }

        int leftX = (width - BOOK_WIDTH) / 2 - 20 - 16;
        int leftOffset = 0;
        if (MapAtlasesClientConfig.compass.get()) {
            this.addRenderableWidget(new ItemWidget(leftX, topY, this, Items.COMPASS.getDefaultInstance()));
            leftOffset += 20;
        }
        if (MapAtlasesClientConfig.clock.get()) {
            this.addRenderableWidget(new ItemWidget(leftX, topY + leftOffset, this, Items.CLOCK.getDefaultInstance()));
        }
    }

    private void initLecternButtons() {
        if (lectern == null) return;
        int pY = (int) (globalScale * (height + BOOK_HEIGHT + 4) / 2);
        if (player.mayBuild()) {
            this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                    .bounds(this.width / 2 - 100, pY, 98, 20).build());
            this.addRenderableWidget(Button.builder(Component.translatable("lectern.take_book"), b -> {
                NetworkHelper.sendToServer(new C2STakeAtlasPacket(lectern.getBlockPos()));
                this.onClose();
            }).bounds(this.width / 2 + 2, pY, 98, 20).build());
        } else {
            this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                    .bounds(this.width / 2 - 100, pY, 200, 20).build());
        }
    }

    // ── Validity / lifecycle ──────────────────────────────────────────────

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
        this.currentMaps = MapAtlasItem.getMaps(atlas, level);

        if (mapWidget != null) mapWidget.tick();
        if (this.editBox != null && editBox.active) this.editBox.tick();

        if (!isValid()) this.minecraft.setScreen(null);
    }

    // ── Input handling ────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (editBox.active) {
                editBox.active = false;
                editBox.visible = false;
                partialPin = null;
                if (isPinOnly) this.onClose();
                return true;
            } else if (this.selectedCursorAction != CursorAction.NONE) {
                this.selectedCursorAction = CursorAction.NONE;
                return true;
            }
        }
        if (!MapAtlasesClient.PLACE_PIN_KEYBIND.isUnbound()
                && MapAtlasesClient.PLACE_PIN_KEYBIND.matches(pKeyCode, pScanCode)) {
            if (!isPinOnly && pinButton != null) toggleCursorAction(CursorAction.PLACING_PIN);
            return true;
        }
        if (super.keyPressed(pKeyCode, pScanCode, pModifiers) || editBox.keyPressed(pKeyCode, pScanCode, pModifiers)) {
            return true;
        }
        if (!editBox.active && MapAtlasesClient.OPEN_ATLAS_KEYBIND.matches(pKeyCode, pScanCode)) {
            this.onClose();
            return true;
        }
        for (var v : decorationPanel.getVisibleButtons()) {
            if (v.keyPressed(pKeyCode, pScanCode, pModifiers)) return true;
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

    // ── Rendering ─────────────────────────────────────────────────────────

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(graphics);

        graphics.pose().pushPose();
        graphics.pose().translate(width / 2f, height / 2f, 0);

        RenderSystem.enableDepthTest();
        graphics.blit(texture, -H_BOOK_WIDTH, -H_BOOK_HEIGHT, 0, 0, BOOK_WIDTH, BOOK_HEIGHT, TEXTURE_W, 256);
        graphics.blit(ATLAS_OVERLAY_TEXTURE, -H_BOOK_WIDTH, -H_BOOK_HEIGHT, 0, 0, BOOK_WIDTH, BOOK_HEIGHT, TEXTURE_W, 256);

        graphics.pose().translate(0, 0, 1);
        graphics.blit(texture, H_BOOK_WIDTH - 10, -H_BOOK_HEIGHT, OVERLAY_UR, 0, 5, BOOK_HEIGHT, TEXTURE_W, 256);
        graphics.blit(texture, -H_BOOK_WIDTH + 5, -H_BOOK_HEIGHT, OVERLAY_UL, 0, 5, BOOK_HEIGHT, TEXTURE_W, 256);
        graphics.pose().popPose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        PoseStack poseStack = graphics.pose();

        if (!isPinOnly) {
            poseStack.pushPose();
            poseStack.translate(width / 2f, height / 2f, 0);
            poseStack.scale(globalScale, globalScale, 1);
            poseStack.pushPose();
            RenderSystem.enableDepthTest();
            poseStack.translate(-width / 2f, -height / 2f, 0.2);
            var v = transformMousePos(mouseX, mouseY);
            super.render(graphics, (int) v.x, (int) v.y, delta);
            poseStack.popPose();
            poseStack.popPose();
        }

        if (editBox.active) {
            editBox.render(graphics, mouseX, mouseY, delta);
        } else if (MapAtlasesClientConfig.worldMapCrossair.get()) {
            poseStack.pushPose();
            poseStack.translate(0, 0, 5);
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            graphics.blit(GUI_ICONS_TEXTURE, (width - 15) / 2, (height - 15) / 2, 0, 0, 15, 15);
            RenderSystem.defaultBlendFunc();
            poseStack.popPose();
        }

        ResourceLocation cursorIcon = selectedCursorAction.getIcon(canPerformCursorAction);
        if (cursorIcon != null) {
            poseStack.pushPose();
            poseStack.translate(mouseX - 2.5f, mouseY - 2.5f, 10);
            graphics.blitSprite(cursorIcon, 0, 0, 8, 8);
            poseStack.popPose();
        }
        this.canPerformCursorAction = false;
    }

    // ── Mouse handling ────────────────────────────────────────────────────

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!editBox.active) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        return editBox.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void mouseMoved(double pMouseX, double pMouseY) {
        if (!editBox.active) {
            var v = transformMousePos(pMouseX, pMouseY);
            super.mouseMoved(v.x, v.y);
        }
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (!editBox.active) {
            var v = transformMousePos(pMouseX, pMouseY);
            return super.mouseClicked(v.x, v.y, pButton);
        }
        return editBox.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        if (!editBox.active) {
            var v = transformMousePos(pMouseX, pMouseY);
            return super.mouseDragged(v.x, v.y, pButton, pDragX, pDragY);
        }
        return editBox.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
    }

    public Vector4d transformMousePos(double mouseX, double mouseZ) {
        return AtlasScreenUtils.scaleVector(mouseX, mouseZ, 1 / globalScale, width, height);
    }

    public Vector4d transformPos(double mouseX, double mouseZ) {
        return AtlasScreenUtils.scaleVector(mouseX, mouseZ, globalScale, width, height);
    }

    // ── Map queries ───────────────────────────────────────────────────────

    public MapItemSavedData getCenterMapForSelectedDim() {
        if (selectedSlice.dimension().equals(level.dimension())) {
            return getMapClosestToPlayer().data;
        }
        MapItemSavedData best = null;
        float averageX = 0;
        float averageZ = 0;
        int count = 0;
        for (MapDataHolder holder : currentMaps.selectSection(selectedSlice)) {
            MapItemSavedData d = holder.data;
            averageX += d.centerX;
            averageZ += d.centerZ;
            count++;
            if (d.decorations.values().stream().anyMatch(e -> e.type().value().showOnItemFrame())) {
                if (best != null) {
                    if (Mth.lengthSquared(best.centerX, best.centerZ) > Mth.lengthSquared(d.centerX, d.centerZ)) {
                        best = d;
                    }
                } else best = d;
            }
        }
        if (best != null) return best;
        if (count == 0) return null;
        averageX /= count;
        averageZ /= count;
        MapDataHolder closest = currentMaps.getClosest(averageX, averageZ, selectedSlice);
        return closest == null ? null : closest.data;
    }

    @Nullable
    protected MapDataHolder findMapWithCenter(int reqXCenter, int reqZCenter) {
        return currentMaps.select(reqXCenter, reqZCenter, selectedSlice);
    }

    @Nullable
    protected MapDataHolder findMapContaining(int x, int z) {
        return currentMaps.select(MapGridKey.at(currentMaps.getScale(), selectedSlice, x, z));
    }

    // ── Dimension & slice selection ───────────────────────────────────────

    public void selectDimension(ResourceKey<Level> dimension) {
        // sameDim = true means we are staying on (or re-selecting) the current dimension;
        // in that case, we rebuild the slice object but keep the same dimension key.
        boolean sameDim = selectedSlice.dimension().equals(dimension);
        if (sameDim) this.selectedSlice = new Slice(selectedSlice.type(), selectedSlice.height(), dimension);
        // On first call from init we keep the atlas's saved slice; afterwards use the per-dim saved slice.
        updateSlice(!initialized ? selectedSlice : MapAtlasItem.getSelectedSlice(atlas, dimension));
        boolean isWherePlayerIs = level.dimension().equals(dimension);

        MapItemSavedData center = isWherePlayerIs ? getMapClosestToPlayer().data : this.getCenterMapForSelectedDim();
        if (center == null) return;
        this.mapWidget.resetAndCenter(center.centerX, center.centerZ, isWherePlayerIs, sameDim);
        dimensionPanel.setSelectedDimension(dimension);
        recalculateDecorationWidgets();

        TreeSet<Integer> tree = currentMaps.getHeightTree(selectedSlice.dimension(), selectedSlice.type());
        this.sliceDown.setMaxSlice(tree);
        this.sliceUp.setMaxSlice(tree);
    }

    protected void recalculateDecorationWidgets() {
        List<DecorationHolder> mapIcons = new ArrayList<>();
        boolean ml = MapAtlasesMod.MOONLIGHT;
        for (MapDataHolder holder : currentMaps.selectSection(selectedSlice)) {
            MapItemSavedData data = holder.data;
            for (var d : data.decorations.entrySet()) {
                MapDecoration deco = d.getValue();
                if (deco.renderOnFrame() && !deco.type().is(MapAtlasesMod.NON_REMOVABLE_DECORATIONS)) {
                    mapIcons.add(new DecorationHolder(deco, d.getKey(), holder));
                }
            }
            if (ml) mapIcons.addAll(MoonlightCompat.getCustomDecorations(holder));
        }
        decorationPanel.rebuild(mapIcons);
    }

    public void updateVisibleDecoration(int currentXCenter, int currentZCenter, float radius, boolean followingPlayer) {
        decorationPanel.updateVisible(currentXCenter, currentZCenter, radius, followingPlayer);
    }

    public void centerOnDecoration(DecorationBookmarkButton button) {
        int x = (int) button.getWorldX();
        int z = (int) button.getWorldZ();
        this.mapWidget.resetAndCenter(x, z, false, true);
    }

    public boolean decreaseSlice() {
        int current = selectedSlice.heightOrTop();
        MapType type = selectedSlice.type();
        ResourceKey<Level> dim = selectedSlice.dimension();
        Integer newHeight = currentMaps.getHeightTree(dim, type).floor(current - 1);
        if (newHeight != null) return updateSlice(Slice.of(type, newHeight, dim));
        return false;
    }

    public boolean increaseSlice() {
        int current = selectedSlice.heightOrTop();
        MapType type = selectedSlice.type();
        ResourceKey<Level> dim = selectedSlice.dimension();
        Integer newHeight = currentMaps.getHeightTree(dim, type).ceiling(current + 1);
        if (newHeight != null) return updateSlice(Slice.of(type, newHeight, dim));
        return false;
    }

    public void cycleSliceType() {
        ResourceKey<Level> dim = selectedSlice.dimension();
        var slices = new ArrayList<>(currentMaps.getAvailableTypes(dim));
        if (!slices.isEmpty()) {
            int index = (slices.indexOf(selectedSlice.type()) + 1) % slices.size();
            MapType type = slices.get(index);
            TreeSet<Integer> heightTree = currentMaps.getHeightTree(dim, type);
            Integer h = heightTree.floor(selectedSlice.heightOrTop());
            if (h == null) h = heightTree.first();
            updateSlice(Slice.of(type, h, dim));
        }
    }

    private boolean updateSlice(Slice newSlice) {
        boolean changed = false;
        if (!Objects.equals(selectedSlice, newSlice)) {
            selectedSlice = newSlice;
            sliceButton.setSlice(selectedSlice);
            NetworkHelper.sendToServer(new C2SSelectSlicePacket(selectedSlice,
                    Optional.ofNullable(lectern).map(BlockEntity::getBlockPos)));
            MapAtlasItem.setSelectedSlice(atlas, selectedSlice, level);
            recalculateDecorationWidgets();
            changed = true;
        }
        var dim = selectedSlice.dimension();
        boolean manySlices = currentMaps.getHeightTree(dim, selectedSlice.type()).size() > 1;
        boolean manyTypes = currentMaps.getAvailableTypes(dim).size() != 1;
        sliceButton.refreshState(manySlices, manyTypes);
        sliceDown.setActive(manySlices);
        sliceUp.setActive(manySlices);
        mapWidget.resetZoom();
        return changed;
    }

    // ── Cursor action state ───────────────────────────────────────────────

    public boolean isEditingText() {
        return editBox.active;
    }

    public boolean isPlacingPin() {
        return this.selectedCursorAction == CursorAction.PLACING_PIN;
    }

    public void clearCursorAction() {
        this.selectedCursorAction = CursorAction.NONE;
    }

    public void toggleCursorAction(CursorAction targetAction) {
        this.selectedCursorAction = (this.selectedCursorAction == targetAction) ? CursorAction.NONE : targetAction;
    }

    public void notifyOfClickActionUsage() {
        this.canPerformCursorAction = true;
    }

    public boolean isShearing() {
        return this.selectedCursorAction == CursorAction.SHEARING;
    }

    public void shearMapAt(ColumnPos pos) {
        MapDataHolder selected = findMapContaining(pos.x(), pos.z());
        if (selected != null) {
            NetworkHelper.sendToServer(new C2SRemoveMapPacket(selected.id, selected.type));
            currentMaps.removeDataAndAssign(atlas, level, selected);
            recalculateDecorationWidgets();
        }
        this.clearCursorAction();
    }

    public void shearSlice(Slice slice) {
        NetworkHelper.sendToServer(new C2SRemoveSlicePacket(slice));
        currentMaps.removeSliceAndAssign(atlas, level, slice);
        recalculateDecorationWidgets();
        this.clearCursorAction();
    }

    // ── Pin flow ──────────────────────────────────────────────────────────

    public void placePinAt(ColumnPos pos) {
        MapDataHolder selected = findMapContaining(pos.x(), pos.z());
        if (selected != null) {
            editBox.setValue("");
            this.partialPin = Pair.of(selected, pos);
            if (hasShiftDown() || hasAltDown()) {
                focusEditBox(true);
            } else {
                addNewPin();
            }
        }
        this.clearCursorAction();
    }

    private void focusEditBox(boolean on) {
        editBox.active = on;
        editBox.visible = on;
        editBox.setCanLoseFocus(!on);
        editBox.setFocused(on);
        this.setFocused(on ? editBox : mapWidget);
        if (!on && isPinOnly) this.onClose();
    }

    private void addNewPin() {
        if (partialPin != null) {
            String text = editBox.getValue();
            PinButton.placePin(partialPin.getFirst(), partialPin.getSecond(), text, editBox.getIndex());
            editBox.increasePinIndex();
            focusEditBox(false);
            partialPin = null;
            this.recalculateDecorationWidgets();
        }
    }

    // ── Misc ──────────────────────────────────────────────────────────────

    public boolean canTeleport() {
        return hasShiftDown() && minecraft.gameMode.getPlayerMode().isCreative() &&
                selectedCursorAction == CursorAction.NONE && !editBox.active;
    }

    public Minecraft getMinecraft() {
        return minecraft;
    }

}