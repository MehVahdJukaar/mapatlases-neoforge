package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.client.AbstractAtlasDisplay;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.client.ui.MapAtlasesHUD;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.networking.C2STeleportPacket;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.Slice;

public class MapWidget extends AbstractAtlasDisplay implements Renderable, GuiEventListener, NarratableEntry {

    private static final int PAN_BUCKET = 25;
    private static final int ZOOM_BUCKET = 2;
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 20;

    private final AtlasOverviewScreen mapScreen;

    private final int x;
    private final int y;
    private final int width;
    private final int height;

    private float cumulativeZoomValue;
    private float cumulativeMouseX = 0;
    private float cumulativeMouseY = 0;

    private double targetXCenter;
    private double targetZCenter;
    private float targetZoomLevel;

    private boolean isHovered;
    private float scaleAlpha = 0;

    public MapWidget(int x, int y, int width, int height, int atlasesCount,
                     AtlasOverviewScreen screen, MapDataHolder originalCenterMap) {
        super(atlasesCount);
        initialize(originalCenterMap);
        this.targetZoomLevel = zoomLevel;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.mapScreen = screen;
        this.drawBigPlayerMarker = false;

        var player = Minecraft.getInstance().player;
        this.currentXCenter = player.getX();
        this.currentZCenter = player.getZ();
    }

    @Override
    protected void applyScissors(GuiGraphics graphics, int x, int y, int x1, int y1) {
        var v = mapScreen.transformPos(x, y);
        var v2 = mapScreen.transformPos(x1, y1);
        super.applyScissors(graphics, (int) v.x, (int) v.y, (int) v2.x, (int) v2.y);
    }

    @Override
    public void render(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        this.isHovered = isMouseOver(pMouseX, pMouseY);

        // Handle zooming markers hack
        MapAtlasesClient.setDecorationsScale(zoomLevel * (float) (double) MapAtlasesClientConfig.worldMapDecorationScale.get());
        MapAtlasesClient.setDecorationsTextScale(zoomLevel * (float) (double) MapAtlasesClientConfig.worldMapDecorationTextScale.get());

        MapItemSavedData hoveredData = null;
        boolean shearing = mapScreen.isShearing();
        if (shearing || mapScreen.isPlacingPin()) {
            MapDataHolder d = getHoveredMap(pMouseX, pMouseY);
            hoveredData = d != null ? d.data : null;
        }
        this.drawAtlas(graphics, x, y, width, height, zoomLevel, mapScreen.getSelectedSlice().type(),
                LightTexture.FULL_BRIGHT, shearing ? hoveredData : null);

        MapAtlasesClient.setDecorationsScale(1);
        MapAtlasesClient.setDecorationsTextScale(1);

        mapScreen.updateVisibleDecoration((int) currentXCenter, (int) currentZCenter,
                (zoomLevel / 2) * mapBlocksSize);

        if (isHovered && hoveredData != null) {
            mapScreen.notifyOfClickActionUsage();
        }

        if (this.isHovered && !mapScreen.isEditingText()) {
            this.renderPositionText(graphics, mc.font, pMouseX, pMouseY);

            if (mapScreen.canTeleport()) {
                graphics.renderTooltip(mc.font,
                        Component.translatable("chat.coordinates.tooltip")
                                .withStyle(ChatFormatting.GREEN),
                        pMouseX, pMouseY);
            }
            if (PlatHelper.isDev()) {
                var d = getHoveredMap(pMouseX, pMouseY);
                if (d != null) {
                    AtlasScreenUtils.drawScaledComponent(
                            graphics, mc.font, x, y + height + 8 + 10, "Map: [id=" + d.id.id() + ", type=" + d.type + ", y=" + d.height + "]", 1, width, width);
                }
            }
        }
        renderScaleText(graphics, mc);
    }

    private void renderScaleText(GuiGraphics graphics, Minecraft mc) {
        boolean zooming = zoomLevel != targetZoomLevel;
        if (zooming) scaleAlpha = 1;
        else scaleAlpha = Math.max(0, scaleAlpha - 0.03f);
        int a = (int) (scaleAlpha * 255);
        if (a <= 10) return;
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 4);
        graphics.drawString(mc.font,
                Component.translatable("message.map_atlases.map_scale", String.format("%.1f", targetZoomLevel)),
                x, y + height - 8, FastColor.ABGR32.color(a, 255, 255, 255));
        poseStack.popPose();
    }

    @Override
    protected boolean showMapBackground() {
        return MapAtlasesClientConfig.showsMapBackground.get();
    }

    @Override
    protected boolean showMapBorders() {
        return MapAtlasesClientConfig.worldMapBorder.get();
    }

    @Override
    public MapDataHolder getMapWithCenter(int centerX, int centerZ) {
        return mapScreen.findMapWithCenter(centerX, centerZ);
    }

    private void renderPositionText(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (!MapAtlasesClientConfig.drawWorldMapCoords.get()) return;
        ColumnPos pos = getHoveredPos(mouseX, mouseY);
        float textScaling = (float) (double) MapAtlasesClientConfig.worldMapCoordsScale.get();
        String coordsToDisplay = Component.translatable("message.map_atlases.coordinates", pos.x(), pos.z()).getString();
        AtlasScreenUtils.drawScaledComponent(
                graphics, font, x, y + height + 8, coordsToDisplay, textScaling, width, width);
    }


    @Override
    public boolean isMouseOver(double pMouseX, double pMouseY) {
        return pMouseX >= this.x && pMouseY >= this.y && pMouseX < (this.x + this.width) && pMouseY < (this.y + this.height);
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double deltaX, double deltaY) {
        if (pButton == 0) {
            cumulativeMouseX += deltaX;
            cumulativeMouseY += deltaY;
            double newXCenter;
            double newZCenter;
            boolean discrete = !MapAtlasesClientConfig.worldMapSmoothPanning.get();
            if (discrete) {
                newXCenter = (int) (currentXCenter - (round((int) cumulativeMouseX, PAN_BUCKET) / PAN_BUCKET * mapBlocksSize));
                newZCenter = (int) (currentZCenter - (round((int) cumulativeMouseY, PAN_BUCKET) / PAN_BUCKET * mapBlocksSize));
            } else {
                float blocksPerPixel = zoomLevel * mapBlocksSize / (width * mapScreen.globalScale);
                newXCenter = currentXCenter - cumulativeMouseX * blocksPerPixel;
                newZCenter = currentZCenter - cumulativeMouseY * blocksPerPixel;
            }
            if (newXCenter != currentXCenter) {
                targetXCenter = newXCenter;
                if (!discrete) currentXCenter = targetXCenter;
                cumulativeMouseX = 0;
            }
            if (newZCenter != currentZCenter) {
                targetZCenter = newZCenter;
                if (!discrete) currentZCenter = targetZCenter;
                cumulativeMouseY = 0;
            }
            followingPlayer = false;
            return true;
        }
        return GuiEventListener.super.mouseDragged(pMouseX, pMouseY, pButton, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        boolean atZoomLimit = (scrollY < 0 && targetZoomLevel >= MAX_ZOOM) || (scrollY > 0 && targetZoomLevel <= MIN_ZOOM);
        if (atZoomLimit) {
            cumulativeZoomValue = 0;
            return false;
        }

        if (MapAtlasesClientConfig.worldMapSmoothZooming.get()) {
            double v = -scrollY / 25d * MapAtlasesClientConfig.worldMapZoomScrollSpeed.get();
            if (Screen.hasShiftDown() || Screen.hasControlDown()) v *= 3;
            targetZoomLevel = Mth.clamp(targetZoomLevel + targetZoomLevel * (float) v, MIN_ZOOM, MAX_ZOOM);
            zoomLevel = targetZoomLevel - 0.001f;
        } else {
            cumulativeZoomValue = Math.max(cumulativeZoomValue - (float) scrollY, 0);
            int zoomSteps = round((int) cumulativeZoomValue, ZOOM_BUCKET) / ZOOM_BUCKET;
            targetZoomLevel = Mth.clamp(2 + 2 * zoomSteps, MIN_ZOOM, MAX_ZOOM);
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int pButton) {
        if (!isHovered) return false;
        ColumnPos pos = getHoveredPos(mouseX, mouseY);
        if (mapScreen.isPlacingPin()) {
            mapScreen.placePinAt(pos);
            playClickSound(SoundEvents.ITEM_FRAME_ADD_ITEM);
        } else if (mapScreen.isShearing()) {
            mapScreen.shearMapAt(pos);
            playClickSound(SoundEvents.SHEEP_SHEAR);
        } else if (mapScreen.canTeleport()) {
            Slice slice = mapScreen.getSelectedSlice();
            NetworkHelper.sendToServer(new C2STeleportPacket(pos.x(), pos.z(), slice.height(), slice.dimension()));
            if (!PlatHelper.isDev()) mapScreen.onClose();
            return true;
        }
        return !mapScreen.isEditingText();
    }

    private void playClickSound(SoundEvent sound) {
        mapScreen.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.7F, 2f));
    }

    @Nullable
    private MapDataHolder getHoveredMap(double mouseX, double mouseY) {
        ColumnPos pos = getHoveredPos(mouseX, mouseY);
        return mapScreen.findMapContaining(pos.x(), pos.z());
    }

    @NotNull
    private ColumnPos getHoveredPos(double mouseX, double mouseY) {
        double wSize = zoomLevel;
        double hSize = zoomLevel * height / width;
        double atlasMapsRelativeMouseX = Mth.map(
                mouseX, x, x + width, -wSize, wSize);
        double atlasMapsRelativeMouseZ = Mth.map(
                mouseY, y, y + height, -hSize, hSize);
        int hackOffset = +3;
        return new ColumnPos(
                (int) (Math.floor(atlasMapsRelativeMouseX * (mapBlocksSize / 2.0)) + currentXCenter) + hackOffset,
                (int) (Math.floor(atlasMapsRelativeMouseZ * (mapBlocksSize / 2.0)) + currentZCenter) + hackOffset);
    }

    @Override
    public void setFocused(boolean pFocused) {

    }

    @Override
    public boolean isFocused() {
        return true;
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput pNarrationElementOutput) {

    }

    public void resetAndCenter(int centerX, int centerZ, boolean followPlayer, boolean animation) {
        if (followPlayer) {
            centerX = Minecraft.getInstance().player.getBlockX();
            centerZ = Minecraft.getInstance().player.getBlockZ();
        }
        this.targetXCenter = centerX;
        this.targetZCenter = centerZ;
        if (!animation) {
            this.currentXCenter = centerX;
            this.currentZCenter = centerZ;
        }
        // Reset offset & zoom
        this.cumulativeMouseX = 0;
        this.cumulativeMouseY = 0;
        this.cumulativeZoomValue = 0;
        this.followingPlayer = followPlayer;
        resetZoom();
    }

    public void resetZoom() {
        this.targetZoomLevel = atlasesCount * mapScreen.getSelectedSlice().type().getDefaultZoomFactor();
    }

    public void tick() {
        float animationSpeed = 0.4f;
        zoomLevel = (float) interpolate(targetZoomLevel, zoomLevel, animationSpeed);
        currentXCenter = interpolate(targetXCenter, currentXCenter, animationSpeed);
        currentZCenter = interpolate(targetZCenter, currentZCenter, animationSpeed);

        //TODO:: better player snap
        if (followingPlayer) {
            var player = Minecraft.getInstance().player;
            targetXCenter = (int) player.getX();
            targetZCenter = (int) player.getZ();
        }
    }

    private static double interpolate(double target, double current, double animationSpeed) {
        double diff = target - current;
        if (Math.abs(diff) < 0.01) return target;
        return current + (diff * animationSpeed);
    }
}
