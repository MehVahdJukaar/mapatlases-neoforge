package pepjebs.mapatlases.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractAtlasWidget {

    public static final int MAP_DIMENSION = 128;


    //internally controls how many maps are displayed
    protected final int atlasesCount;
    protected final int mapAtlasScale;
    private final MapItemSavedData originalCenterMap;

    protected boolean followingPlayer = true;
    protected float currentXCenter;
    protected float currentZCenter;

    protected AbstractAtlasWidget(int atlasesCount, MapItemSavedData originalCenterMap) {

        this.atlasesCount = atlasesCount;
        this.originalCenterMap = originalCenterMap;
        this.mapAtlasScale = (1 << originalCenterMap.scale) * MAP_DIMENSION;

        this.currentXCenter = originalCenterMap.centerX;
        this.currentZCenter = originalCenterMap.centerZ;
    }

    public void drawAtlas(GuiGraphics graphics, int x, int y, int width, int height, Player player, float zoomLevelDim) {
        // Handle zooming markers hack
        MapAtlasesClient.setWorldMapZoomLevel(zoomLevelDim * (float) (double) MapAtlasesClientConfig.worldMapDecorationScale.get());


        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        float mapScalingFactor = width / (float) (atlasesCount * MAP_DIMENSION);
        float zoomScale = atlasesCount / zoomLevelDim;

        int intXCenter = (int) (currentXCenter);
        int intZCenter = (int) (currentZCenter);

        int centerMapX = round(intXCenter, mapAtlasScale);
        int centerMapZ = round(intZCenter, mapAtlasScale);


        poseStack.translate(x + width / 2f, y + height / 2f, 0);
        poseStack.scale(mapScalingFactor * zoomScale, mapScalingFactor * zoomScale, -1);

        // Draw maps, putting active map in middle of grid

        MultiBufferSource.BufferSource vcp = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        graphics.enableScissor(x, y, (x + width), (y + height));

        float offsetX = currentXCenter - centerMapX;
        float offsetZ = currentZCenter - centerMapZ;


        poseStack.translate(-offsetX, -offsetZ, 0);

        int hz = Mth.ceil(zoomLevelDim / 2f);

        int minI = -hz;
        int maxI = hz;
        int minJ = -hz;
        int maxJ = hz;
        //adds more maps to draw if needed
        if (offsetX < 0) minJ--;
        else if (offsetX > 0) maxJ++;
        if (offsetZ < 0) minI--;
        else if (offsetZ > 0) maxI++;
        for (int i = maxI; i >= minI; i--) {
            for (int j = maxJ; j >= minJ; j--) {
                int reqXCenter = centerMapX + (j * mapAtlasScale);
                int reqZCenter = centerMapZ + (i * mapAtlasScale);
                Pair<String, MapItemSavedData> state = getMapAtCenter(reqXCenter, reqZCenter);
                if (state == null) continue;
                MapItemSavedData data = state.getSecond();
                boolean drawPlayerIcons = data.dimension.equals(player.level().dimension());
                drawPlayerIcons = drawPlayerIcons && originalCenterMap == state.getSecond();
                this.drawMap(poseStack, vcp, i, j, state, drawPlayerIcons);
            }
        }

        vcp.endBatch();
        poseStack.popPose();
        graphics.disableScissor();

        MapAtlasesClient.setWorldMapZoomLevel(1);

    }

    public abstract Pair<String, MapItemSavedData> getMapAtCenter(int centerX, int centerZ);

    public void setFollowingPlayer(boolean followingPlayer) {
        this.followingPlayer = followingPlayer;
    }

    private void drawMap(
            PoseStack matrices,
            MultiBufferSource.BufferSource vcp,
            int ix, int iy,
            Pair<String, MapItemSavedData> state,
            boolean drawPlayerIcons
    ) {
        // Draw the map
        double curMapComponentX = (MAP_DIMENSION * iy) - MAP_DIMENSION / 2f;
        double curMapComponentY = (MAP_DIMENSION * ix) - MAP_DIMENSION / 2f;
        matrices.pushPose();
        matrices.translate(curMapComponentX, curMapComponentY, 0.0);

        // Remove the off-map player icons temporarily during render
        MapItemSavedData data = state.getSecond();
        List<Map.Entry<String, MapDecoration>> removed = new ArrayList<>();
        // Only remove the off-map icon if it's not the active map, or it's not the active dimension
        for (var e : data.decorations.entrySet()) {
            MapDecoration decoration = e.getValue();
            MapDecoration.Type type = decoration.getType();
            if (type == MapDecoration.Type.PLAYER_OFF_MAP || type == MapDecoration.Type.PLAYER_OFF_LIMITS
                    || (type == MapDecoration.Type.PLAYER && !drawPlayerIcons)) {
                removed.add(e);
            }
        }

        removed.forEach(d -> data.decorations.remove(d.getKey()));
        if (data.decorations.size() != 0) {
            int aa = 1;
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


    public static int round(int num, int mod) {
        //return Math.round((float) num / mod) * mod
        int t = num % mod;
        if (t < (int) Math.floor(mod / 2.0))
            return num - t;
        else
            return num + mod - t;
    }
}
