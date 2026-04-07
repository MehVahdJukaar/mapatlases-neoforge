package pepjebs.mapatlases.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.client.screen.AtlasOverviewScreen;
import pepjebs.mapatlases.integration.moonlight.ClientMarkers;
import pepjebs.mapatlases.integration.moonlight.InternalPinDecoration;
import pepjebs.mapatlases.integration.moonlight.MoonlightCompat;
import pepjebs.mapatlases.utils.DecorationHolder;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AtlasInHandRenderer {

    private static final RenderType MAP_BACKGROUND =
            RenderTypes.text(Identifier.withDefaultNamespace("textures/map/map_background.png"));
    private static final RenderType MAP_BACKGROUND_CHECKERBOARD =
            RenderTypes.text(Identifier.withDefaultNamespace("textures/map/map_background_checkerboard.png"));
    private static final float MAP_PRE_ROT_SCALE = 0.38F;
    private static final float MAP_GLOBAL_X_POS = -0.5F;
    private static final float MAP_GLOBAL_Y_POS = -0.5F;
    private static final float MAP_GLOBAL_Z_POS = 0.0F;
    private static final float MAP_FINAL_SCALE = 0.0078125F;
    private static final int MAP_BORDER = 7;
    private static final int MAP_HEIGHT = 128;
    private static final int MAP_WIDTH = 128;
    private static final MapRenderState MAP_RENDER_STATE = new MapRenderState();

    public static void render(PoseStack pPoseStack, SubmitNodeCollector submitNodeCollector, int pCombinedLight,
                              ItemStack pStack, Minecraft mc) {
        if (mc.screen instanceof AtlasOverviewScreen) return;


        MapDataHolder state = MapAtlasesClient.getActiveMap();
        if (state != null) {
            MapAtlasesClient.setIsDrawingAtlas(true);
            try {
                pPoseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                pPoseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
                pPoseStack.scale(MAP_PRE_ROT_SCALE, MAP_PRE_ROT_SCALE, MAP_PRE_ROT_SCALE);
                pPoseStack.translate(MAP_GLOBAL_X_POS, MAP_GLOBAL_Y_POS, MAP_GLOBAL_Z_POS);
                pPoseStack.scale(MAP_FINAL_SCALE, MAP_FINAL_SCALE, MAP_FINAL_SCALE);

                MapItemSavedData data = state.data;
                submitNodeCollector.submitCustomGeometry(
                        pPoseStack,
                        data == null ? MAP_BACKGROUND : MAP_BACKGROUND_CHECKERBOARD,
                        (pose, vertexConsumer) -> {
                            vertexConsumer.addVertex(pose, -MAP_BORDER, MAP_HEIGHT + MAP_BORDER, 0.0F)
                                    .setColor(-1)
                                    .setUv(0.0F, 1.0F)
                                    .setLight(pCombinedLight);
                            vertexConsumer.addVertex(pose, MAP_WIDTH + MAP_BORDER, MAP_HEIGHT + MAP_BORDER, 0.0F)
                                    .setColor(-1)
                                    .setUv(1.0F, 1.0F)
                                    .setLight(pCombinedLight);
                            vertexConsumer.addVertex(pose, MAP_WIDTH + MAP_BORDER, -MAP_BORDER, 0.0F)
                                    .setColor(-1)
                                    .setUv(1.0F, 0.0F)
                                    .setLight(pCombinedLight);
                            vertexConsumer.addVertex(pose, -MAP_BORDER, -MAP_BORDER, 0.0F)
                                    .setColor(-1)
                                    .setUv(0.0F, 0.0F)
                                    .setLight(pCombinedLight);
                        }
                );
                if (data != null) {
                    renderMapData(pPoseStack, submitNodeCollector, pCombinedLight, mc, state, data);
                }
            } finally {
                MapAtlasesClient.setIsDrawingAtlas(false);
            }
        }
    }

    private static void renderMapData(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int combinedLight,
                                      Minecraft minecraft, MapDataHolder state, MapItemSavedData data) {
        List<DecorationHolder> customPins = new ArrayList<>(MoonlightCompat.getCustomDecorations(state));
        Map<String, MapDecoration> decorations = MapAtlasesClient.getMutableDecorations(data);
        List<Map.Entry<String, MapDecoration>> removedPins = new ArrayList<>();
        for (var entry : decorations.entrySet()) {
            if (MoonlightCompat.isCustomDecoration(entry.getKey(), entry.getValue())) {
                removedPins.add(entry);
            }
        }

        try {
            removedPins.forEach(entry -> decorations.remove(entry.getKey()));
            MapAtlasesClient.markDecorationsDirty(data);

            var mapRenderer = minecraft.getMapRenderer();
            mapRenderer.extractRenderState(new MapId(state.id), data, MAP_RENDER_STATE);
            mapRenderer.render(MAP_RENDER_STATE, poseStack, submitNodeCollector, false, combinedLight);
            renderCustomPins(poseStack, submitNodeCollector, combinedLight, customPins);
        } finally {
            removedPins.forEach(entry -> decorations.put(entry.getKey(), entry.getValue()));
            MapAtlasesClient.markDecorationsDirty(data);
        }
    }

    private static void renderCustomPins(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int combinedLight,
                                         List<DecorationHolder> customPins) {
        for (var holder : customPins) {
            if (!(holder.deco() instanceof InternalPinDecoration pin)) {
                continue;
            }
            float x = MAP_WIDTH / 2f + pin.decoration().x() / 2f;
            float y = MAP_HEIGHT / 2f + pin.decoration().y() / 2f;
            renderPin(poseStack, submitNodeCollector, combinedLight, pin, x, y);
        }
    }

    private static void renderPin(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int combinedLight,
                                  InternalPinDecoration pin, float x, float y) {
        float halfSize = 4.0F;
        float left = x - halfSize;
        float right = x + halfSize;
        float top = y - halfSize;
        float bottom = y + halfSize;
        submitNodeCollector.submitCustomGeometry(
                poseStack,
                RenderTypes.text(ClientMarkers.getPinTexture(pin.index(), false)),
                (pose, vertexConsumer) -> {
                    vertexConsumer.addVertex(pose, left, bottom, -0.01F)
                            .setColor(-1)
                            .setUv(0.0F, 1.0F)
                            .setLight(combinedLight);
                    vertexConsumer.addVertex(pose, right, bottom, -0.01F)
                            .setColor(-1)
                            .setUv(1.0F, 1.0F)
                            .setLight(combinedLight);
                    vertexConsumer.addVertex(pose, right, top, -0.01F)
                            .setColor(-1)
                            .setUv(1.0F, 0.0F)
                            .setLight(combinedLight);
                    vertexConsumer.addVertex(pose, left, top, -0.01F)
                            .setColor(-1)
                            .setUv(0.0F, 0.0F)
                            .setLight(combinedLight);
                }
        );
    }
}
