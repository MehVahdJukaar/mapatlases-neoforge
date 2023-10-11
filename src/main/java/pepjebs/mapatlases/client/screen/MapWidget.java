package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import pepjebs.mapatlases.client.AbstractAtlasWidget;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.client.ui.MapAtlasesHUD;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.networking.C2STeleportPacket;
import pepjebs.mapatlases.networking.MapAtlasesNetworking;
import pepjebs.mapatlases.utils.MapDataHolder;

import static pepjebs.mapatlases.client.screen.DecorationBookmarkButton.MAP_ICON_TEXTURE;

public class MapWidget extends AbstractAtlasWidget implements Renderable, GuiEventListener, NarratableEntry {


    private static final int PAN_BUCKET = 25;
    private static final int ZOOM_BUCKET = 2;

    private final AtlasOverviewScreen mapScreen;

    protected final int x;
    protected final int y;
    protected final int width;
    protected final int height;

    private float cumulativeZoomValue;
    private float startZoom = 1;
    private float cumulativeMouseX = 0;
    private float cumulativeMouseY = 0;

    protected double targetXCenter;
    protected double targetZCenter;
    protected float targetZoomLevel;

    private boolean isHovered;
    private float animationProgress = 0; //from zero to 1

    private float scaleAlpha = 0;

    public MapWidget(int x, int y, int width, int height, int atlasesCount,
                     AtlasOverviewScreen screen, MapDataHolder originalCenterMap) {
        super(atlasesCount);
        initialize(originalCenterMap);
        this.targetZoomLevel = zoomLevel;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.mapScreen = screen;
        this.drawBigPlayerMarker = false;
    }

    @Override
    protected void applyScissors(GuiGraphics graphics, int x, int y, int x1, int y1) {
        var v = mapScreen.transformPos(x, y);
        var v2 = mapScreen.transformPos(x1, y1);
        super.applyScissors(graphics, (int) v.x, (int) v.y, (int) v2.x, (int) v2.y);
    }

    @Override
    public void render(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;


        this.isHovered = isMouseOver(pMouseX, pMouseY);

        // Handle zooming markers hack
        MapAtlasesClient.setDecorationsScale(zoomLevel * (float) (double) MapAtlasesClientConfig.worldMapDecorationScale.get());

        this.drawAtlas(graphics, x, y, width, height, player, zoomLevel,
                MapAtlasesClientConfig.worldMapBorder.get(), mapScreen.getSelectedSlice().type(), LightTexture.FULL_BRIGHT);

        MapAtlasesClient.setDecorationsScale(1);


        //TODO: fix
        mapScreen.updateVisibleDecoration((int) currentXCenter, (int) currentZCenter,
                zoomLevel / 2f * MAP_DIMENSION, followingPlayer);

        if (isHovered && mapScreen.isPlacingPin()) {
            PoseStack poseStack = graphics.pose();
            poseStack.pushPose();
            poseStack.translate(pMouseX - 2.5f, pMouseY - 2.5f, 10);
            graphics.blit(MAP_ICON_TEXTURE, 0,
                    0,
                    40, 0, 8, 8, 128, 128);
            poseStack.popPose();

        }
        if (this.isHovered && !mapScreen.isEditingText()) {
            this.renderPositionText(graphics, mc.font, pMouseX, pMouseY);

            if (mapScreen.canTeleport()) {
                graphics.renderTooltip(mapScreen.getMinecraft().font,
                        Component.translatable("chat.coordinates.tooltip")
                                .withStyle(ChatFormatting.GREEN),
                        pMouseX, pMouseY);
            }
        }
        renderScaleText(graphics, mc);
    }

    private void renderScaleText(GuiGraphics graphics, Minecraft mc) {
        boolean animation = zoomLevel != targetZoomLevel;
        if (animation || scaleAlpha != 0) {
            if (animation) scaleAlpha = 1;
            else {
                scaleAlpha = Math.max(0, scaleAlpha - 0.03f);
            }
            int a = (int) (scaleAlpha * 255);
            if (a > 10) {
                PoseStack poseStack = graphics.pose();
                poseStack.pushPose();
                poseStack.translate(0, 0, 4);
                graphics.drawString(mc.font,
                        Component.translatable("message.map_atlases.map_scale", (int) targetZoomLevel),
                        x, y + height - 8, FastColor.ABGR32.color(a, 255, 255, 255));
                poseStack.popPose();
            }
        }
    }

    @Override
    public MapDataHolder getMapWithCenter(int centerX, int centerZ) {
        return mapScreen.findMapEntryForCenter(centerX, centerZ);
    }

    private void renderPositionText(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (!MapAtlasesClientConfig.drawWorldMapCoords.get()) return;
        ColumnPos pos = getHoveredPos(mouseX, mouseY);
        float textScaling = (float) (double) MapAtlasesClientConfig.worldMapCoordsScale.get();
        String coordsToDisplay = "X: " + pos.x() + ", Z: " + pos.z();
        MapAtlasesHUD.drawScaledComponent(
                graphics, font, x, y + height + 8, coordsToDisplay, textScaling, width);
    }


    @Override
    public boolean isMouseOver(double pMouseX, double pMouseY) {
        return pMouseX >= this.x && pMouseY >= this.y && pMouseX < (this.x + this.width) && pMouseY < (this.y + this.height);
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double deltaX, double deltaY) {
        if (pButton == 0) {
            cumulativeMouseX += deltaX;
            cumulativeMouseY += deltaY;
            double newXCenter;
            double newZCenter;
            boolean discrete = !MapAtlasesClientConfig.worldMapSmoothPanning.get();
            if (discrete) {
                //discrete mode
                newXCenter = (int) (currentXCenter - (round((int) cumulativeMouseX, PAN_BUCKET) / PAN_BUCKET * mapPixelSize));
                newZCenter = (int) (currentZCenter - (round((int) cumulativeMouseY, PAN_BUCKET) / PAN_BUCKET * mapPixelSize));
            } else {
                newXCenter = (currentXCenter - cumulativeMouseX * zoomLevel * (mapPixelSize / MAP_DIMENSION));
                newZCenter = (currentZCenter - cumulativeMouseY * zoomLevel * (mapPixelSize / MAP_DIMENSION));
            }
            if (newXCenter != currentXCenter) {
                targetXCenter = newXCenter;
                if (!discrete) {
                    currentXCenter = targetXCenter;
                }
                cumulativeMouseX = 0;
            }
            if (newZCenter != currentZCenter) {
                targetZCenter = newZCenter;
                if (!discrete) {
                    currentZCenter = targetZCenter;
                }
                cumulativeMouseY = 0;
            }
            followingPlayer = false;
            return true;
        }
        return GuiEventListener.super.mouseDragged(pMouseX, pMouseY, pButton, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        if (targetZoomLevel > 20 && pDelta < 0) return false;

        float zl;
        if (MapAtlasesClientConfig.worldMapSmoothZooming.get()) {
            float c = (float) (pDelta);
            float v = -c / 10;
            targetZoomLevel = Math.max(startZoom, targetZoomLevel + v);
            zoomLevel = targetZoomLevel - 0.001f;
        } else {
            cumulativeZoomValue -= pDelta;
            cumulativeZoomValue = Math.max(cumulativeZoomValue, 0);
            zl = round((int) cumulativeZoomValue, ZOOM_BUCKET) / ZOOM_BUCKET;
            zl = Math.max(zl, 0);
            targetZoomLevel = startZoom + (2 * zl) + 1f;
        }

        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int pButton) {
        if (isHovered) {
            if (mapScreen.isPlacingPin()) {
                ColumnPos pos = getHoveredPos(mouseX, mouseY);
                mapScreen.placePinAt(pos);
                mapScreen.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            } else if (mapScreen.canTeleport()) {
                ColumnPos pos = getHoveredPos(mouseX, mouseY);
                MapAtlasesNetworking.sendToServer(new C2STeleportPacket(pos.x(), pos.z(),
                        mapScreen.getSelectedSlice().height(), mapScreen.getSelectedDimension()));
                return true;
            }
            return !mapScreen.isEditingText();
        }
        return false;
    }

    @NotNull
    private ColumnPos getHoveredPos(double mouseX, double mouseY) {
        double atlasMapsRelativeMouseX = Mth.map(
                mouseX, x, x + width, -1.0, 1.0);
        double atlasMapsRelativeMouseZ = Mth.map(
                mouseY, y, y + height, -1.0, 1.0);
        int hackOffset = +3;
        return new ColumnPos(
                (int) (Math.floor(atlasMapsRelativeMouseX * zoomLevel * (mapPixelSize / 2.0)) + currentXCenter) + hackOffset,
                (int) (Math.floor(atlasMapsRelativeMouseZ * zoomLevel * (mapPixelSize / 2.0)) + currentZCenter) + hackOffset);
    }

    @Override
    public void setFocused(boolean pFocused) {

    }

    @Override
    public boolean isFocused() {
        return true;
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput pNarrationElementOutput) {

    }

    public void resetAndCenter(int centerX, int centerZ, boolean followPlayer, boolean animation) {
        if (followPlayer) {
            centerX = Minecraft.getInstance().player.getBlockX();
            centerZ = Minecraft.getInstance().player.getBlockZ();
        }
        this.targetXCenter = centerX;
        this.targetZCenter = centerZ;
        if (!animation) {
            this.currentXCenter = centerX;
            this.currentZCenter = centerZ;
        }
        // Reset offset & zoom
        this.cumulativeMouseX = 0;
        this.cumulativeMouseY = 0;
        this.cumulativeZoomValue = 0;
        this.followingPlayer = followPlayer;
        resetZoom();
    }

    public void resetZoom() {
        this.targetZoomLevel = atlasesCount * mapScreen.getSelectedSlice().type().getDefaultZoomFactor();
    }

    public void tick() {
        float animationSpeed = 0.4f;
        if (animationProgress != 0) {
            animationProgress -= animationProgress * animationSpeed - 0.01;
            animationProgress = Math.max(0, animationProgress);
        }
        if (this.zoomLevel != targetZoomLevel) {
            zoomLevel = (float) interpolate(targetZoomLevel, zoomLevel, animationSpeed);
        }
        boolean test = true;
        if (this.currentXCenter != targetXCenter) {
            currentXCenter = interpolate(targetXCenter, currentXCenter, animationSpeed);
            test = false;
        }
        if (this.currentZCenter != targetZCenter) {
            currentZCenter = interpolate(targetZCenter, currentZCenter, animationSpeed);
            test = false;
        }

        //TODO:: better player snap
        //follow player
        if (followingPlayer) {

            var player = Minecraft.getInstance().player;
            targetXCenter = (int) player.getX();
            targetZCenter = (int) player.getZ();
        }
    }

    private double interpolate(double targetZCenter, double currentZCenter, double animationSpeed) {
        double diff = targetZCenter - currentZCenter;
        if (diff < 0) {
            return Math.max(targetZCenter, currentZCenter + (diff * animationSpeed) - 0.001);
        } else {
            return Math.min(targetZCenter, currentZCenter + (diff * animationSpeed) + 0.001);
        }
    }
}
