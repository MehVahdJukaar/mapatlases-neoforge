package pepjebs.mapatlases.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractAtlasWidget extends GuiComponent {

    public static final ResourceLocation MAP_BORDER =
            MapAtlasesMod.res("textures/gui/screen/map_border.png");


    public static final int MAP_DIMENSION = 128;


    //internally controls how many maps are displayed
    protected final int atlasesCount;
    protected int mapAtlasScale;
    private MapItemSavedData originalCenterMap;

    protected boolean followingPlayer = true;
    protected float currentXCenter;
    protected float currentZCenter;
    protected float zoomLevel = 3;

    protected boolean rotatesWithPlayer = false;

    protected AbstractAtlasWidget(int atlasesCount) {
        this.atlasesCount = atlasesCount;
    }

    protected void initialize(MapItemSavedData originalCenterMap) {
        this.originalCenterMap = originalCenterMap;
        this.mapAtlasScale = (1 << originalCenterMap.scale) * MAP_DIMENSION;

        this.currentXCenter = originalCenterMap.x;
        this.currentZCenter = originalCenterMap.z;
    }

    public void drawAtlas(PoseStack poseStack, int x, int y, int width, int height,
                          Player player, float zoomLevelDim, boolean showBorders) {


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

        //MultiBufferSource.BufferSource vcp = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        MultiBufferSource.BufferSource vcp = Minecraft.getInstance().renderBuffers().bufferSource();


        List<Matrix4f> outlineHack = new ArrayList<>();

        applyScissors(poseStack, x, y, (x + width), (y + height));

        float offsetX = currentXCenter - centerMapX;
        float offsetZ = currentZCenter - centerMapZ;


        //zoom leve is essentially maps on screen
        //dont ask me why all this stuff is like that
        int hz = (int) (Mth.ceil((zoomLevelDim)) / 2f);
        boolean small = false;
        if ( (followingPlayer && atlasesCount == 1) || rotatesWithPlayer) {
            hz+=1;
            small = true;
        }

        if (rotatesWithPlayer) {
            poseStack.mulPose(Vector3f.ZP.rotationDegrees(180 - player.getYRot()));
        }
        poseStack.translate(-offsetX, -offsetZ, 0);

        int minI = -hz;
        int maxI = hz;
        int minJ = -hz;
        int maxJ = hz;
        //adds more maps to draw if needed
        if(!small) {
            if (offsetX < 0) minJ--;
            else if (offsetX > 0) maxJ++;
            if (offsetZ < 0) minI--;
            else if (offsetZ > 0) maxI++;
        }
        for (int i = maxI; i >= minI; i--) {
            for (int j = maxJ; j >= minJ; j--) {
                int reqXCenter = centerMapX + (j * mapAtlasScale);
                int reqZCenter = centerMapZ + (i * mapAtlasScale);
                Pair<String, MapItemSavedData> state = getMapWithCenter(reqXCenter, reqZCenter);
                if (state == null) continue;
                MapItemSavedData data = state.getSecond();
                boolean drawPlayerIcons = data.dimension.equals(player.level.dimension());
                // drawPlayerIcons = drawPlayerIcons && originalCenterMap == state.getSecond();
                this.drawMap(poseStack, vcp, outlineHack, i, j, state, drawPlayerIcons);
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
        GuiComponent.disableScissor();
    }

    protected void applyScissors(PoseStack graphics, int x, int y, int x1, int y1) {
        GuiComponent.enableScissor(x, y, x1, y1);
    }

    public abstract Pair<String, MapItemSavedData> getMapWithCenter(int centerX, int centerZ);

    public void setFollowingPlayer(boolean followingPlayer) {
        this.followingPlayer = followingPlayer;
    }

    private void drawMap(
            PoseStack matrices,
            MultiBufferSource.BufferSource vcp,
            List<Matrix4f> outlineHack,
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


        Minecraft.getInstance().gameRenderer.getMapRenderer()
                .render(
                        matrices,
                        vcp,
                        MapAtlasesAccessUtils.getMapIntFromString(state.getFirst()),
                        data,
                        false,//(1+ix+iy)*50
                        LightTexture.FULL_BRIGHT //
                );

        outlineHack.add(new Matrix4f(matrices.last().pose()));

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
