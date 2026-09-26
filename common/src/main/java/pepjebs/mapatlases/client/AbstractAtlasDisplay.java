package pepjebs.mapatlases.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import pepjebs.mapatlases.utils.AtlasMap;
import pepjebs.mapatlases.utils.MapType;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public abstract class AbstractAtlasDisplay {

    public static final int MAP_DIMENSION = 128;

    //internally controls how many maps are displayed
    protected final int mapsPerSide;
    protected int mapBlocksSize;
    protected AtlasMap mapWherePlayerIs;

    protected boolean followingPlayer = true;
    protected double currentXCenter;
    protected double currentZCenter;
    protected float zoomLevel = 3;

    protected boolean rotatesWithPlayer = false;
    protected boolean drawBigPlayerMarker = true;

    protected AbstractAtlasDisplay(int mapsPerSide) {
        this.mapsPerSide = mapsPerSide;
    }

    protected void initialize(AtlasMap newCenter) {
        if (mapWherePlayerIs == null || !mapWherePlayerIs.slice.isSameGroup(newCenter.slice)) {
            this.zoomLevel = mapsPerSide * newCenter.type.getDefaultZoomFactor();
        }
        this.mapWherePlayerIs = newCenter;
        this.mapBlocksSize = (1 << mapWherePlayerIs.data.scale) * MAP_DIMENSION;

        this.currentXCenter = mapWherePlayerIs.data.centerX;
        this.currentZCenter = mapWherePlayerIs.data.centerZ;
    }

    public void drawAtlas(GuiGraphics graphics, int x, int y, int width, int height,
                          float zoomLevelDim, MapType type, int light, @Nullable MapItemSavedData selectedData) {

        MapAtlasesClient.setIsDrawingAtlas(true);
        Player player = Minecraft.getInstance().player;

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        float widgetScale = width / (float) (mapsPerSide * MAP_DIMENSION);
        float zoomScale = mapsPerSide / zoomLevelDim;

        int intXCenter = (int) (currentXCenter);
        int intZCenter = (int) (currentZCenter);
        int scaleIndex = mapBlocksSize / MAP_DIMENSION;

        ColumnPos c = type.getCenter(intXCenter, intZCenter, mapBlocksSize);
        int centerMapX = c.x();
        int centerMapZ = c.z();

        //translate to center
        poseStack.translate(x + width / 2f, y + height / 2f, 0);
        //widget scale + zoom

        poseStack.scale(widgetScale * zoomScale, widgetScale * zoomScale, -1);

        // Draw maps, putting active map in middle of grid

        MultiBufferSource.BufferSource vcp = graphics.bufferSource();

        List<Matrix4f> drawnMaps = new ArrayList<>();
        List<Matrix4f> hoveredMaps = new ArrayList<>();

        applyScissors(graphics, x, y, (x + width), (y + height));

        double mapCenterOffsetX = currentXCenter - centerMapX;
        double mapCenterOffsetZ = currentZCenter - centerMapZ;

        //zoom leve is essentially maps on screen
        //dont ask me why all this stuff is like that

        if (rotatesWithPlayer) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(180 - player.getYRot()));
        }
        poseStack.translate(-mapCenterOffsetX / scaleIndex, -mapCenterOffsetZ / scaleIndex, 0);

        //grid side len
        double sideLength = mapBlocksSize * zoomScale;
        //radius of widget
        int radius = (int) (mapBlocksSize * mapsPerSide * 0.71f); // radius using hyp

        // Calculate the distance from the circle's center to the center of each grid square
        int o = Mth.ceil(zoomLevelDim);
        double maxDist = rotatesWithPlayer ?
                Mth.square(radius + (sideLength * 0.71)) :
                (o + 1) * sideLength * 0.5;

        for (int i = o; i >= -o; i--) {
            for (int j = o; j >= -o; j--) {
                double gridCenterI = i * sideLength;
                double gridCenterJ = j * sideLength;

                boolean shouldDraw;
                // Calculate the distance between the grid square center and the circle's center
                if (rotatesWithPlayer) {
                    double distance = Mth.lengthSquared(
                            gridCenterI - mapCenterOffsetZ * zoomScale,
                            gridCenterJ - mapCenterOffsetX * zoomScale);
                    //circle dist
                    shouldDraw = (distance <= maxDist);
                } else {
                    //square dist
                    shouldDraw = Math.abs(gridCenterI - mapCenterOffsetZ * zoomScale) < maxDist &&
                            Math.abs(gridCenterJ - mapCenterOffsetX * zoomScale) < maxDist;
                }
                if (shouldDraw) {
                    AtlasMap state = getMapWithCenter(centerMapX + (j * mapBlocksSize), centerMapZ + (i * mapBlocksSize));
                    if (state != null) {
                        Matrix4f pose = drawMap(player, poseStack, vcp, i, j, state, light);
                        (state.data == selectedData ? hoveredMaps : drawnMaps).add(pose);
                    }
                }
            }
        }
        vcp.endBatch();

        if (showMapBorders()) {
            VertexConsumer outlineVC = MapAtlasesClient.MAP_BORDER_TEXTURE.buffer(vcp, RenderType::text); //its already on block atlas
            //using this so we use mipmap. cant use blit sprite
            for (var matrix4f : drawnMaps) {
                drawOutline(matrix4f, outlineVC);
            }
            if (showMapBackground()) {
                VertexConsumer backVC = MapAtlasesClient.MAP_BACKGROUND_TEXTURE.buffer(vcp, RenderType::text); //its already on block atlas
                //using this so we use mipmap. cant use blit sprite
                for (var matrix4f : drawnMaps) {
                    drawOutline(matrix4f.translate(0, 0, 1), backVC);
                }
            }
            VertexConsumer outlineVC2 = MapAtlasesClient.MAP_HOVERED_TEXTURE.buffer(vcp, RenderType::text); //its already on block atlas
            for (var matrix4f : hoveredMaps) {
                drawOutline(matrix4f, outlineVC2);
            }
            vcp.endBatch();
        }

        poseStack.popPose();
        graphics.disableScissor();

        MapAtlasesClient.setIsDrawingAtlas(false);
    }

    protected abstract boolean showMapBackground();

    protected abstract boolean showMapBorders();

    private static void drawOutline(Matrix4f matrix4f, VertexConsumer outlineVC) {
        //cause of vertex consumer chaining bug...
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        float zOffset = -1;
        outlineVC.addVertex(matrix4f, 0.0F, 128.0F, zOffset).setColor(255, 255, 255, 255);
        outlineVC.setUv(0.0F, 1.0F)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
        outlineVC.addVertex(matrix4f, 128.0F, 128.0F, zOffset).setColor(255, 255, 255, 255);
        outlineVC.setUv(1.0F, 1.0F)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
        outlineVC.addVertex(matrix4f, 128.0F, 0.0F, zOffset).setColor(255, 255, 255, 255);
        outlineVC.setUv(1.0F, 0.0F)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
        outlineVC.addVertex(matrix4f, 0.0F, 0.0F, zOffset).setColor(255, 255, 255, 255);
        outlineVC.setUv(0.0F, 0.0F)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    protected void applyScissors(GuiGraphics graphics, int x, int y, int x1, int y1) {
        graphics.enableScissor(x, y, x1, y1);
    }

    @Nullable
    public abstract AtlasMap getMapWithCenter(int centerX, int centerZ);

    public void setFollowingPlayer(boolean followingPlayer) {
        this.followingPlayer = followingPlayer;
    }

    private Matrix4f drawMap(Player player, PoseStack poseStack, MultiBufferSource.BufferSource vcp,
                             int ix, int iy, AtlasMap state, int light) {
        // Draw the map
        int curMapComponentX = (MAP_DIMENSION * iy) - MAP_DIMENSION / 2;
        int curMapComponentY = (MAP_DIMENSION * ix) - MAP_DIMENSION / 2;
        poseStack.pushPose();
        poseStack.translate(curMapComponentX, curMapComponentY, 0.0);

        MapItemSavedData data = state.data;
        boolean drawPlayerIcons = !this.drawBigPlayerMarker && data.dimension.equals(player.level().dimension());

        var hidden = swapOutPlayerMarkers(data, player, drawPlayerIcons);

        light = MapAtlasesClient.debugIsMapUpdated(light, state.id, state.type);

        try {
            Minecraft.getInstance().gameRenderer.getMapRenderer()
                    .render(
                            poseStack,
                            vcp,
                            state.id,
                            data,
                            false,//(1+ix+iy)*50
                            light //
                    );
        } finally {
            //adds back the off-map player icons after render
            for (Map.Entry<String, MapDecoration> e : hidden) {
                data.decorations.put(e.getKey(), e.getValue());
            }
        }

        Matrix4f pose = new Matrix4f(poseStack.last().pose());
        poseStack.popPose();
        return pose;
    }

    private List<Map.Entry<String, MapDecoration>> swapOutPlayerMarkers(MapItemSavedData data, Player player, boolean drawPlayerIcons) {
        // Remove the off-map player icons temporarily during render
        List<Map.Entry<String, MapDecoration>> removed = new ArrayList<>();
        List<Map.Entry<String, MapDecoration>> added = new ArrayList<>();
        // Only remove the off-map icon if it's not the active map, or it's not the active dimension
        for (var e : data.decorations.entrySet()) {
            MapDecoration dec = e.getValue();
            var type = dec.type();
            if (type.is(MapDecorationTypes.PLAYER_OFF_MAP) || type.is(MapDecorationTypes.PLAYER_OFF_LIMITS)) {
                if (data == mapWherePlayerIs.data && drawPlayerIcons) {
                    removed.add(e);
                    added.add(new AbstractMap.SimpleEntry<>(e.getKey(), new MapDecoration(MapDecorationTypes.PLAYER,
                            dec.x(), dec.y(), getPlayerMarkerRot(player), dec.name())));
                } else removed.add(e);

            } else if (type.is(MapDecorationTypes.PLAYER)) {
                if (!drawPlayerIcons || data != mapWherePlayerIs.data) {
                    removed.add(e);
                } else {
                    int i = 1 << data.scale;
                    float f = (float) (player.getX() - data.centerX) / i;
                    float f1 = (float) (player.getZ() - data.centerZ) / i;
                    byte b0 = (byte) ((int) ((f * 2.0F) + 0.5D));
                    byte b1 = (byte) ((int) ((f1 * 2.0F) + 0.5D));
                    added.add(new AbstractMap.SimpleEntry<>(e.getKey(), new MapDecoration(MapDecorationTypes.PLAYER,
                            b0, b1, getPlayerMarkerRot(player), dec.name())));
                    //add accurate player
                }
            }
        }

        removed.forEach(d -> data.decorations.remove(d.getKey()));
        added.forEach(d -> data.decorations.put(d.getKey(), d.getValue()));
        return removed;
    }

    private static byte getPlayerMarkerRot(Player p) {
        float pRotation = p.getYRot();
        pRotation += pRotation < 0.0D ? -8.0D : 8.0D;
        return (byte) ((int) (pRotation * 16.0D / 360.0D));
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
