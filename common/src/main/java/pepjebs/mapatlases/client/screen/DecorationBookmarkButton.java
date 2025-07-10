package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mehvahdjukaar.moonlight.api.client.util.RenderUtil;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MapDecorationTextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.PlatStuff;
import pepjebs.mapatlases.client.CompoundTooltip;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.integration.moonlight.CustomDecorationButton;
import pepjebs.mapatlases.networking.C2SRemoveMarkerPacket;
import pepjebs.mapatlases.utils.DecorationHolder;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.util.Locale;
import java.util.Map;

import static pepjebs.mapatlases.client.AbstractAtlasWidget.MAP_DIMENSION;
import static pepjebs.mapatlases.client.MapAtlasesClient.DELETE_MARKER_SPRITE;
import static pepjebs.mapatlases.client.MapAtlasesClient.FOCUS_MARKER_SPRITE;

public abstract class DecorationBookmarkButton extends BookmarkButton {

    private static final int BUTTON_H = 14;
    private static final int BUTTON_W = 24;
    protected final MapDataHolder mapData;
    protected final String decorationId;

    protected int index = 0;
    protected boolean shfting = false;
    protected boolean control = false;

    protected DecorationBookmarkButton(int pX, int pY, AtlasOverviewScreen parentScreen, MapDataHolder data, String id) {
        super(pX - BUTTON_W, pY, BUTTON_W, BUTTON_H, parentScreen,
                MapAtlasesClient.BOOKMARK_LEFT_SPRITE, MapAtlasesClient.BOOKMARK_LEFT_SELECTED_SPRITE);
        this.mapData = data;
        this.decorationId = id;
        this.shfting = Screen.hasShiftDown();
        this.control = Screen.hasShiftDown();
    }

    public static DecorationBookmarkButton of(int px, int py, DecorationHolder holder, AtlasOverviewScreen screen) {
        if (holder.deco() instanceof MapDecoration md)
            return new Vanilla(px, py, screen, holder.data(), md, holder.id());
        else {
            return CustomDecorationButton.create(px, py, screen, holder.data(), holder.deco(), holder.id());
        }
    }

    @Override
    public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
        this.shfting = Screen.hasShiftDown();
        this.control = Screen.hasControlDown();
        this.setTooltip(this.createTooltip());
        return false;
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        this.shfting = Screen.hasShiftDown();
        this.control = Screen.hasControlDown();
        this.setTooltip(this.createTooltip());
        return false;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.setSelected(true);
        if (shfting) {
            this.deleteMarker();
            parentScreen.recalculateDecorationWidgets();
        } else {
            parentScreen.centerOnDecoration(this);
        }
    }

    //@Override
    public void onClick(double mouseX, double mouseY, int button) {
        onClick(mouseX, mouseY);
    }

    protected abstract void deleteMarker();


    public abstract double getWorldX();

    public abstract double getWorldZ();

    public abstract Component getDecorationName();

    protected static double getDecorationPos(int decoX, MapItemSavedData data) {
        float s = (1 << data.scale) * (float) MAP_DIMENSION;
        return (s / 2.0d) - ((s / 2.0d) * ((decoX + MAP_DIMENSION) / (float) MAP_DIMENSION));
    }

    public int getBatchGroup() {
        return 0;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        PoseStack matrices = graphics.pose();
        matrices.pushPose();
        matrices.translate(0, 0, 0.01 * this.index);
        super.renderWidget(graphics, pMouseX, pMouseY, pPartialTick);
        if (!parentScreen.isPlacingPin() && !parentScreen.isEditingText()) {
            if (this.control && canFocusMarker()) {
                graphics.blitSprite(FOCUS_MARKER_SPRITE, getX(), getY(), 5, 5);
            } else if (this.shfting && canDeleteMarker()) {
                graphics.blitSprite(DELETE_MARKER_SPRITE, getX(), getY(), 5, 5);
            }
        }
        renderDecoration(graphics, pMouseX, pMouseY);

        matrices.popPose();

        //hide waiting to be activated by mapWidget
        setSelected(false);
    }

    protected abstract void renderDecoration(GuiGraphics graphics, int mouseX, int mouseY);

    @Override
    public Tooltip createTooltip() {
        if (control && canFocusMarker()) {
            return Tooltip.create(Component.translatable("tooltip.map_atlases.focus_marker"));
        }
        if (shfting && canDeleteMarker()) {
            return Tooltip.create(Component.translatable("tooltip.map_atlases.delete_marker"));
        }
        Component mapIconComponent = getDecorationName();
        Tooltip t = Tooltip.create(mapIconComponent);
        if (!MapAtlasesClientConfig.drawWorldMapCoords.get()) {
            return t;
        }
        Component coordsComponent = Component.literal("X: " + (int) getWorldX() + ", Z: " + (int) getWorldZ())
                .withStyle(ChatFormatting.GRAY);
        Tooltip t2 = Tooltip.create(coordsComponent);
        return CompoundTooltip.create(t, t2);
    }

    protected boolean canFocusMarker() {
        return false;
    }

    protected boolean canDeleteMarker() {
        return true;
    }


    public static class Vanilla extends DecorationBookmarkButton {

        private final MapDecoration decoration; // might not match what on map
        private final boolean canRemove;

        public Vanilla(int px, int py, AtlasOverviewScreen screen, MapDataHolder data, MapDecoration mapDecoration, String decoId) {
            super(px, py, screen, data, decoId);
            this.decoration = mapDecoration;
            this.setTooltip(createTooltip());
            this.canRemove = !decoration.type().value().explorationMapElement();
        }

        @Override
        protected boolean canDeleteMarker() {
            return canRemove;
        }

        @Override
        public double getWorldX() {
            return mapData.data.centerX - getDecorationPos(decoration.x(), mapData.data);
        }

        @Override
        public double getWorldZ() {
            return mapData.data.centerZ - getDecorationPos(decoration.y(), mapData.data);
        }


        @Override
        public Component getDecorationName() {
            var name = decoration.name();
            return name.orElseGet(() -> Component.literal(
                    AtlasOverviewScreen.getReadableName(decoration.type().unwrapKey().get()
                            .location().getPath().toLowerCase(Locale.ROOT))));
        }

        @Override
        protected void renderDecoration(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
            PoseStack matrices = pGuiGraphics.pose();
            MultiBufferSource.BufferSource bufferSource = pGuiGraphics.bufferSource();

            matrices.translate(getX() + width / 2f, getY() + height / 2f, 0.001);
            matrices.mulPose(Axis.ZP.rotationDegrees((decoration.rot() * 360) / 16.0F));
            matrices.scale(-4, -4, 1);

            MapDecorationTextureManager textures = Minecraft.getInstance().gameRenderer.getMapRenderer().decorationTextures;
            if (!PlatStuff.renderForgeMapDecoration(decoration, matrices, bufferSource, mapData.data,
                    textures, true, LightTexture.FULL_BRIGHT, 0)) {
                TextureAtlasSprite textureAtlasSprite = textures.get(decoration);
                VertexConsumer vertexConsumer = pGuiGraphics.bufferSource()
                        .getBuffer(RenderType.text(textureAtlasSprite.atlasLocation()));

                RenderUtil.renderSprite(matrices, vertexConsumer, LightTexture.FULL_BRIGHT, 255, 255, 255, 255, textureAtlasSprite);
            }
        }


        @Override
        protected void deleteMarker() {
            Map<String, MapDecoration> decorations = mapData.data.decorations;
            var d = decorations.get(decorationId);
            if (d != null) {
                //we cant use string id because server has them different...
                NetworkHelper.sendToServer(new C2SRemoveMarkerPacket(mapData.id, mapData.type,
                        d.hashCode(), false));

                //removes immediately from client so we update gui
                decorations.remove(decorationId);
            }

        }
    }


}
