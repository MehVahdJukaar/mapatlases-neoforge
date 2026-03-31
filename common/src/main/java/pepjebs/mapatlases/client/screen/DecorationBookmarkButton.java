package pepjebs.mapatlases.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.client.CompoundTooltip;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.integration.moonlight.CustomDecorationButton;
import pepjebs.mapatlases.networking.C2SRemoveMarkerPacket;
import pepjebs.mapatlases.networking.MapAtlasesNetworking;
import pepjebs.mapatlases.utils.DecorationHolder;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.util.Locale;
import java.util.Map;

import static pepjebs.mapatlases.client.AbstractAtlasWidget.MAP_DIMENSION;
import static pepjebs.mapatlases.client.MapAtlasesClient.ATLAS_BACKGROUND_TEXTURE;

public abstract class DecorationBookmarkButton extends BookmarkButton {

    private static final int BUTTON_H = 14;
    private static final int BUTTON_W = 24;
    protected final MapDataHolder mapData;
    protected final String decorationId;

    protected int index = 0;
    protected boolean shfting = false;
    protected boolean control = false;

    protected DecorationBookmarkButton(int pX, int pY, AtlasOverviewScreen parentScreen, MapDataHolder data, String id) {
        super(pX - BUTTON_W, pY, BUTTON_W, BUTTON_H, 0, 167 + 36, parentScreen);
        this.mapData = data;
        this.decorationId = id;
        this.shfting = AtlasOverviewScreen.isShiftDown();
        this.control = AtlasOverviewScreen.isControlDown();
    }

    public static DecorationBookmarkButton of(int px, int py, DecorationHolder holder, AtlasOverviewScreen screen) {
        if (holder.deco() instanceof MapDecoration md)
            return new Vanilla(px, py, screen, holder.data(), md, holder.id());
        else {
            return CustomDecorationButton.create(px, py, screen, holder.data(), holder.deco(), holder.id());
        }
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        this.shfting = AtlasOverviewScreen.isShiftDown();
        this.control = AtlasOverviewScreen.isControlDown();
        this.setTooltip(this.createTooltip());
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        this.shfting = AtlasOverviewScreen.isShiftDown();
        this.control = AtlasOverviewScreen.isControlDown();
        this.setTooltip(this.createTooltip());
        return false;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        this.setSelected(true);
        if (shfting) {
            this.deleteMarker();
            parentScreen.recalculateDecorationWidgets();
        } else {
            parentScreen.centerOnDecoration(this);
        }
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
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (this.index != 0) {
            graphics.nextStratum();
        }
        super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
        if (!parentScreen.isPlacingPin() && !parentScreen.isEditingText()) {
            if (this.control && canFocusMarker()) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, ATLAS_BACKGROUND_TEXTURE, getX(), getY(),
                        24, 173, 5, 5, 256, 256);
            } else if (this.shfting && canDeleteMarker()) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, ATLAS_BACKGROUND_TEXTURE, getX(), getY(),
                        24, 167, 5, 5, 256, 256);
            }
        }
        renderDecoration(graphics, mouseX, mouseY);

        //hide waiting to be activated by mapWidget
        setSelected(false);
    }

    protected abstract void renderDecoration(GuiGraphicsExtractor graphics, int mouseX, int mouseY);

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
        return CompoundTooltip.create(mapIconComponent, coordsComponent);
    }

    protected boolean canFocusMarker() {
        return false;
    }

    protected boolean canDeleteMarker() {
        return true;
    }


    public static class Vanilla extends DecorationBookmarkButton {

        private final MapDecoration decoration; // might not match what on map
        private final boolean isBanner;

        public Vanilla(int px, int py, AtlasOverviewScreen screen, MapDataHolder data, MapDecoration mapDecoration, String decoId) {
            super(px, py, screen, data, decoId);
            this.decoration = mapDecoration;
            this.setTooltip(createTooltip());
            this.isBanner = decoration.type().isBound() &&
                    decoration.type().value().assetId().getPath().startsWith("banner_");
        }

        @Override
        protected boolean canDeleteMarker() {
            return isBanner;
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
            return name.isEmpty()
                    ? Component.literal(
                    AtlasOverviewScreen.getReadableName(decoration.type().value().assetId().getPath().toLowerCase(Locale.ROOT)))
                    : name.get();
        }

        @Override
        protected void renderDecoration(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
            graphics.nextStratum();
            graphics.blit(RenderPipelines.GUI_TEXTURED, decoration.getSpriteLocation(),
                    getX() + width / 2 - 4, getY() + height / 2 - 4,
                    0, 0, 8, 8, 8, 8);
        }


        @Override
        protected void deleteMarker() {
            Map<String, MapDecoration> decorations = MapAtlasesClient.getMutableDecorations(mapData.data);
            var d = decorations.get(decorationId);
            if (d != null) {
                //we cant use string id because server has them different...
                MapAtlasesNetworking.CHANNEL.sendToServer(new C2SRemoveMarkerPacket(mapData.stringId,
                        d.hashCode(), false));

                //removes immediately from client so we update gui
                decorations.remove(decorationId);
                MapAtlasesClient.markDecorationsDirty(mapData.data);
            }

        }
    }


}
