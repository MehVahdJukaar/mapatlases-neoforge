package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.client.ui.MapAtlasesHUD;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static pepjebs.mapatlases.client.screen.MapAtlasesAtlasOverviewScreen.ZOOM_BUCKET;

public class MapWidget implements Renderable, GuiEventListener, NarratableEntry {


    private static final int MAP_DIMENSION = 128;
    private static final int PAN_BUCKET = 25;

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    //internally controls how many maps are displayed
    private final int atlases;
    private final int atlasScale;
    private final int mapSize;
    private final MapAtlasesAtlasOverviewScreen hack;
    private final MapItemSavedData originalCenterMap;

    private int zoomValue = ZOOM_BUCKET;
    private double rawMouseXMoved = 0;
    private double rawMouseYMoved = 0;
    private int mouseXOffset = 0;
    private int mouseYOffset = 0;
    private int currentXCenter;
    private int currentZCenter;
    private boolean isHovered;

    public MapWidget(int x, int y, int width, int height, int atlasesCount, int atlasScale,
                     MapAtlasesAtlasOverviewScreen hack) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.atlases = atlasesCount;
        this.atlasScale = atlasScale;
        this.mapSize = width / atlasesCount;
        this.hack = hack;

        originalCenterMap = hack.getClosestMapToPlayer();

         currentXCenter = originalCenterMap.centerX;
         currentZCenter = originalCenterMap.centerZ;
    }

    @Override
    public void render(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        isHovered = isHovered(pMouseX, pMouseY);

        int zoomLevelDim = getZoomLevelDim();
        // Handle zooming markers hack
        MapAtlasesClient.setWorldMapZoomLevel(zoomLevelDim * (float) (double) MapAtlasesClientConfig.worldMapDecorationScale.get());


        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(x, y, 0);
        float scale = mapSize / (float) MAP_DIMENSION;
        poseStack.scale(scale, scale, -1);

        // Draw maps, putting active map in middle of grid

        MultiBufferSource.BufferSource vcp = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        //graphics.enableScissor(mapComponentX, mapComponentY,
        //         (mapComponentX + MAP_SIZE), (mapComponentY + MAP_SIZE));


        //follow player
        //   poseStack.translate(
        //            (centerMap.centerX - player.getX()),
        //            (centerMap.centerZ - player.getZ()),0);

        for (int i = zoomLevelDim - 1; i >= 0; i--) {
            for (int j = zoomLevelDim - 1; j >= 0; j--) {
                int iXIdx = i - (zoomLevelDim / 2);
                int jYIdx = j - (zoomLevelDim / 2);
                int reqXCenter = currentXCenter + (jYIdx * atlasScale);
                int reqZCenter = currentZCenter + (iXIdx * atlasScale);
                Pair<String, MapItemSavedData> state = hack.findMapEntryForCenter(reqXCenter, reqZCenter);
                if (state == null) {
                    continue;
                }
                MapItemSavedData data = state.getSecond();
                boolean drawPlayerIcons = data.dimension.equals(player.level().dimension());
                drawPlayerIcons = drawPlayerIcons && originalCenterMap == state.getSecond();
                drawMap(poseStack, vcp, i, j, state, drawPlayerIcons);
            }
        }

        vcp.endBatch();
        poseStack.popPose();
        //  graphics.disableScissor();
        if(this.isHovered) {
            this.renderText(graphics, pMouseX, pMouseY, zoomLevelDim);
        }
    }

    private void renderText(GuiGraphics graphics, int mouseX, int mouseY, int zoomLevelDim){
        // Draw world map coords

        if (!MapAtlasesClientConfig.drawWorldMapCoords.get()) return;
        double atlasMapsRelativeMouseX = Mth.map(
                mouseX, x, x + width, -1.0, 1.0);
        double atlasMapsRelativeMouseZ = Mth.map(
                mouseY, y , y + height, -1.0, 1.0);
        BlockPos pos = new BlockPos(
                (int) (Math.floor(atlasMapsRelativeMouseX * zoomLevelDim * (atlasScale / 2.0)) + currentXCenter),
                0,
                (int) (Math.floor(atlasMapsRelativeMouseZ * zoomLevelDim * (atlasScale / 2.0)) + currentZCenter));
        float textScaling = (float) (double) MapAtlasesClientConfig.worldMapCoordsScale.get();
        String coordsToDisplay = "X: " + pos.getX() + ", Z: " + pos.getZ() ;
        MapAtlasesHUD.drawScaledComponent(
                graphics, x, y, coordsToDisplay, textScaling, width, height+16);


    }


    private void drawMap(
            PoseStack matrices,
            MultiBufferSource.BufferSource vcp,
            int ix, int iy,
            Pair<String, MapItemSavedData> state,
            boolean drawPlayerIcons
    ) {
        // Draw the map
        double curMapComponentX = (MAP_DIMENSION * iy);
        double curMapComponentY = (MAP_DIMENSION * ix);
        matrices.pushPose();
        matrices.translate(curMapComponentX, curMapComponentY, 0.0);

        // Remove the off-map player icons temporarily during render
        MapItemSavedData data = state.getSecond();
        Iterator<Map.Entry<String, MapDecoration>> it = data.decorations.entrySet().iterator();
        List<Map.Entry<String, MapDecoration>> removed = new ArrayList<>();
        // Only remove the off-map icon if it's not the active map, or it's not the active dimension
        while (it.hasNext()) {
            Map.Entry<String, MapDecoration> e = it.next();
            var type = e.getValue().getType();
            if (type == MapDecoration.Type.PLAYER_OFF_MAP || type == MapDecoration.Type.PLAYER_OFF_LIMITS
                    || (type == MapDecoration.Type.PLAYER && !drawPlayerIcons)) {
                it.remove();
                removed.add(e);
            }
        }

        Minecraft.getInstance().gameRenderer.getMapRenderer()
                .render(
                        matrices,
                        vcp,
                        MapAtlasesAccessUtils.getMapIntFromString(state.getFirst()),
                        data,
                        false,
                        0xF000F0
                );
        matrices.popPose();
        // Re-add the off-map player icons after render
        for (Map.Entry<String, MapDecoration> e : removed) {
            data.decorations.put(e.getKey(), e.getValue());
        }
    }


    private int getZoomLevelDim() {
        int zoomLevel = round(zoomValue, ZOOM_BUCKET) / ZOOM_BUCKET;
        zoomLevel = Math.max(zoomLevel, 0);
        return (2 * zoomLevel) + 1;
    }

    public static int round(int num, int mod) {
        //return Math.round((float) num / mod) * mod
        int t = num % mod;
        if (t < (int) Math.floor(mod / 2.0))
            return num - t;
        else
            return num + mod - t;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        rawMouseXMoved = mouseX;
        rawMouseYMoved = mouseY;
    }

    protected boolean isHovered(double pMouseX, double pMouseY) {
        return pMouseX >= this.x && pMouseY >= this.y && pMouseX < (this.x + this.width) && pMouseY < (this.y + this.height);
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double deltaX, double deltaY) {
        if (pButton == 0 ) {
            mouseXOffset += deltaX;
            mouseYOffset += deltaY;
            int targetXCenter = currentXCenter + (round(mouseXOffset, PAN_BUCKET) / PAN_BUCKET * atlasScale * -1);
            int targetZCenter = currentZCenter + (round(mouseYOffset, PAN_BUCKET) / PAN_BUCKET * atlasScale * -1);
            if (targetXCenter != currentXCenter || targetZCenter != currentZCenter) {
                currentXCenter = targetXCenter;
                currentZCenter = targetZCenter;
                mouseXOffset = 0;
                mouseYOffset = 0;
            }
            return true;
        }
        return GuiEventListener.super.mouseDragged(pMouseX, pMouseY, pButton, deltaX, deltaY);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        return  isHovered;
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
}
