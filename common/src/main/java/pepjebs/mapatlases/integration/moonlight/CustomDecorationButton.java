package pepjebs.mapatlases.integration.moonlight;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.moonlight.api.map.CustomMapDecoration;
import net.mehvahdjukaar.moonlight.api.map.ExpandedMapData;
import net.mehvahdjukaar.moonlight.api.map.client.DecorationRenderer;
import net.mehvahdjukaar.moonlight.api.map.client.MapDecorationClientHandler;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.client.screen.AtlasOverviewScreen;
import pepjebs.mapatlases.client.screen.DecorationBookmarkButton;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.util.Locale;


public class CustomDecorationButton extends DecorationBookmarkButton {

    public static DecorationBookmarkButton create(int px, int py, AtlasOverviewScreen screen, MapDataHolder data, Object mapDecoration, String id) {
        return new CustomDecorationButton(px, py, screen, data, (CustomMapDecoration) mapDecoration, id);
    }

    private final CustomMapDecoration decoration; // could not match whats in maps

    private CustomDecorationButton(int px, int py, AtlasOverviewScreen screen,
                                   MapDataHolder data, CustomMapDecoration mapDecoration, String id) {
        super(px, py, screen, data, id);
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
    public void onClick(double mouseX, double mouseY, int button) {
        if (control || button == 1) {
            focusMarker();
        } else super.onClick(mouseX, mouseY);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (control) {
            focusMarker();
        } else super.onClick(mouseX, mouseY);
    }

    @Override
    protected boolean isValidClickButton(int pButton) {
        return (pButton == 0 && canDeleteMarker()) || (pButton == 1 && canFocusMarker());
    }

    protected void focusMarker() {
    }

    @Override
    protected boolean canFocusMarker() {
        return false;
    }

    @Override
    protected void deleteMarker() {
        var decorations = ((ExpandedMapData) mapData.data).getCustomDecorations();
        var d = decorations.get(decorationId);
        if (d != null) {
            //in case this is is a pin
            decorations.remove(decorationId);
        }
    }
    public static void renderStaticMarker(PoseStack poseStack, CustomMapDecoration decoration,
                                          MapItemSavedData data, float x, float y, int index, boolean outline) {
        DecorationRenderer<CustomMapDecoration> renderer = MapDecorationClientHandler.getRenderer(decoration);

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
                var id = Utils.getID(decoration.getType());
                ResourceLocation texture = new ResourceLocation(id.getNamespace(), "map_markers/" + id.getPath());
                VertexConsumer vb2 = buffer.getBuffer(RenderType.text(texture));
                for(int j = -1; j <= 1; ++j) {
                    for(int k = -1; k <= 1; ++k) {
                        if (j != 0 || k != 0) {
                            poseStack.pushPose();
                            poseStack.translate(j*0.5,k*0.5, -0.01);
                            renderer.render(decoration, poseStack,
                                    buffer,vb2, data,
                                    false, LightTexture.FULL_BRIGHT, index);
                            poseStack.popPose();
                        }
                    }
                }
            }

            renderer.render(decoration, poseStack
                    , buffer, data,
                    false, LightTexture.FULL_BRIGHT, index);
            renderer.rendersText = true;

            poseStack.popPose();
        }
    }



}
