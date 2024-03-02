package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.integration.moonlight.CustomDecorationButton;
import pepjebs.mapatlases.networking.C2SRemoveMarkerPacket;
import pepjebs.mapatlases.networking.MapAtlasesNetworking;
import pepjebs.mapatlases.utils.DecorationHolder;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

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
        this.tooltip = (this.createTooltip());
        return false;
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        this.shfting = Screen.hasShiftDown();
        this.control = Screen.hasControlDown();
        this.tooltip = (this.createTooltip());
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
    public void renderButton(PoseStack matrices, int pMouseX, int pMouseY, float pPartialTick) {
        matrices.pushPose();
        matrices.translate(0, 0, 0.01 * this.index);
        super.renderButton(matrices, pMouseX, pMouseY, pPartialTick);
        RenderSystem.setShaderTexture(0,AtlasOverviewScreen.ATLAS_TEXTURE);
        if (this.control && canFocusMarker()) {
            blit(matrices, x, y, 24, 173, 5, 5);
        } else if (this.shfting && canDeleteMarker()) {
            blit(matrices, x, y, 24, 167, 5, 5);
        }
        renderDecoration(matrices, pMouseX, pMouseY);

        matrices.popPose();

        //hide waiting to be activated by mapWidget
        setSelected(false);

    }

    protected abstract void renderDecoration(PoseStack graphics, int mouseX, int mouseY);

    @Override
    public List<Component> createTooltip() {
        if (control && canFocusMarker()) {
            return List.of(Component.translatable("tooltip.map_atlases.focus_marker"));
        }
        if (shfting && canDeleteMarker()) {
            return List.of(Component.translatable("tooltip.map_atlases.delete_marker"));
        }
        Component mapIconComponent = getDecorationName();
        Component coordsComponent = Component.literal("X: " + (int) getWorldX() + ", Z: " + (int) getWorldZ())
                .withStyle(ChatFormatting.GRAY);
        var t = mapIconComponent;
        var t2 = coordsComponent;
        return List.of(t, t2);
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
            this.tooltip = (createTooltip());
            this.isBanner = decoration.getType().name().startsWith("BANNER");
        }

        @Override
        protected boolean canDeleteMarker() {
            return isBanner;
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
        public Component getDecorationName() {
            var name = decoration.getName();
            return name == null
                    ? Component.literal(
                    AtlasOverviewScreen.getReadableName(decoration.getType().name().toLowerCase(Locale.ROOT)))
                    : name;
        }

        @Override
        protected void renderDecoration(PoseStack matrices, int pMouseX, int pMouseY) {
            byte b = decoration.getImage();

            int u = (b % 16) * 8;
            int v = (b / 16) * 8;

            matrices.translate(x + width / 2f, y + height / 2f, 0.001);
            matrices.mulPose(Vector3f.ZP.rotationDegrees((decoration.getRot() * 360) / 16.0F));
            matrices.scale(-1, -1, 1);

            RenderSystem.setShaderTexture(0, MAP_ICON_TEXTURE);
            blit(matrices, -4, -4, u, v, 8, 8, 128, 128);

        }


        @Override
        protected void deleteMarker() {
            Map<String, MapDecoration> decorations = mapData.data.decorations;
            var d = decorations.get(decorationId);
            if (d != null) {
                //we cant use string id because server has them different...
                MapAtlasesNetworking.CHANNEL.sendToServer(new C2SRemoveMarkerPacket(mapData.stringId,
                        d.hashCode(), false));

                //removes immediately from client so we update gui
                decorations.remove(decorationId);
            }

        }
    }


}
