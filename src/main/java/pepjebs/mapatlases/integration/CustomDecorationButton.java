package pepjebs.mapatlases.integration;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.mehvahdjukaar.moonlight.api.map.CustomMapDecoration;
import net.mehvahdjukaar.moonlight.api.map.ExpandedMapData;
import net.mehvahdjukaar.moonlight.api.map.client.DecorationRenderer;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.client.screen.AtlasOverviewScreen;
import pepjebs.mapatlases.client.screen.DecorationBookmarkButton;
import pepjebs.mapatlases.networking.C2SRemoveMarkerPacket;
import pepjebs.mapatlases.networking.MapAtlasesNetworking;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.util.Locale;
import java.util.Map;


public class CustomDecorationButton extends DecorationBookmarkButton {

    public static DecorationBookmarkButton create(int px, int py, AtlasOverviewScreen screen, MapDataHolder data, Object mapDecoration) {
        return new CustomDecorationButton(px, py, screen, data, (CustomMapDecoration) mapDecoration);
    }

    private final CustomMapDecoration decoration;

    private CustomDecorationButton(int px, int py, AtlasOverviewScreen screen, MapDataHolder data, CustomMapDecoration mapDecoration) {
        super(px, py, screen, data);
        this.decoration = mapDecoration;
        this.tooltip = (createTooltip());
    }

    @Override
    public double getWorldX() {
        return mapData.data.x - getDecorationPos(decoration.getX(), mapData.data);
    }

    @Override
    public double getWorldZ() {
        return mapData.data.z - getDecorationPos(decoration.getY(), mapData.data);
    }

    @Override
    public int getBatchGroup() {
        return 1;
    }

    @Override
    public Component getDecorationName() {
        Component displayName = decoration.getDisplayName();
        return displayName == null
                ? Component.literal(
                AtlasOverviewScreen.getReadableName(Utils.getID(decoration.getType()).getPath()
                        .toLowerCase(Locale.ROOT)))
                : displayName;
    }

    @Override
    protected void renderDecoration(PoseStack pGuiGraphics, int pMouseX, int pMouseY) {
        renderStaticMarker(pGuiGraphics, decoration, mapData.data, x + width / 2f, y     + height / 2f, index, false);
    }


    @Override
    protected void deleteMarker() {
        Map<String, CustomMapDecoration> decorations = ((ExpandedMapData) mapData.data).getCustomDecorations();
        for (var d : decorations.entrySet()) {
            CustomMapDecoration deco = d.getValue();
            if (deco == decoration) {
                MapAtlasesNetworking.sendToServer(new C2SRemoveMarkerPacket(mapData.stringId, deco.hashCode()));
                decorations.remove(d.getKey());
                ClientMarker.removeDeco(mapData.stringId, d.getKey());
                return;
            }
        }
    }

    public static void renderStaticMarker(PoseStack poseStack, CustomMapDecoration decoration,
                                          MapItemSavedData data, float x, float y, int index, boolean outline) {
        DecorationRenderer<CustomMapDecoration> renderer = MapDecorationClientManager.getRenderer(decoration);

        if (renderer != null) {

            poseStack.pushPose();
            poseStack.translate(x, y, 1.0D);

            // de translates by the amount the decoration renderer will translate
            poseStack.translate(-(float) decoration.getX() / 2.0F - 64.0F,
                    -(float) decoration.getY() / 2.0F - 64.0F, -0.02F);

            var buffer = Minecraft.getInstance().renderBuffers().bufferSource();

            renderer.rendersText = false;

            if (outline) {
                RenderSystem.setShaderColor(1, 1, 1, 1);
                VertexConsumer vb2 = buffer.getBuffer(R.COLOR_TEXT);
                for(int j = -1; j <= 1; ++j) {
                    for(int k = -1; k <= 1; ++k) {
                        if (j != 0 || k != 0) {
                            poseStack.pushPose();
                            poseStack.translate(j*0.5,k*0.5, -0.01);
                            renderer.render(decoration, poseStack,
                                    vb2, buffer, data,
                                    false, LightTexture.FULL_BRIGHT, index);
                            poseStack.popPose();
                        }
                    }
                }
            }
            VertexConsumer vertexBuilder = buffer.getBuffer(MapDecorationClientManager.MAP_MARKERS_RENDER_TYPE);

            renderer.render(decoration, poseStack,
                    vertexBuilder, buffer, data,
                    false, LightTexture.FULL_BRIGHT, index);
            renderer.rendersText = true;

            poseStack.popPose();
        }
    }

    private static class R extends RenderType {
        protected static final RenderStateShard.ShaderStateShard SHARD = new RenderStateShard.ShaderStateShard(
                () -> MapAtlasesClient.TEXT_ALPHA_SHADER);

        public static final RenderType COLOR_TEXT = create("map_atlases_text_colored",
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, 256, false, true,

                CompositeState.builder()
                        .setShaderState(SHARD)
                        .setTextureState(new TextureStateShard(MapDecorationClientManager.LOCATION_MAP_MARKERS,
                                false, true))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setLightmapState(LIGHTMAP)
                        .createCompositeState(false));

        public R(String pName, VertexFormat pFormat, VertexFormat.Mode pMode, int pBufferSize, boolean pAffectsCrumbling, boolean pSortOnUpload, Runnable pSetupState, Runnable pClearState) {
            super(pName, pFormat, pMode, pBufferSize, pAffectsCrumbling, pSortOnUpload, pSetupState, pClearState);
        }
    }

}
