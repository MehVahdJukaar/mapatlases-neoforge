package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector4f;
import net.minecraft.client.Minecraft;
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
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.integration.moonlight.MoonlightCompat;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.IMapCollection;
import pepjebs.mapatlases.networking.C2SSelectSlicePacket;
import pepjebs.mapatlases.networking.C2STakeAtlasPacket;
import pepjebs.mapatlases.networking.MapAtlasesNetworking;
import pepjebs.mapatlases.utils.DecorationHolder;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.MapType;
import pepjebs.mapatlases.utils.Slice;

import java.util.*;

//in retrospective, we should have kept the menu
public class AtlasOverviewScreen extends Screen {

    public static final ResourceLocation ATLAS_OVERLAY = MapAtlasesMod.res("textures/gui/screen/atlas_overlay.png");
    public static final ResourceLocation ATLAS_TEXTURE = MapAtlasesMod.res("textures/gui/screen/atlas_background.png");
    public static final ResourceLocation ATLAS_TEXTURE_BIG = MapAtlasesMod.res("textures/gui/screen/atlas_background_big.png");
    public static final ResourceLocation GUI_ICONS = new ResourceLocation("textures/gui/icons.png");

    private final boolean bigTexture = MapAtlasesClientConfig.worldMapBigTexture.get();
    private final ResourceLocation texture = bigTexture ? ATLAS_TEXTURE_BIG : ATLAS_TEXTURE;

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
    private SliceBookmarkButton sliceButton;
    private SliceArrowButton sliceUp;
    private SliceArrowButton sliceDown;
    private final List<DecorationBookmarkButton> decorationBookmarks = new ArrayList<>();
    private final List<DimensionBookmarkButton> dimensionBookmarks = new ArrayList<>();
    public final float globalScale;
    private Slice selectedSlice;
    private boolean initialized = false;

    public AtlasOverviewScreen(ItemStack atlas) {
        this(atlas, null, false);
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

        MapDataHolder closest = getMapClosestToPlayer();
        //improve for wrong dimension atlas
        this.selectedSlice = closest.slice;

        // Play open sound
        this.player.playSound(MapAtlasesMod.ATLAS_OPEN_SOUND_EVENT.get(),
                (float) (double) MapAtlasesClientConfig.soundScalar.get(), 1.0F);
    }

    @NotNull
    private MapDataHolder getMapClosestToPlayer() {
        IMapCollection maps = MapAtlasItem.getMaps2(atlas, level);
        this.selectedSlice = MapAtlasItem.getSelectedSlice2(atlas, player.level.dimension());
        MapDataHolder closest = maps.getClosest(player, selectedSlice);
        if (closest == null) {
            //if it has no maps here, grab a random one
            closest = maps.getAll().stream().findFirst().get();
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
        IMapCollection maps = MapAtlasItem.getMaps2(atlas, level);
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
                (height - MAP_WIDGET_HEIGHT) / 2 + (bigTexture ? 2 : 5),
                MAP_WIDGET_WIDTH, MAP_WIDGET_HEIGHT, 3,
                this, getMapClosestToPlayer()));

        this.setFocused(mapWidget);

        this.selectDimension(level.dimension());

        if (lectern != null) {
            int pY = (int) (globalScale * (height + BOOK_HEIGHT + 4) / 2);
            if (this.minecraft.player.mayBuild()) {
                this.addRenderableWidget(new Button(this.width / 2 - 100, pY, 98, 20, CommonComponents.GUI_DONE, (p_99033_) -> {
                    this.onClose();
                }));
                this.addRenderableWidget(new Button(this.width / 2 + 2, pY, 98, 20, Component.translatable("lectern.take_book"), (p_99024_) -> {
                    MapAtlasesNetworking.CHANNEL.sendToServer(new C2STakeAtlasPacket(lectern.getBlockPos()));
                    this.onClose();

                }));
            } else {
                this.addRenderableWidget(new Button(this.width / 2 - 100, pY, 200, 20, CommonComponents.GUI_DONE, (p_99033_) -> {
                    this.onClose();
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
        if (mapWidget != null) {
            mapWidget.tick();
        }
        //TODO: update widgets
        //recalculate parameters
        if (!isValid()) {
            this.minecraft.setScreen(null);
        }
        //add lectern marker
        if (false && lectern != null && selectedSlice.dimension().equals(lectern.getLevel().dimension())) {
            var data = MapAtlasItem.getMaps2(atlas, level).getClosest(
                    lectern.getBlockPos().getX(), lectern.getBlockPos().getZ(),
                    selectedSlice).data;
        }
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (super.keyPressed(pKeyCode, pScanCode, pModifiers)) {
            return true;
        }
        if (MapAtlasesClient.OPEN_ATLAS_KEYBIND.matches(pKeyCode, pScanCode)) {
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
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        poseStack.pushPose();

        poseStack.translate(width / 2f, height / 2f, 0);
        poseStack.scale(globalScale, globalScale, 1);


        poseStack.pushPose();

        RenderSystem.enableDepthTest();
        RenderSystem.setShaderTexture(0, texture);
        //background
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
        // Draw foreground
        RenderSystem.setShaderTexture(0, ATLAS_OVERLAY);
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

        poseStack.translate(0, 0, 1);
        RenderSystem.setShaderTexture(0, texture);
        //background overlay
        blit(
                poseStack,
                H_BOOK_WIDTH - 10,
                -H_BOOK_HEIGHT,
                OVERLAY_UR,
                0,
                5,
                BOOK_HEIGHT,
                TEXTURE_W,
                256
        );
        blit(
                poseStack,
                -H_BOOK_WIDTH + 5,
                -H_BOOK_HEIGHT,
                OVERLAY_UL,
                0,
                5,
                BOOK_HEIGHT,
                TEXTURE_W,
                256
        );
        poseStack.popPose();

        //render widgets
        poseStack.pushPose();
        RenderSystem.enableDepthTest();

        poseStack.translate(-width / 2f, -height / 2f, 0.2);
        var v = transformMousePos(mouseX, mouseY);
        super.render(poseStack, (int) v.x(), (int) v.y(), delta);
        poseStack.popPose();

        poseStack.popPose();
        poseStack.pushPose();
        RenderSystem.enableDepthTest();
        poseStack.translate(0, 0, -20);
        renderBackground(poseStack);
        poseStack.popPose();

        if (MapAtlasesClientConfig.worldMapCrossair.get()) {
            poseStack.pushPose();
            poseStack.translate(0, 0, 5);
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            RenderSystem.setShaderTexture(0, GUI_ICONS);

            blit(poseStack, (width - 15) / 2, (height - 15) / 2,
                    0, 0, 15, 15);
            RenderSystem.defaultBlendFunc();

            poseStack.popPose();
        }
    }

    // ================== Mouse Functions ==================

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
        if (selectedSlice.dimension().equals(level.dimension())) {
            return getMapClosestToPlayer().data;
        } else {
            IMapCollection maps = MapAtlasItem.getMaps2(atlas, level);
            MapItemSavedData best = null;
            float averageX = 0;
            float averageZ = 0;
            int count = 0;
            for (MapDataHolder holder : maps.selectSection(selectedSlice)) {
                MapItemSavedData d = holder.data;
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
            MapDataHolder closest = maps.getClosest(averageX, averageZ, selectedSlice);
            if (closest == null) {
                int error = 1;
            }
            return closest == null ? null : closest.data;
            //centers to any map that has decoration
        }
    }

    @Nullable
    protected MapDataHolder findMapEntryForCenter(int reqXCenter, int reqZCenter) {
        return MapAtlasItem.getMaps2(atlas, level).select(reqXCenter, reqZCenter, selectedSlice);
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
        if (changedDim) this.selectedSlice = Slice.of(selectedSlice.type(), selectedSlice.height(), dimension);
        //we dont change slice when calling this from init as we want to use the atlas initial slice
        updateSlice(!initialized ? selectedSlice : MapAtlasItem.getSelectedSlice2(atlas, dimension));
        boolean isWherePlayerIs = level.dimension().equals(dimension);

        MapItemSavedData center = isWherePlayerIs ? getMapClosestToPlayer().data : this.getCenterMapForSelectedDim();
        if (center == null) {
            int error = 0;
            return;
        }
        this.mapWidget.resetAndCenter(center.x, center.z, isWherePlayerIs, changedDim);
        for (var v : dimensionBookmarks) {
            v.setSelected(v.getDimension().equals(dimension));
        }
        recalculateDecorationWidgets();
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
                d.y = ((height - BOOK_HEIGHT) / 2 + 15 + index * separation);
                d.setIndex(index);
                index++;
            }
        }
    }

    private void addDecorationWidgets() {
        if (!this.selectedSlice.hasMarkers()) return;
        List<DecorationHolder> mapIcons = new ArrayList<>();

        boolean ml = MapAtlasesMod.MOONLIGHT;
        for (MapDataHolder holder : MapAtlasItem.getMaps2(atlas, level).selectSection(selectedSlice)) {
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
        IMapCollection maps = MapAtlasItem.getMaps2(atlas, level);
        int current = selectedSlice.heightOrTop();
        MapType type = selectedSlice.type();
        ResourceKey<Level> dim = selectedSlice.dimension();
        Integer newHeight = maps.getHeightTree(dim, type).floor(current - 1);
        return updateSlice(Slice.of(type, newHeight, dim));
    }

    //TODO: make static
    public boolean increaseSlice() {
        IMapCollection maps = MapAtlasItem.getMaps2(atlas, level);
        int current = selectedSlice.heightOrTop();
        MapType type = selectedSlice.type();
        ResourceKey<Level> dim = selectedSlice.dimension();
        Integer newHeight = maps.getHeightTree(dim, type).ceiling(current + 1);
        return updateSlice(Slice.of(type, newHeight, dim));
    }

    public void cycleSliceType() {
        IMapCollection maps = MapAtlasItem.getMaps2(atlas, level);
        ResourceKey<Level> dim = selectedSlice.dimension();
        var slices = new ArrayList<>(maps.getAvailableTypes(dim));
        if (!slices.isEmpty()) {
            int index = slices.indexOf(selectedSlice.type());
            index = (index + 1) % slices.size();
            MapType type = slices.get(index);
            TreeSet<Integer> heightTree = maps.getHeightTree(dim, type);
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
            MapAtlasesNetworking.CHANNEL.sendToServer(new C2SSelectSlicePacket(selectedSlice,
                    lectern == null ? null : lectern.getBlockPos()));
            //update the client immediately
            MapAtlasItem.setSelectedSlice(atlas, selectedSlice);
            recalculateDecorationWidgets();
            changed = true;
        }
        //update button regardless
        IMapCollection maps = MapAtlasItem.getMaps2(atlas, level);
        var dim = selectedSlice.dimension();
        boolean manySlices = maps.getHeightTree(dim, selectedSlice.type()).size() > 1;
        boolean manyTypes = maps.getAvailableTypes(dim).size() != 1;
        sliceButton.refreshState(manySlices, manyTypes);
        sliceDown.setActive(manySlices);
        sliceUp.setActive(manySlices);
        mapWidget.resetZoom();
        return changed;
    }

    public boolean canTeleport() {
        return hasShiftDown() && minecraft.gameMode.getPlayerMode().isCreative();
    }


    public static Vector4f scaleVector(double mouseX, double mouseZ, float scale, int w, int h) {
        Matrix4f matrix4f = new Matrix4f();
        matrix4f.setIdentity();
        // Calculate the translation and scaling factors
        float translateX = w / 2.0F;
        float translateY = h / 2.0F;
        // Apply translation to the matrix (combined)
        matrix4f.multiplyWithTranslation(translateX, translateY, 0);

        // Apply scaling to the matrix
        matrix4f.multiply(Matrix4f.createScaleMatrix(scale, scale, scale));

        // Apply translation back to the original position (combined)
        matrix4f.multiplyWithTranslation(-translateX, -translateY, 0);

        // Create a vector with the input coordinates
        Vector4f v = new Vector4f((float) mouseX, (float) mouseZ, 0, 1.0F);

        // Apply the transformation matrix to the vector
        v.transform(matrix4f);
        return v;
    }

    public Minecraft getMinecraft() {
        return minecraft;
    }
}