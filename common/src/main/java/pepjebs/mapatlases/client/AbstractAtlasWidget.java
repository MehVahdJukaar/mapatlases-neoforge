package pepjebs.mapatlases.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.ImmediatelyFastCompat;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.MapType;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractAtlasWidget {

    public static final Material MAP_BORDER = new Material(
            new ResourceLocation("textures/atlas/shulker_boxes.png"), //so we have mipmap here too
            MapAtlasesMod.res("gui/screen/map_border"));

    public static final int MAP_DIMENSION = 128;


    //internally controls how many maps are displayed
    protected final int atlasesCount;
    protected int mapBlocksSize;
    protected MapDataHolder mapWherePlayerIs;

    protected boolean followingPlayer = true;
    protected double currentXCenter;
    protected double currentZCenter;
    protected float zoomLevel = 3;

    protected boolean rotatesWithPlayer = false;
    protected boolean drawBigPlayerMarker = true;

    protected AbstractAtlasWidget(int atlasesCount) {
        this.atlasesCount = atlasesCount;
    }

    protected void initialize( MapDataHolder newCenter) {
        if (mapWherePlayerIs == null || !mapWherePlayerIs.slice.isSameGroup(newCenter.slice)) {
            this.zoomLevel = atlasesCount * newCenter.type.getDefaultZoomFactor();
        }
        this.mapWherePlayerIs = newCenter;
        this.mapBlocksSize = (1 << mapWherePlayerIs.data.scale) * MAP_DIMENSION;

        this.currentXCenter = mapWherePlayerIs.data.centerX;
        this.currentZCenter = mapWherePlayerIs.data.centerZ;
    }

    public void drawAtlas(GuiGraphics graphics, int x, int y, int width, int height,
                          Player player, float zoomLevelDim, boolean showBorders, MapType type, int light) {

        MapAtlasesClient.setIsDrawingAtlas(true);

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        float widgetScale = width / (float) (atlasesCount * MAP_DIMENSION);
        float zoomScale = atlasesCount / zoomLevelDim;

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

        List<Matrix4f> outlineHack = new ArrayList<>();

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
        int radius = (int) (mapBlocksSize * atlasesCount * 0.71f); // radius using hyp

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
                    getAndDrawMap(player, poseStack, centerMapX, centerMapZ, vcp, outlineHack, i, j, light);
                }
            }
        }
        vcp.endBatch();

        if (showBorders) {

            if (MapAtlasesMod.IMMEDIATELY_FAST) ImmediatelyFastCompat.startBatching();

            VertexConsumer outlineVC = MAP_BORDER.buffer(vcp, RenderType::text); //its already on block atlas
            //using this so we use mipmap
            int a = 50;
            for (var matrix4f : outlineHack) {
                //cause of vertex consumer chaining bug...
                float zOffset = -0.01F;
                outlineVC.vertex(matrix4f, 0.0F, 128.0F, zOffset).color(255, 255, 255, a);
                outlineVC.uv(0.0F, 1.0F)
                        .uv2(LightTexture.FULL_BRIGHT).normal(0, 1, 0).endVertex();
                outlineVC.vertex(matrix4f, 128.0F, 128.0F, zOffset).color(255, 255, 255, a);
                outlineVC.uv(1.0F, 1.0F)
                        .uv2(LightTexture.FULL_BRIGHT).normal(0, 1, 0).endVertex();
                outlineVC.vertex(matrix4f, 128.0F, 0.0F, zOffset).color(255, 255, 255, a);
                outlineVC.uv(1.0F, 0.0F)
                        .uv2(LightTexture.FULL_BRIGHT).normal(0, 1, 0).endVertex();
                outlineVC.vertex(matrix4f, 0.0F, 0.0F, zOffset).color(255, 255, 255, a);
                outlineVC.uv(0.0F, 0.0F)
                        .uv2(LightTexture.FULL_BRIGHT).normal(0, 1, 0).endVertex();
            }
            vcp.endBatch();

            if (MapAtlasesMod.IMMEDIATELY_FAST) ImmediatelyFastCompat.endBatching();
        }

        poseStack.popPose();
        graphics.disableScissor();

        MapAtlasesClient.setIsDrawingAtlas(false);

    }

    protected void applyScissors(GuiGraphics graphics, int x, int y, int x1, int y1) {
        graphics.enableScissor(x, y, x1, y1);
    }

    private void getAndDrawMap(Player player, PoseStack poseStack, int centerMapX, int centerMapZ, MultiBufferSource.BufferSource vcp,
                               List<Matrix4f> outlineHack, int i, int j, int light) {
        int reqXCenter = centerMapX + (j * mapBlocksSize);
        int reqZCenter = centerMapZ + (i * mapBlocksSize);
        MapDataHolder state = getMapWithCenter(reqXCenter, reqZCenter);
        if (state != null) {
            MapItemSavedData data = state.data;
            boolean drawPlayerIcons = !this.drawBigPlayerMarker && data.dimension.equals(player.level().dimension());
            // drawPlayerIcons = drawPlayerIcons && originalCenterMap == state.getSecond();
            this.drawMap(player, poseStack, vcp, outlineHack, i, j, state, drawPlayerIcons, light);
        }
    }

    @Nullable
    public abstract MapDataHolder getMapWithCenter(int centerX, int centerZ);

    public void setFollowingPlayer(boolean followingPlayer) {
        this.followingPlayer = followingPlayer;
    }

    private void drawMap(
            Player player,
            PoseStack poseStack,
            MultiBufferSource.BufferSource vcp,
            List<Matrix4f> outlineHack,
            int ix, int iy,
            MapDataHolder state,
            boolean drawPlayerIcons,
            int light
    ) {
        // Draw the map
        int curMapComponentX = (MAP_DIMENSION * iy) - MAP_DIMENSION / 2;
        int curMapComponentY = (MAP_DIMENSION * ix) - MAP_DIMENSION / 2;
        poseStack.pushPose();
        poseStack.translate(curMapComponentX, curMapComponentY, 0.0);

        // Remove the off-map player icons temporarily during render
        MapItemSavedData data = state.data;
        List<Map.Entry<String, MapDecoration>> removed = new ArrayList<>();
        List<Map.Entry<String, MapDecoration>> added = new ArrayList<>();
        // Only remove the off-map icon if it's not the active map, or it's not the active dimension
        for (var e : data.decorations.entrySet()) {
            MapDecoration dec = e.getValue();
            MapDecoration.Type type = dec.getType();
            if (type == MapDecoration.Type.PLAYER_OFF_MAP || type == MapDecoration.Type.PLAYER_OFF_LIMITS) {
                if (data == mapWherePlayerIs.data && drawPlayerIcons) {
                    removed.add(e);
                    added.add(new AbstractMap.SimpleEntry<>(e.getKey(), new MapDecoration(MapDecoration.Type.PLAYER,
                            dec.getX(), dec.getY(), getPlayerMarkerRot(player), dec.getName())));
                } else removed.add(e);

            } else if (type == MapDecoration.Type.PLAYER) {
                if (!drawPlayerIcons || data != mapWherePlayerIs.data) {
                    removed.add(e);
                } else {
                    int i = 1 << data.scale;
                    float f = (float) (player.getX() - data.centerX) / i;
                    float f1 = (float) (player.getZ() - data.centerZ) / i;
                    byte b0 = (byte) ((int) ((f * 2.0F) + 0.5D));
                    byte b1 = (byte) ((int) ((f1 * 2.0F) + 0.5D));
                    added.add(new AbstractMap.SimpleEntry<>(e.getKey(), new MapDecoration(MapDecoration.Type.PLAYER,
                            b0, b1, getPlayerMarkerRot(player), dec.getName())));
                    //add accurate player
                }
            }
        }

        removed.forEach(d -> data.decorations.remove(d.getKey()));
        added.forEach(d -> data.decorations.put(d.getKey(), d.getValue()));

        light = MapAtlasesClient.debugIsMapUpdated(light, state.stringId);

        Minecraft.getInstance().gameRenderer.getMapRenderer()
                .render(
                        poseStack,
                        vcp,
                        state.id,
                        data,
                        false,//(1+ix+iy)*50
                        light //
                );

        outlineHack.add(new Matrix4f(poseStack.last().pose()));

        poseStack.popPose();
        // Re-add the off-map player icons after render
        for (Map.Entry<String, MapDecoration> e : removed) {
            data.decorations.put(e.getKey(), e.getValue());
        }
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
