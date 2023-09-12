package pepjebs.mapatlases.client.screen;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
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

    private final MapAtlasesAtlasOverviewScreen mapScreen;

    protected final int x;
    protected final int y;
    protected final int width;
    protected final int height;

    private float cumulativeZoomValue = ZOOM_BUCKET;
    private float cumulativeMouseX = 0;
    private float cumulativeMouseY = 0;

    private float targetZoomLevel = 3;
    private float zoomLevel = 3;

    private boolean isHovered;
    private float animationProgress = 0; //from zero to 1

    public MapWidget(int x, int y, int width, int height, int atlasesCount,
                     MapAtlasesAtlasOverviewScreen hack, MapItemSavedData originalCenterMap) {
        super(atlasesCount, originalCenterMap);

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.mapScreen = hack;

        currentXCenter = originalCenterMap.centerX;
        currentZCenter = originalCenterMap.centerZ;
    }

    @Override
    public void render(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        isHovered = isMouseOver(pMouseX, pMouseY);


        this.drawAtlas(graphics, x, y, width, height, player, zoomLevel);


        if (this.isHovered) {
            this.renderPositionText(graphics, pMouseX, pMouseY, zoomLevel);
        }

        mapScreen.updateVisibleDecoration(currentXCenter, currentZCenter, zoomLevel / 2f * MAP_DIMENSION, followingPlayer);
    }

    @Override
    public Pair<String, MapItemSavedData> getMapAtCenter(int centerX, int centerZ) {
        return mapScreen.findMapEntryForCenter(centerX, centerZ);
    }

    private void renderPositionText(GuiGraphics graphics, int mouseX, int mouseY, float zoomLevelDim) {
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
                graphics, x, y, coordsToDisplay, textScaling, width, height + 16);


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
            int targetXCenter;
            int targetZCenter;
            if (false) {
                //discrete mode
                targetXCenter = currentXCenter - (round((int) cumulativeMouseX, PAN_BUCKET) / PAN_BUCKET * mapAtlasScale);
                targetZCenter = currentZCenter - (round((int) cumulativeMouseY, PAN_BUCKET) / PAN_BUCKET * mapAtlasScale);
            } else {
                targetXCenter = (int) (currentXCenter - cumulativeMouseX * mapAtlasScale / PAN_BUCKET);
                targetZCenter = (int) (currentZCenter - cumulativeMouseY * mapAtlasScale / PAN_BUCKET);
            }
            if (targetXCenter != currentXCenter || targetZCenter != currentZCenter) {
                currentXCenter = targetXCenter;
                currentZCenter = targetZCenter;
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
        currentXCenter = centerX;
        currentZCenter = centerZ;
        // Reset offset & zoom
        cumulativeMouseX = 0;
        cumulativeMouseY = 0;
        cumulativeZoomValue = ZOOM_BUCKET;
        this.followingPlayer = followPlayer;
    }

    public int getMapAtlasScale() {
        return mapAtlasScale;
    }

    public void tick() {
        if (animationProgress != 0) {
            animationProgress -= animationProgress * 0.5 - 0.01;
            animationProgress = Math.max(0, animationProgress);
        }
        if (this.zoomLevel != targetZoomLevel) {
            float diff = targetZoomLevel - zoomLevel;
            if (diff < 0) {
                zoomLevel = (float) Math.max(targetZoomLevel, zoomLevel + diff * 0.2 - 0.001);
            } else {
                zoomLevel = (float) Math.min(targetZoomLevel, zoomLevel + diff * 0.2 + 0.001);
            }
        }
    }
}
