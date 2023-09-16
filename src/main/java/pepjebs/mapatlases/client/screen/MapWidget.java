package pepjebs.mapatlases.client.screen;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.client.AbstractAtlasWidget;
import pepjebs.mapatlases.client.ui.MapAtlasesHUD;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;

public class MapWidget extends AbstractAtlasWidget implements Renderable, GuiEventListener, NarratableEntry {


    private static final int PAN_BUCKET = 25;
    private static final int ZOOM_BUCKET = 2;

    private final AtlasOverviewScreen mapScreen;

    protected final int x;
    protected final int y;
    protected final int width;
    protected final int height;

    private float cumulativeZoomValue = ZOOM_BUCKET;
    private float cumulativeMouseX = 0;
    private float cumulativeMouseY = 0;

    private float targetZoomLevel = 2;
    private float zoomLevel = 2;
    protected int targetXCenter;
    protected int targetZCenter;

    private boolean isHovered;
    private float animationProgress = 0; //from zero to 1

    public MapWidget(int x, int y, int width, int height, int atlasesCount,
                     AtlasOverviewScreen hack, MapItemSavedData originalCenterMap) {
        super(atlasesCount);
        initialize(originalCenterMap);

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.mapScreen = hack;

        this.targetXCenter = originalCenterMap.centerX;
        this.targetZCenter = originalCenterMap.centerZ;
    }

    @Override
    public void render(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        this.isHovered = isMouseOver(pMouseX, pMouseY);

        this.drawAtlas(graphics, x, y, width, height, player, zoomLevel,
                MapAtlasesClientConfig.worldMapBorder.get());

        if (this.isHovered) {
            this.renderPositionText(graphics, mc.font,  pMouseX, pMouseY, zoomLevel);
        }

        mapScreen.updateVisibleDecoration((int) currentXCenter, (int) currentZCenter, zoomLevel / 2f * MAP_DIMENSION, followingPlayer);
    }

    @Override
    public Pair<String, MapItemSavedData> getMapWithCenter(int centerX, int centerZ) {
        return mapScreen.findMapEntryForCenter(centerX, centerZ);
    }

    private void renderPositionText(GuiGraphics graphics, Font font, int mouseX, int mouseY, float zoomLevelDim) {
        // Draw world map coords

        if (!MapAtlasesClientConfig.drawWorldMapCoords.get()) return;
        double atlasMapsRelativeMouseX = Mth.map(
                mouseX, x, x + width, -1.0, 1.0);
        double atlasMapsRelativeMouseZ = Mth.map(
                mouseY, y, y + height, -1.0, 1.0);
        BlockPos pos = new BlockPos(
                (int) (Math.floor(atlasMapsRelativeMouseX * zoomLevelDim * (mapAtlasScale / 2.0)) + currentXCenter),
                0,
                (int) (Math.floor(atlasMapsRelativeMouseZ * zoomLevelDim * (mapAtlasScale / 2.0)) + currentZCenter));
        float textScaling = (float) (double) MapAtlasesClientConfig.worldMapCoordsScale.get();
        int hackOffset = +3;
        //TODO: fix coordinate being slightly offset
        //idk why
        String coordsToDisplay = "X: " + (pos.getX() + hackOffset) + ", Z: " + (pos.getZ() + hackOffset);
        MapAtlasesHUD.drawScaledComponent(
                graphics, font, x, y+ height +8, coordsToDisplay, textScaling, width);


    }


    @Override
    public boolean isMouseOver(double pMouseX, double pMouseY) {
        return pMouseX >= this.x && pMouseY >= this.y && pMouseX < (this.x + this.width) && pMouseY < (this.y + this.height);
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double deltaX, double deltaY) {
        if (pButton == 0) {
            float hack = (zoomLevel / atlasesCount);
            //TODO: fix pan
            cumulativeMouseX += deltaX * hack;
            cumulativeMouseY += deltaY * hack;
            int newXCenter;
            int newZCenter;
            boolean discrete = false;
            if (discrete) {
                //discrete mode
                newXCenter = (int) (currentXCenter - (round((int) cumulativeMouseX, PAN_BUCKET) / PAN_BUCKET * mapAtlasScale));
                newZCenter = (int) (currentZCenter - (round((int) cumulativeMouseY, PAN_BUCKET) / PAN_BUCKET * mapAtlasScale));
            } else {
                newXCenter = (int) (currentXCenter - cumulativeMouseX * mapAtlasScale / PAN_BUCKET);
                newZCenter = (int) (currentZCenter - cumulativeMouseY * mapAtlasScale / PAN_BUCKET);
            }
            if (newXCenter != currentXCenter || newZCenter != currentZCenter) {
                if (!discrete) {
                    currentXCenter = newXCenter;
                    currentZCenter = newZCenter;
                }
                targetXCenter = newXCenter;
                targetZCenter = newZCenter;
                cumulativeMouseX = 0;
                cumulativeMouseY = 0;
            }
            followingPlayer = false;
            return true;
        }
        return GuiEventListener.super.mouseDragged(pMouseX, pMouseY, pButton, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        cumulativeZoomValue -= pDelta;
        cumulativeZoomValue = Math.max(cumulativeZoomValue, -1 * ZOOM_BUCKET);

        int zl = round((int) cumulativeZoomValue, ZOOM_BUCKET) / ZOOM_BUCKET;
        zl = Math.max(zl, 0);
        targetZoomLevel = (2 * zl) + 1f;
        return true;
    }


    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        return isHovered;
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

    public void resetAndCenter(int centerX, int centerZ, boolean followPlayer) {
        this.targetXCenter = centerX;
        this.targetZCenter = centerZ;
        // Reset offset & zoom
        this.cumulativeMouseX = 0;
        this.cumulativeMouseY = 0;
        this.cumulativeZoomValue = ZOOM_BUCKET;
        this.targetZoomLevel = 2;
        this.followingPlayer = followPlayer;
    }

    public int getMapAtlasScale() {
        return mapAtlasScale;
    }

    public void tick() {
        float animationSpeed = 0.4f;
        if (animationProgress != 0) {
            animationProgress -= animationProgress * animationSpeed - 0.01;
            animationProgress = Math.max(0, animationProgress);
        }
        if (this.zoomLevel != targetZoomLevel) {
            zoomLevel = interpolate(targetZoomLevel, zoomLevel, animationSpeed);
        }
        boolean test = true;
        if (this.currentXCenter != targetXCenter) {
            currentXCenter = interpolate(targetXCenter, currentXCenter, animationSpeed);
            test = false;
        }
        if (this.currentZCenter != targetZCenter) {
            currentZCenter = interpolate(targetZCenter, currentZCenter, animationSpeed);
            test = false;
        }

        //TODO:: better player snap
        //follow player
        if (followingPlayer) {

            var player = Minecraft.getInstance().player;
            targetXCenter = (int) player.getX();
            targetZCenter = (int) player.getZ();
        }
    }

    private float interpolate(float targetZCenter, float currentZCenter, float animationSpeed) {
        float diff = targetZCenter - currentZCenter;
        if (diff < 0) {
            return Math.max(targetZCenter, currentZCenter + (diff * animationSpeed) - 0.001f);
        } else {
            return Math.min(targetZCenter, currentZCenter + (diff * animationSpeed) + 0.001f);
        }
    }
}
