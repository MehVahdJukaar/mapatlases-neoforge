package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.NotNull;
import pepjebs.mapatlases.capabilities.MapCollectionCap;
import pepjebs.mapatlases.capabilities.MapKey;
import pepjebs.mapatlases.client.AbstractAtlasWidget;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.client.ui.MapAtlasesHUD;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.networking.C2SMarkerPacket;
import pepjebs.mapatlases.networking.C2STeleportPacket;
import pepjebs.mapatlases.networking.MapAtlasesNetowrking;

import static pepjebs.mapatlases.client.screen.DecorationBookmarkButton.MAP_ICON_TEXTURE;

public class MapWidget extends AbstractAtlasWidget implements GuiEventListener, NarratableEntry, Widget {


    private static final int PAN_BUCKET = 25;
    private static final int ZOOM_BUCKET = 2;

    private final AtlasOverviewScreen mapScreen;

    protected final int x;
    protected final int y;
    protected final int width;
    protected final int height;

    private float cumulativeZoomValue = ZOOM_BUCKET;
    private float cumulativeMouseX = 0;
    private float cumulativeMouseY = 0;

    protected int targetXCenter;
    protected int targetZCenter;
    protected float targetZoomLevel;

    private boolean isHovered;
    private float animationProgress = 0; //from zero to 1

    private float scaleAlpha = 0;

    public MapWidget(int x, int y, int width, int height, int atlasesCount,
                     AtlasOverviewScreen hack, MapItemSavedData originalCenterMap) {
        super(atlasesCount);
        initialize(originalCenterMap, hack.getSelectedSlice());
        this.targetZoomLevel = zoomLevel;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.mapScreen = hack;
        this.drawBigPlayerMarker = false;
    }

    @Override
    protected void applyScissors(PoseStack graphics, int x, int y, int x1, int y1) {
        var v = mapScreen.transformPos(x, y);
        var v2 = mapScreen.transformPos(x1, y1);
        super.applyScissors(graphics, (int) v.x(), (int) v.y(), (int) v2.x(), (int) v2.y());
    }

    @Override
    public void render(PoseStack poseStack, int pMouseX, int pMouseY, float pPartialTick) {

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;


        this.isHovered = isMouseOver(pMouseX, pMouseY);

        // Handle zooming markers hack
        MapAtlasesClient.setDecorationsScale(zoomLevel * (float) (double) MapAtlasesClientConfig.worldMapDecorationScale.get());

        this.drawAtlas(poseStack, x, y, width, height, player, zoomLevel,
                MapAtlasesClientConfig.worldMapBorder.get(), mapScreen.getSelectedSlice());

        MapAtlasesClient.setDecorationsScale(1);


        //TODO: fix
        mapScreen.updateVisibleDecoration((int) currentXCenter, (int) currentZCenter,
                zoomLevel / 2f * MAP_DIMENSION, followingPlayer);

        if (isHovered && mapScreen.placingPin) {
            poseStack.pushPose();
            poseStack.translate(pMouseX - 2.5f, pMouseY - 2.5f, 10);
            RenderSystem.setShaderTexture(0,MAP_ICON_TEXTURE);
           // blit(poseStack, 20, 0,
           //         40, 0, 8, 8, 128, 128);

            int textureW = 128;
            int textureH = 128;
            int width = 8;
            int height = 8;
            int x = 0;
            int y = 0;
            int x1 = x+width;
            int y1 = y+height;

            float u0 = 40f/textureW;
            float u1 = u0 + (float)(width) / (float)textureW;
            float centerU = (u0 + u1) / 2.0F;
            float v0 = 0f/textureH;
            float v1 =v0 + (float)(height) / (float)textureH;
            float centerV = (v0 + v1) / 2.0F;
            float shrink = 4f/textureW;
            float nu0 = Mth.lerp(shrink, u0, centerU);
            float nu1 = Mth.lerp(shrink, u1, centerU);
            float nv0 = Mth.lerp(shrink, v0, centerV);
            float nv1 = Mth.lerp(shrink, v1, centerV);

           var pMatrix = poseStack.last().pose();
            float pBlitOffset = 0;

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
            bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            bufferbuilder.vertex(pMatrix, (float)x, (float)y1, (float)pBlitOffset).uv(nu0, nv1).endVertex();
            bufferbuilder.vertex(pMatrix, (float)x1, (float)y1, (float)pBlitOffset).uv(nu1, nv1).endVertex();
            bufferbuilder.vertex(pMatrix, (float)x1, (float)y, (float)pBlitOffset).uv(nu1, nv0).endVertex();
            bufferbuilder.vertex(pMatrix, (float)x, (float)y, (float)pBlitOffset).uv(nu0, nv0).endVertex();
            BufferUploader.drawWithShader(bufferbuilder.end());
            poseStack.popPose();


        }
        if (this.isHovered) {
            this.renderPositionText(graphics, mc.font, pMouseX, pMouseY);

            if (Screen.hasShiftDown() && mapScreen.getMinecraft().gameMode.getPlayerMode().isCreative()) {
                mapScreen.renderTooltip(poseStack,
                        Component.translatable("chat.coordinates.tooltip")
                                .withStyle(ChatFormatting.GREEN),
                        pMouseX, pMouseY);
            }
        }

        boolean animation = zoomLevel != targetZoomLevel;
        if (animation || scaleAlpha != 0) {
            if (animation) scaleAlpha = 1;
            else {
                scaleAlpha = Math.max(0, scaleAlpha - 0.07f);
            }
            int a = (int) (scaleAlpha * 255);
            if(a != 0) {
                PoseStack poseStack = graphics.pose();
                poseStack.pushPose();
                poseStack.translate(0,0,4);
                graphics.drawString(mc.font,
                        Component.translatable("message.map_atlases.map_scale", (int) targetZoomLevel),
                        x, y + height - 8, FastColor.ABGR32.color(a, 255, 255, 255));
                poseStack.popPose();

            }
        }
    }

    @Override
    public Pair<Integer, MapItemSavedData> getMapWithCenter(int centerX, int centerZ) {
        return mapScreen.findMapEntryForCenter(centerX, centerZ);
    }

    private void renderPositionText(PoseStack graphics, Font font, int mouseX, int mouseY) {
        // Draw world map coords
        if (!MapAtlasesClientConfig.drawWorldMapCoords.get()) return;
        BlockPos pos = getHoveredPos(mouseX, mouseY);
        float textScaling = (float) (double) MapAtlasesClientConfig.worldMapCoordsScale.get();
        //TODO: fix coordinate being slightly offset
        //idk why
        String coordsToDisplay = "X: " + pos.getX() + ", Z: " + pos.getZ();
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
            float hack = (zoomLevel / atlasesCount);
            //TODO: fix pan
            cumulativeMouseX += deltaX * hack;
            cumulativeMouseY += deltaY * hack;
            int newXCenter;
            int newZCenter;
            boolean discrete = !MapAtlasesClientConfig.worldMapSmoothPanning.get();
            if (discrete) {
                //discrete mode
                newXCenter = (int) (currentXCenter - (round((int) cumulativeMouseX, PAN_BUCKET) / PAN_BUCKET * mapPixelSize));
                newZCenter = (int) (currentZCenter - (round((int) cumulativeMouseY, PAN_BUCKET) / PAN_BUCKET * mapPixelSize));
            } else {
                newXCenter = (int) (currentXCenter - cumulativeMouseX * mapPixelSize / PAN_BUCKET);
                newZCenter = (int) (currentZCenter - cumulativeMouseY * mapPixelSize / PAN_BUCKET);
            }
            if (newXCenter != currentXCenter || newZCenter != currentZCenter) {
                if (!discrete) {
                    currentXCenter = newXCenter;
                    currentZCenter = newZCenter;
                }
                targetXCenter = newXCenter;
                targetZCenter = newZCenter;
                cumulativeMouseX = 0;
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

        cumulativeZoomValue -= pDelta;
        cumulativeZoomValue = Math.max(cumulativeZoomValue, -1 * ZOOM_BUCKET);

        int zl = round((int) cumulativeZoomValue, ZOOM_BUCKET) / ZOOM_BUCKET;
        zl = Math.max(zl, 0);
        targetZoomLevel = (2 * zl) + 1f;
        return true;
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int pButton) {
        if (isHovered && Screen.hasShiftDown() && Minecraft.getInstance().gameMode.getPlayerMode().isCreative()) {
            BlockPos pos = getHoveredPos(mouseX, mouseY);
            MapAtlasesNetowrking.sendToServer(new C2STeleportPacket(pos.getX(), pos.getZ(),
                    mapScreen.getSelectedSlice().height(), mapScreen.getSelectedDimension()));
        }


        if (mapScreen.placingPin) {
            BlockPos pos = getHoveredPos(mouseX, mouseY);
            MapCollectionCap maps = MapAtlasItem.getMaps(mapScreen.getAtlas(), mapScreen.getMinecraft().level);
            MapKey key = MapKey.at(maps.getScale(), pos.getX(), pos.getZ(), mapScreen.getSelectedDimension(), mapScreen.getSelectedSlice());
            var m = maps.select(key);
            if (m != null) {
                MapAtlasesNetowrking.sendToServer(new C2SMarkerPacket(pos, m.getFirst()));
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                mapScreen.placingPin = false;
            }
        }

        return isHovered;
    }

    @NotNull
    private BlockPos getHoveredPos(double mouseX, double mouseY) {
        double atlasMapsRelativeMouseX = Mth.map(
                mouseX, x, x + width, -1.0, 1.0);
        double atlasMapsRelativeMouseZ = Mth.map(
                mouseY, y, y + height, -1.0, 1.0);
        int hackOffset = +3;
        return new BlockPos(
                (int) (Math.floor(atlasMapsRelativeMouseX * zoomLevel * (mapPixelSize / 2.0)) + currentXCenter) + hackOffset,
                0,
                (int) (Math.floor(atlasMapsRelativeMouseZ * zoomLevel * (mapPixelSize / 2.0)) + currentZCenter) + hackOffset);
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput pNarrationElementOutput) {

    }

    public void resetAndCenter(int centerX, int centerZ, boolean followPlayer, boolean animation) {
        this.targetXCenter = centerX;
        this.targetZCenter = centerZ;
        if (!animation) {
            this.currentXCenter = centerX;
            this.currentZCenter = centerZ;
        }
        // Reset offset & zoom
        this.cumulativeMouseX = 0;
        this.cumulativeMouseY = 0;
        this.cumulativeZoomValue = ZOOM_BUCKET;
        this.followingPlayer = followPlayer;
        resetZoom();
    }

    public void resetZoom() {
        this.targetZoomLevel = atlasesCount * mapScreen.getSelectedSlice().getDefaultZoomFactor();
    }

    public void tick() {
        float animationSpeed = 0.4f;
        if (animationProgress != 0) {
            animationProgress -= animationProgress * animationSpeed - 0.01;
            animationProgress = Math.max(0, animationProgress);
        }
        if (this.zoomLevel != targetZoomLevel) {
            zoomLevel = interpolate(targetZoomLevel, zoomLevel, animationSpeed);
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

    private float interpolate(float targetZCenter, float currentZCenter, float animationSpeed) {
        float diff = targetZCenter - currentZCenter;
        if (diff < 0) {
            return Math.max(targetZCenter, currentZCenter + (diff * animationSpeed) - 0.001f);
        } else {
            return Math.min(targetZCenter, currentZCenter + (diff * animationSpeed) + 0.001f);
        }
    }
}
