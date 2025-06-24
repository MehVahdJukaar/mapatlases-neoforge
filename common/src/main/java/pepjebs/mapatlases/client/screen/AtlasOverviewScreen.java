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
import org.joml.Matrix4d;
import org.joml.Vector4d;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.moonlight.MoonlightCompat;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.map_collection.MapSearchKey;
import pepjebs.mapatlases.networking.C2SRemoveMapPacket;
import pepjebs.mapatlases.networking.C2SSelectSlicePacket;
import pepjebs.mapatlases.networking.C2STakeAtlasPacket;
import pepjebs.mapatlases.utils.*;

import java.util.*;

import static pepjebs.mapatlases.client.MapAtlasesClient.*;

//in retrospective, we should have kept the menu
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
    private final List<DecorationBookmarkButton> decorationBookmarks = new ArrayList<>();
    private final List<DimensionBookmarkButton> dimensionBookmarks = new ArrayList<>();
    public final float globalScale;
    private final boolean isPinOnly;
    private Slice selectedSlice;
    private boolean initialized = false;
    private boolean placingPin;
    private boolean shearing;
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
        //improve for wrong dimension atlas
        this.selectedSlice = closest.slice;

        this.isPinOnly = placingPin;
        this.placingPin = placingPin;
        if (!isPinOnly) {
            // Play open sound
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
            //if it has no maps here, grab a random one
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


    @Override
    protected void init() {
        super.init();

        this.editBox = new PinNameBox(this.font,
                (width - 100) / 2,
                (height - 20) / 2,
                100, 20,
                Component.translatable("message.map_atlases.marker_name"), this::addNewPin);
        //we manage this separately on its own

        this.sliceButton = new SliceBookmarkButton(
                (width + BOOK_WIDTH) / 2 - 13,
                (height - BOOK_HEIGHT) / 2 + (BOOK_HEIGHT - 36),
                selectedSlice, this);
        this.addRenderableWidget(sliceButton);
        sliceUp = new SliceArrowButton(false, sliceButton, this);
        this.addRenderableWidget(sliceUp);
        sliceDown = new SliceArrowButton(true, sliceButton, this);
        this.addRenderableWidget(sliceDown);

        int i = 0;
        Collection<ResourceKey<Level>> dimensions = currentMaps.getAvailableDimensions();
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
                (height - MAP_WIDGET_HEIGHT) / 2 + (bigTexture ? 2 : 5),
                MAP_WIDGET_WIDTH, MAP_WIDGET_HEIGHT, 3,
                this, getMapClosestToPlayer()));

        this.setFocused(mapWidget);

        int by = 0;
        if (!MapAtlasesConfig.pinMarkerId.get().isEmpty() && MapAtlasesMod.MOONLIGHT && MapAtlasesClientConfig.moonlightCompat.get()) {
            this.pinButton = new PinButton((width + BOOK_WIDTH) / 2 + 20,
                    (height - BOOK_HEIGHT) / 2 + 16, this);
            this.addRenderableWidget(pinButton);
            by += 20;
        }
        if (MapAtlasesClientConfig.shearButton.get()) {
            ShearButton shearButton = new ShearButton((width + BOOK_WIDTH) / 2 + 20,
                    (height - BOOK_HEIGHT) / 2 + 16 + by, this);
            this.addRenderableWidget(shearButton);
        }
        int bby = 0;

        if (MapAtlasesClientConfig.compass.get()) {
            this.addRenderableWidget(new ItemWidget((width - BOOK_WIDTH) / 2 - 20 - 16,
                    (height - BOOK_HEIGHT) / 2 + 16, this, Items.COMPASS.getDefaultInstance()));
            bby += 20;
        }

        if (MapAtlasesClientConfig.clock.get()) {
            this.addRenderableWidget(new ItemWidget((width - BOOK_WIDTH) / 2 - 20 - 16,
                    (height - BOOK_HEIGHT) / 2 + 16 + bby, this, Items.CLOCK.getDefaultInstance()));
        }

        this.selectDimension(level.dimension());

        if (lectern != null) {
            int pY = (int) (globalScale * (height + BOOK_HEIGHT + 4) / 2);
            if (player.mayBuild()) {
                this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (button) -> {
                    this.onClose();
                }).bounds(this.width / 2 - 100, pY, 98, 20).build());
                this.addRenderableWidget(Button.builder(Component.translatable("lectern.take_book"), (button) -> {
                    NetworkHelper.sendToServer(new C2STakeAtlasPacket(lectern.getBlockPos()));
                    this.onClose();
                }).bounds(this.width / 2 + 2, pY, 98, 20).build());
            } else {
                this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (button) -> {
                    this.onClose();
                }).bounds(this.width / 2 - 100, pY, 200, 20).build());
            }
        }

        if (isPinOnly) focusEditBox(true);

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
        this.currentMaps = MapAtlasItem.getMaps(atlas, level);

        if (mapWidget != null) {
            mapWidget.tick();
        }
        if (this.editBox != null && editBox.active) {
            this.editBox.tick();
        }

        //TODO: update widgets
        //recalculate parameters
        if (!isValid()) {
            this.minecraft.setScreen(null);
        }
        //add lectern marker
        if (false && lectern != null && selectedSlice.dimension().equals(lectern.getLevel().dimension())) {
            var data = currentMaps.getClosest(
                    lectern.getBlockPos().getX(), lectern.getBlockPos().getZ(),
                    selectedSlice).data;
        }
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == 256) {
            if (editBox.active) {
                editBox.active = false;
                editBox.visible = false;
                partialPin = null;
                if (isPinOnly) {
                    this.onClose();
                }
                return true;
            } else if (placingPin) {
                placingPin = false;
                return true;
            } else if (shearing) {
                shearing = false;
                return true;
            }
        }
        if (!MapAtlasesClient.PLACE_PIN_KEYBIND.isUnbound() && MapAtlasesClient.PLACE_PIN_KEYBIND.matches(pKeyCode, pScanCode)) {
            if (!isPinOnly && pinButton != null) {
                this.togglePlacingPin();
            }
            return true;
        }
        if (super.keyPressed(pKeyCode, pScanCode, pModifiers) || editBox.keyPressed(pKeyCode, pScanCode, pModifiers)) {
            return true;
        }
        if (!editBox.active && MapAtlasesClient.OPEN_ATLAS_KEYBIND.matches(pKeyCode, pScanCode)) {
            this.onClose();
            return true;
        }
        for (var v : decorationBookmarks) {
            if (v.keyPressed(pKeyCode, pScanCode, pModifiers)) return true;
        }
        return false;
    }

    @Override
    public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
        for (var v : decorationBookmarks) {
            v.keyReleased(pKeyCode, pScanCode, pModifiers);
        }
        return super.keyReleased(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    protected void renderMenuBackground(GuiGraphics graphics, int x, int y, int width, int height) {
        super.renderMenuBackground(graphics, x, y, width, height);

        graphics.pose().pushPose();
        graphics.pose().translate(width / 2f, height / 2f, 0);

        RenderSystem.enableDepthTest();
        //background
        graphics.blit(
                texture,
                -H_BOOK_WIDTH,
                -H_BOOK_HEIGHT,
                0,
                0,
                BOOK_WIDTH,
                BOOK_HEIGHT,
                TEXTURE_W,
                256
        );
        // Draw foreground
        graphics.blit(
                ATLAS_OVERLAY_TEXTURE,
                -H_BOOK_WIDTH,
                -H_BOOK_HEIGHT,
                0,
                0,
                BOOK_WIDTH,
                BOOK_HEIGHT,
                TEXTURE_W,
                256
        );

        graphics.pose().translate(0, 0, 1);
        //background overlay

        graphics.blit(
                texture,
                H_BOOK_WIDTH - 10,
                -H_BOOK_HEIGHT,
                OVERLAY_UR,
                0,
                5,
                BOOK_HEIGHT,
                TEXTURE_W,
                256
        );
        graphics.blit(
                texture,
                -H_BOOK_WIDTH + 5,
                -H_BOOK_HEIGHT,
                OVERLAY_UL,
                0,
                5,
                BOOK_HEIGHT,
                TEXTURE_W,
                256
        );
        graphics.pose().popPose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        PoseStack poseStack = graphics.pose();

        if (!isPinOnly) {

            poseStack.pushPose();

            poseStack.translate(width / 2f, height / 2f, 0);
            poseStack.scale(globalScale, globalScale, 1);

            //render widgets
            poseStack.pushPose();
            RenderSystem.enableDepthTest();

            poseStack.translate(-width / 2f, -height / 2f, 0.2);
            var v = transformMousePos(mouseX, mouseY);
            super.render(graphics, (int) v.x, (int) v.y, delta);
            poseStack.popPose();


            poseStack.popPose();
        }
        poseStack.pushPose();
        RenderSystem.enableDepthTest();
        poseStack.translate(0, 0, editBox.active ? 22 : -20);
        //renderBackground(graphics);
        poseStack.popPose();

        if (editBox.active) editBox.render(graphics, mouseX, mouseY, delta);

        else if (MapAtlasesClientConfig.worldMapCrossair.get()) {
            poseStack.pushPose();
            poseStack.translate(0, 0, 5);
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            graphics.blit(GUI_ICONS_TEXTURE, (width - 15) / 2, (height - 15) / 2,
                    0, 0, 15, 15);
            RenderSystem.defaultBlendFunc();

            poseStack.popPose();
        }
    }

    // ================== Mouse Functions ==================


    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!editBox.active) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
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
        } else return editBox.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        if (!editBox.active) {
            var v = transformMousePos(pMouseX, pMouseY);
            return super.mouseDragged(v.x, v.y, pButton, pDragX, pDragY);
        } else return editBox.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
    }

    public Vector4d transformMousePos(double mouseX, double mouseZ) {
        return scaleVector(mouseX, mouseZ, 1 / (globalScale), width, height);
    }

    public Vector4d transformPos(double mouseX, double mouseZ) {
        return scaleVector(mouseX, mouseZ, globalScale, width, height);
    }

    // ================== Other Util Fns ==================

    public MapItemSavedData getCenterMapForSelectedDim() {
        if (selectedSlice.dimension().equals(level.dimension())) {
            return getMapClosestToPlayer().data;
        } else {
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
            if (count == 0) {
                return null;
            }
            averageX /= count;
            averageZ /= count;
            MapDataHolder closest = currentMaps.getClosest(averageX, averageZ, selectedSlice);
            if (closest == null) {
                int error = 1;
            }
            return closest == null ? null : closest.data;
            //centers to any map that has decoration
        }
    }

    @Nullable
    protected MapDataHolder findMapWithCenter(int reqXCenter, int reqZCenter) {
        return currentMaps.select(reqXCenter, reqZCenter, selectedSlice);
    }

    @Nullable
    protected MapDataHolder findMapContaining(int x, int z) {
        return currentMaps.select(MapSearchKey.at(currentMaps.getScale(), x, z, selectedSlice));
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
        boolean changedDim = selectedSlice.dimension().equals(dimension);
        if (changedDim) this.selectedSlice = new Slice(selectedSlice.type(), selectedSlice.height(), dimension);
        //we dont change slice when calling this from init as we want to use the atlas initial slice
        updateSlice(!initialized ? selectedSlice : MapAtlasItem.getSelectedSlice(atlas, dimension));
        boolean isWherePlayerIs = level.dimension().equals(dimension);

        MapItemSavedData center = isWherePlayerIs ? getMapClosestToPlayer().data : this.getCenterMapForSelectedDim();
        if (center == null) {
            int error = 0;
            return;
        }
        this.mapWidget.resetAndCenter(center.centerX, center.centerZ, isWherePlayerIs, changedDim);
        for (var v : dimensionBookmarks) {
            v.setSelected(v.getDimension().equals(dimension));
        }
        recalculateDecorationWidgets();

        TreeSet<Integer> tree = currentMaps.getHeightTree(selectedSlice.dimension(), selectedSlice.type());
        this.sliceDown.setMaxSlice(tree);
        this.sliceUp.setMaxSlice(tree);
    }

    public void recalculateDecorationWidgets() {
        for (var v : decorationBookmarks) {
            this.removeWidget(v);
        }
        decorationBookmarks.clear();
        addDecorationWidgets();
    }

    public void updateVisibleDecoration(int currentXCenter, int currentZCenter, float radius, boolean followingPlayer) {
        if (decorationBookmarks.isEmpty()) return;
        float minX = currentXCenter - radius;
        float maxX = currentXCenter + radius;
        float minZ = currentZCenter - radius;
        float maxZ = currentZCenter + radius;
        // Create a list to store selected decorations
        // Create a TreeMap to store selected decorations sorted by distance
        List<Pair<Double, DecorationBookmarkButton>> byDistance = new ArrayList<>();
        for (var bookmark : decorationBookmarks) {
            double x = bookmark.getWorldX();
            double z = bookmark.getWorldZ();
            // Check if the decoration is within the specified range
            if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) bookmark.setSelected(true);
            if (followingPlayer) {
                double distance = Mth.square(x - currentXCenter) + Mth.square(z - currentZCenter);
                // Store the decoration in the TreeMap with distance as the key
                byDistance.add(Pair.of(distance, bookmark));
            }
        }
        //TODO: maybe this isnt needed
        if (followingPlayer) {
            int index = 0;
            int maxW = BOOK_HEIGHT - 24;
            int separation = Math.min(17, maxW / byDistance.size());

            byDistance.sort(Comparator.comparingDouble(Pair::getFirst));
            for (var e : byDistance) {
                var d = e.getSecond();
                d.setY((height - BOOK_HEIGHT) / 2 + 15 + index * separation);
                d.setIndex(index);
                index++;
            }
        }
    }

    private void addDecorationWidgets() {
        if (!this.selectedSlice.hasMarkers()) return;
        List<DecorationHolder> mapIcons = new ArrayList<>();

        boolean ml = MapAtlasesMod.MOONLIGHT;
        for (MapDataHolder holder : currentMaps.selectSection(selectedSlice)) {
            MapItemSavedData data = holder.data;
            for (var d : data.decorations.entrySet()) {
                MapDecoration deco = d.getValue();
                if (deco.renderOnFrame()) {
                    mapIcons.add(new DecorationHolder(deco, d.getKey(), holder));
                }
            }
            if (ml) {
                mapIcons.addAll(MoonlightCompat.getCustomDecorations(holder));
            }
        }
        int i = 0;

        int separation = Math.min(17, (int) ((BOOK_HEIGHT - 22f) / mapIcons.size()));
        List<DecorationBookmarkButton> widgets = new ArrayList<>();
        for (var e : mapIcons) {
            DecorationBookmarkButton pWidget = DecorationBookmarkButton.of(
                    (width - BOOK_WIDTH) / 2 + 10,
                    (height - BOOK_HEIGHT) / 2 + 15 + i * separation, e, this);
            pWidget.setIndex(i);
            widgets.add(pWidget);
            this.decorationBookmarks.add(pWidget);
            i++;
        }
        //add widget in order so they render optimized without unneded texture swaps
        widgets.sort(Comparator.comparingInt(DecorationBookmarkButton::getBatchGroup));
        widgets.forEach(this::addRenderableWidget);
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
        if (newHeight != null) {
            return updateSlice(Slice.of(type, newHeight, dim));
        }
        return false;
    }

    public boolean increaseSlice() {
        int current = selectedSlice.heightOrTop();
        MapType type = selectedSlice.type();
        ResourceKey<Level> dim = selectedSlice.dimension();
        TreeSet<Integer> tree = currentMaps.getHeightTree(dim, type);
        Integer newHeight = tree.ceiling(current + 1);
        if (newHeight != null) {
            return updateSlice(Slice.of(type, newHeight, dim));
        }
        return false;

    }

    public void cycleSliceType() {
        ResourceKey<Level> dim = selectedSlice.dimension();
        var slices = new ArrayList<>(currentMaps.getAvailableTypes(dim));
        if (!slices.isEmpty()) {
            int index = slices.indexOf(selectedSlice.type());
            index = (index + 1) % slices.size();
            MapType type = slices.get(index);
            TreeSet<Integer> heightTree = currentMaps.getHeightTree(dim, type);
            Integer ceiling = heightTree.floor(selectedSlice.heightOrTop());
            if (ceiling == null) ceiling = heightTree.first();
            updateSlice(Slice.of(type, ceiling, dim));
        }
    }

    private boolean updateSlice(Slice newSlice) {
        boolean changed = false;
        if (!Objects.equals(selectedSlice, newSlice)) {
            selectedSlice = newSlice;
            sliceButton.setSlice(selectedSlice);
            //notify server
            NetworkHelper.sendToServer(new C2SSelectSlicePacket(selectedSlice,
                    Optional.ofNullable(lectern).map(BlockEntity::getBlockPos)));
            //update the client immediately
            MapAtlasItem.setSelectedSlice(atlas, selectedSlice, level);
            recalculateDecorationWidgets();
            changed = true;
        }
        //update button regardless
        var dim = selectedSlice.dimension();
        boolean manySlices = currentMaps.getHeightTree(dim, selectedSlice.type()).size() > 1;
        boolean manyTypes = currentMaps.getAvailableTypes(dim).size() != 1;
        sliceButton.refreshState(manySlices, manyTypes);
        sliceDown.setActive(manySlices);
        sliceUp.setActive(manySlices);
        mapWidget.resetZoom();
        return changed;
    }

    public boolean isEditingText() {
        return editBox.active;
    }

    public boolean isPlacingPin() {
        return placingPin;
    }

    public void togglePlacingPin() {
        this.placingPin = !this.placingPin;
        if (this.placingPin) {
            this.shearing = false;
        }
    }

    public boolean isShearing() {
        return shearing;
    }

    public void toggleShearing() {
        this.shearing = !this.shearing;
        if (this.shearing) {
            this.placingPin = false;
        }
    }

    public void shearMapAt(ColumnPos pos) {
        MapDataHolder selected = findMapContaining(pos.x(), pos.z());
        if (selected != null) {
            NetworkHelper.sendToServer(new C2SRemoveMapPacket(selected.id, selected.type));
            //also remove immediately
            currentMaps.removeAndAssigns(atlas, level, selected.id, selected.type);
            recalculateDecorationWidgets();
        }
        shearing = false;
    }

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
        placingPin = false;
    }

    private void focusEditBox(boolean on) {
        editBox.active = on;
        editBox.visible = on;
        editBox.setCanLoseFocus(!on);
        editBox.setFocused(on);
        this.setFocused(on ? editBox : mapWidget);
        if (!on && isPinOnly) this.onClose();
    }

    // Actually places pin and update screen accordingly
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


    public boolean canTeleport() {
        return hasShiftDown() && minecraft.gameMode.getPlayerMode().isCreative() && !placingPin && !editBox.active;
    }


    public static Vector4d scaleVector(double mouseX, double mouseZ, float scale, int w, int h) {
        Matrix4d matrix4d = new Matrix4d();

        // Calculate the translation and scaling factors
        double translateX = w / 2.0;
        double translateY = h / 2.0;
        double scaleFactor = scale - 1.0;

        // Apply translation to the matrix (combined)
        matrix4d.translate(translateX, translateY, 0);

        // Apply scaling to the matrix
        matrix4d.scale(1.0 + scaleFactor);

        // Apply translation back to the original position (combined)
        matrix4d.translate(-translateX, -translateY, 0);

        // Create a vector with the input coordinates
        Vector4d v = new Vector4d(mouseX, mouseZ, 0, 1.0F);

        // Apply the transformation matrix to the vector
        matrix4d.transform(v);
        return v;
    }

    public Minecraft getMinecraft() {
        return minecraft;
    }

    public void removeMapAt(double mouseX, double mouseY) {
        //find map at pos
        Vector4d v = transformMousePos(mouseX, mouseY);
        MapDataHolder map = currentMaps.select((int) v.x, (int) v.y, selectedSlice);
        if (map != null) {
            NetworkHelper.sendToServer(new C2SRemoveMapPacket(map.id, map.type));
        }
    }
}