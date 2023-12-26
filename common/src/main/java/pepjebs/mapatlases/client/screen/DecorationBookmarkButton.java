package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.client.CompoundTooltip;
import pepjebs.mapatlases.integration.moonlight.CustomDecorationButton;
import pepjebs.mapatlases.networking.C2SRemoveMarkerPacket;
import pepjebs.mapatlases.networking.MapAtlasesNetworking;
import pepjebs.mapatlases.utils.DecorationHolder;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.util.Locale;
import java.util.Map;

import static pepjebs.mapatlases.client.AbstractAtlasWidget.MAP_DIMENSION;

public abstract class DecorationBookmarkButton extends BookmarkButton {

    public static final ResourceLocation MAP_ICON_TEXTURE = new ResourceLocation("textures/map/map_icons.png");

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
            parentScreen.removeBookmark(this);
        } else {
            parentScreen.focusDecoration(this);
        }
    }

    @Override
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
                graphics.blit(AtlasOverviewScreen.ATLAS_TEXTURE, getX(), getY(),
                        24, 173, 5, 5);
            } else if (this.shfting) {
                graphics.blit(AtlasOverviewScreen.ATLAS_TEXTURE, getX(), getY(),
                        24, 167, 5, 5);
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
        if (shfting) {
            return Tooltip.create(Component.translatable("tooltip.map_atlases.delete_marker"));
        }
        Component mapIconComponent = getDecorationName();
        Component coordsComponent = Component.literal("X: " + (int) getWorldX() + ", Z: " + (int) getWorldZ())
                .withStyle(ChatFormatting.GRAY);
        var t = Tooltip.create(mapIconComponent);
        var t2 = Tooltip.create(coordsComponent);
        return CompoundTooltip.create(t, t2);
    }

    protected boolean canFocusMarker() {
        return false;
    }


    public static class Vanilla extends DecorationBookmarkButton {

        private final MapDecoration decoration; // might not match what on map

        public Vanilla(int px, int py, AtlasOverviewScreen screen, MapDataHolder data, MapDecoration mapDecoration, String decoId) {
            super(px, py, screen, data, decoId);
            this.decoration = mapDecoration;
            this.setTooltip(createTooltip());
        }

        @Override
        public double getWorldX() {
            return mapData.data.centerX - getDecorationPos(decoration.getX(), mapData.data);
        }

        @Override
        public double getWorldZ() {
            return mapData.data.centerZ - getDecorationPos(decoration.getY(), mapData.data);
        }


        @Override
        public Component getDecorationName() {
            var name = decoration.getName();
            return name == null
                    ? Component.literal(
                    AtlasOverviewScreen.getReadableName(decoration.getType().name().toLowerCase(Locale.ROOT)))
                    : name;
        }

        @Override
        protected void renderDecoration(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
            PoseStack matrices = pGuiGraphics.pose();
            byte b = decoration.getImage();

            int u = (b % 16) * 8;
            int v = (b / 16) * 8;

            matrices.translate(getX() + width / 2f, getY() + height / 2f, 0.001);
            matrices.mulPose(Axis.ZP.rotationDegrees((decoration.getRot() * 360) / 16.0F));
            matrices.scale(-1, -1, 1);

            pGuiGraphics.blit(MAP_ICON_TEXTURE, -4, -4, u, v, 8, 8, 128, 128);

        }


        @Override
        protected void deleteMarker() {
            Map<String, MapDecoration> decorations = mapData.data.decorations;
            for (var d : decorations.entrySet()) {
                String targetId = d.getKey();
                if (targetId.equals(decorationId)) {
                    //we cant use string id because server has them diferent...
                    MapAtlasesNetworking.sendToServer(new C2SRemoveMarkerPacket(mapData.stringId, d.getValue().hashCode()));
                    decorations.remove(d.getKey());
                    return;
                }
            }
        }
    }


}
