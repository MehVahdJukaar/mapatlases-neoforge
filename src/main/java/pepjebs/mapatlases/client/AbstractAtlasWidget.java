package pepjebs.mapatlases.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.utils.Slice;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mojang.blaze3d.platform.GlConst.*;
import static org.lwjgl.opengl.GL11C.glTexParameterf;
import static org.lwjgl.opengl.GL11C.glTexParameteri;
import static org.lwjgl.opengl.GL14.GL_TEXTURE_LOD_BIAS;

public abstract class AbstractAtlasWidget {

    public static final ResourceLocation MAP_BORDER =
            MapAtlasesMod.res("textures/gui/screen/map_border.png");


    public static final int MAP_DIMENSION = 128;


    //internally controls how many maps are displayed
    protected final int atlasesCount;
    protected int mapPixelSize;
    private MapItemSavedData originalCenterMap;

    protected boolean followingPlayer = true;
    protected double currentXCenter;
    protected double currentZCenter;
    protected float zoomLevel = 3;

    protected boolean rotatesWithPlayer = false;
    protected boolean drawBigPlayerMarker = true;

    protected AbstractAtlasWidget(int atlasesCount) {
        this.atlasesCount = atlasesCount;
    }

    protected void initialize(MapItemSavedData originalCenterMap, Slice slice) {
        this.originalCenterMap = originalCenterMap;
        this.mapPixelSize = (1 << originalCenterMap.scale) * MAP_DIMENSION;

        this.currentXCenter = originalCenterMap.centerX;
        this.currentZCenter = originalCenterMap.centerZ;

        this.zoomLevel = atlasesCount * slice.getDefaultZoomFactor();
    }

    public void drawAtlas(GuiGraphics graphics, int x, int y, int width, int height,
                          Player player, float zoomLevelDim, boolean showBorders, Slice slice) {

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        float widgetScale = width / (float) (atlasesCount * MAP_DIMENSION);
        float zoomScale = atlasesCount / zoomLevelDim;

        int intXCenter = (int) (currentXCenter);
        int intZCenter = (int) (currentZCenter);
        int scaleIndex = mapPixelSize / MAP_DIMENSION;

        ColumnPos c = slice.getCenter(intXCenter, intZCenter, mapPixelSize);
        int centerMapX = c.x();
        int centerMapZ = c.z();

        //translate to center
        poseStack.translate(x + width / 2f, y + height / 2f, 0);
        //widget scale + zoom

        poseStack.scale(widgetScale * zoomScale, widgetScale * zoomScale, -1);

        // Draw maps, putting active map in middle of grid

        //MultiBufferSource.BufferSource vcp = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
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
        double sideLength = mapPixelSize * zoomScale;
        //radius of widget
        int radius = (int) (mapPixelSize * atlasesCount * 0.71f); // radius using hyp

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
                    getAndDrawMap(player, poseStack, centerMapX, centerMapZ, vcp, outlineHack, i, j);
                }
            }
        }
        vcp.endBatch();

        if (showBorders) {
            VertexConsumer outlineVC = vcp.getBuffer(RenderType.text(MAP_BORDER));
            int a = 50;
            for (var matrix4f : outlineHack) {
                outlineVC.vertex(matrix4f, 0.0F, 128.0F, -0.02F).color(255, 255, 255, a).uv(0.0F, 1.0F).uv2(LightTexture.FULL_BRIGHT).endVertex();
                outlineVC.vertex(matrix4f, 128.0F, 128.0F, -0.02F).color(255, 255, 255, a).uv(1.0F, 1.0F).uv2(LightTexture.FULL_BRIGHT).endVertex();
                outlineVC.vertex(matrix4f, 128.0F, 0.0F, -0.02F).color(255, 255, 255, a).uv(1.0F, 0.0F).uv2(LightTexture.FULL_BRIGHT).endVertex();
                outlineVC.vertex(matrix4f, 0.0F, 0.0F, -0.02F).color(255, 255, 255, a).uv(0.0F, 0.0F).uv2(LightTexture.FULL_BRIGHT).endVertex();

            }
            vcp.endBatch();
        }

        poseStack.popPose();
        graphics.disableScissor();
    }

    protected void applyScissors(GuiGraphics graphics, int x, int y, int x1, int y1) {
        graphics.enableScissor(x, y, x1, y1);
    }

    private void getAndDrawMap(Player player, PoseStack poseStack, int centerMapX, int centerMapZ, MultiBufferSource.BufferSource vcp,
                               List<Matrix4f> outlineHack, int i, int j) {
        int reqXCenter = centerMapX + (j * mapPixelSize);
        int reqZCenter = centerMapZ + (i * mapPixelSize);
        Pair<Integer, MapItemSavedData> state = getMapWithCenter(reqXCenter, reqZCenter);
        if (state != null) {
            MapItemSavedData data = state.getSecond();
            boolean drawPlayerIcons = !this.drawBigPlayerMarker && data.dimension.equals(player.level().dimension());
            // drawPlayerIcons = drawPlayerIcons && originalCenterMap == state.getSecond();
            this.drawMap(player, poseStack, vcp, outlineHack, i, j, state, drawPlayerIcons);
        }
    }

    @Nullable
    public abstract Pair<Integer, MapItemSavedData> getMapWithCenter(int centerX, int centerZ);

    public void setFollowingPlayer(boolean followingPlayer) {
        this.followingPlayer = followingPlayer;
    }

    private void drawMap(
            Player player,
            PoseStack poseStack,
            MultiBufferSource.BufferSource vcp,
            List<Matrix4f> outlineHack,
            int ix, int iy,
            Pair<Integer, MapItemSavedData> state,
            boolean drawPlayerIcons
    ) {
        // Draw the map
        int curMapComponentX = (MAP_DIMENSION * iy) - MAP_DIMENSION / 2;
        int curMapComponentY = (MAP_DIMENSION * ix) - MAP_DIMENSION / 2;
        poseStack.pushPose();
        poseStack.translate(curMapComponentX, curMapComponentY, 0.0);

        // Remove the off-map player icons temporarily during render
        MapItemSavedData data = state.getSecond();
        List<Map.Entry<String, MapDecoration>> removed = new ArrayList<>();
        List<Map.Entry<String, MapDecoration>> added = new ArrayList<>();
        // Only remove the off-map icon if it's not the active map, or it's not the active dimension
        for (var e : data.decorations.entrySet()) {
            MapDecoration decoration = e.getValue();
            MapDecoration.Type type = decoration.getType();
            if (type == MapDecoration.Type.PLAYER_OFF_MAP || type == MapDecoration.Type.PLAYER_OFF_LIMITS) {
                if (data == originalCenterMap && drawPlayerIcons) {
                    var d = e.getValue();
                    removed.add(e);
                    added.add(new AbstractMap.SimpleEntry<>(e.getKey(), new MapDecoration(MapDecoration.Type.PLAYER,
                            d.getX(), d.getY(), getPlayerMarkerRot(player), d.getName())));
                } else removed.add(e);

            } else if (type == MapDecoration.Type.PLAYER && !drawPlayerIcons) {
                removed.add(e);
            }
        }

        removed.forEach(d -> data.decorations.remove(d.getKey()));
        added.forEach(d -> data.decorations.put(d.getKey(), d.getValue()));

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, 0.0f);
        Minecraft.getInstance().gameRenderer.getMapRenderer()
                .render(
                        poseStack,
                        vcp,
                        state.getFirst(),
                        data,
                        false,//(1+ix+iy)*50
                        LightTexture.FULL_BRIGHT //
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

    /**
     * @return biggest int multiple of factor that is less than value
     */
    public static int roundBelow(int value, int factor) {
        return Mth.roundToward(value, factor) - factor;
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
