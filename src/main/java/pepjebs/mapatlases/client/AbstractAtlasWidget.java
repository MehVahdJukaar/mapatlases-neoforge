package pepjebs.mapatlases.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.Material;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.MapType;
import pepjebs.mapatlases.utils.Slice;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mojang.blaze3d.platform.GlConst.*;
import static org.lwjgl.opengl.GL11C.glTexParameterf;
import static org.lwjgl.opengl.GL11C.glTexParameteri;
import static org.lwjgl.opengl.GL14.GL_TEXTURE_LOD_BIAS;

public abstract class AbstractAtlasWidget extends GuiComponent {

    public static final Material MAP_BORDER = new Material(InventoryMenu.BLOCK_ATLAS,
            MapAtlasesMod.res("gui/screen/map_border"));

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

    protected void initialize(MapDataHolder originalCenter) {
        this.originalCenterMap = originalCenter.data;
        this.mapPixelSize = (1 << originalCenterMap.scale) * MAP_DIMENSION;

        this.currentXCenter = originalCenterMap.x;
        this.currentZCenter = originalCenterMap.z;

        this.zoomLevel = atlasesCount * originalCenter.type.getDefaultZoomFactor();
    }

    public void drawAtlas(PoseStack poseStack, int x, int y, int width, int height,
                          Player player, float zoomLevelDim, boolean showBorders, MapType type) {

        poseStack.pushPose();

        float widgetScale = width / (float) (atlasesCount * MAP_DIMENSION);
        float zoomScale = atlasesCount / zoomLevelDim;

        int intXCenter = (int) (currentXCenter);
        int intZCenter = (int) (currentZCenter);
        int scaleIndex = mapPixelSize / MAP_DIMENSION;

        ColumnPos c = type.getCenter(intXCenter, intZCenter, mapPixelSize);
        int centerMapX = c.x();
        int centerMapZ = c.z();

        //translate to center
        poseStack.translate(x + width / 2f, y + height / 2f, 0);
        //widget scale + zoom

        poseStack.scale(widgetScale * zoomScale, widgetScale * zoomScale, -1);

        // Draw maps, putting active map in middle of grid

        //MultiBufferSource.BufferSource vcp = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        MultiBufferSource.BufferSource vcp = Minecraft.getInstance().renderBuffers().bufferSource();


        List<Matrix4f> outlineHack = new ArrayList<>();

        applyScissors(poseStack, x, y, (x + width), (y + height));

        double mapCenterOffsetX = currentXCenter - centerMapX;
        double mapCenterOffsetZ = currentZCenter - centerMapZ;

        //zoom leve is essentially maps on screen
        //dont ask me why all this stuff is like that

        if (rotatesWithPlayer) {
            poseStack.mulPose(Vector3f.ZP.rotationDegrees(180 - player.getYRot()));
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
            VertexConsumer outlineVC = MAP_BORDER.buffer(vcp, RenderType::text); //its already on block atlas
            //using this so we use mipmap
            int a = 50;
            for (var matrix4f : outlineHack) {
                outlineVC.vertex(matrix4f, 0.0F, 128.0F, -0.02F).color(255, 255, 255, a).uv(0.0F, 1.0F)
                        .uv2(LightTexture.FULL_BRIGHT).normal(0,1,0).endVertex();
                outlineVC.vertex(matrix4f, 128.0F, 128.0F, -0.02F).color(255, 255, 255, a).uv(1.0F, 1.0F)
                        .uv2(LightTexture.FULL_BRIGHT).normal(0,1,0).endVertex();
                outlineVC.vertex(matrix4f, 128.0F, 0.0F, -0.02F).color(255, 255, 255, a).uv(1.0F, 0.0F)
                        .uv2(LightTexture.FULL_BRIGHT).normal(0,1,0).endVertex();
                outlineVC.vertex(matrix4f, 0.0F, 0.0F, -0.02F).color(255, 255, 255, a).uv(0.0F, 0.0F)
                        .uv2(LightTexture.FULL_BRIGHT).normal(0,1,0).endVertex();
            }
            vcp.endBatch();
        }

        poseStack.popPose();
         GuiComponent.disableScissor();
    }

    protected void applyScissors(PoseStack graphics, int x, int y, int x1, int y1) {
         GuiComponent.enableScissor(x, y, x1, y1);
    }

    private void getAndDrawMap(Player player, PoseStack poseStack, int centerMapX, int centerMapZ, MultiBufferSource.BufferSource vcp,
                               List<Matrix4f> outlineHack, int i, int j) {
        int reqXCenter = centerMapX + (j * mapPixelSize);
        int reqZCenter = centerMapZ + (i * mapPixelSize);
        MapDataHolder state = getMapWithCenter(reqXCenter, reqZCenter);
        if (state != null) {
            MapItemSavedData data = state.data;
            boolean drawPlayerIcons = !this.drawBigPlayerMarker && data.dimension.equals(player.level.dimension());
            // drawPlayerIcons = drawPlayerIcons && originalCenterMap == state.getSecond();
            this.drawMap(player, poseStack, vcp, outlineHack, i, j, state, drawPlayerIcons);
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
            boolean drawPlayerIcons
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

        //glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
        //glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, 0.0f);
        Minecraft.getInstance().gameRenderer.getMapRenderer()
                .render(
                        poseStack,
                        vcp,
                        state.id,
                        data,
                        false,//(1+ix+iy)*50
                        LightTexture.FULL_BRIGHT //
                );

      //  outlineHack.add(new Matrix4f(poseStack.last().pose()));

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
