package pepjebs.mapatlases.client.ui;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.PlatStuff;
import pepjebs.mapatlases.client.AbstractAtlasWidget;
import pepjebs.mapatlases.client.Anchoring;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.client.screen.AtlasScreenUtils;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.integration.ImmediatelyFastCompat;
import pepjebs.mapatlases.integration.moonlight.ClientMarkersRenderer;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.map_collection.MapGridKey;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.Slice;

import java.util.Objects;

import static pepjebs.mapatlases.client.MapAtlasesClient.MAP_HUD_BACKGROUND_TEXTURE;

public class MapAtlasesHUD extends AbstractAtlasWidget {

    private static final int BACKGROUND_SIZE = 128;
    protected final int BG_SIZE = 64;

    private final Minecraft mc;

    private boolean needsInit = true;
    private ItemStack currentAtlas = ItemStack.EMPTY;
    private MapGridKey currentMapKey = null;
    private MapGridKey lastMapKey = null;
    private MapCollection currentMaps;

    private float globalScale = 1;
    private boolean displaysY = true;

    public MapAtlasesHUD() {
        super(1);
        this.mc = Minecraft.getInstance();
        this.rotatesWithPlayer = true;
        this.zoomLevel = 1;
    }

    @Override
    protected boolean showMapBackground() {
        return false;
    }

    @Nullable
    @Override
    public MapDataHolder getMapWithCenter(int centerX, int centerZ) {
        Slice slice = currentMapKey.slice;
        return currentMaps.select(centerX, centerZ, slice);
    }

    @Override
    protected void initialize(MapDataHolder originalCenterMap) {
        super.initialize(originalCenterMap);
        this.followingPlayer = MapAtlasesClientConfig.miniMapFollowPlayer.get();
        this.rotatesWithPlayer = MapAtlasesClientConfig.miniMapRotate.get();
        this.globalScale = (float) (double) MapAtlasesClientConfig.miniMapScale.get();
        this.currentMaps = MapAtlasItem.getMaps(currentAtlas, mc.level);
        this.displaysY = !MapAtlasesClientConfig.yOnlyWithSlice.get() || currentMaps.hasOneSlicedMap();
        this.drawBigPlayerMarker = followingPlayer;
    }

    @Override
    protected void applyScissors(GuiGraphics graphics, int x, int y, int x1, int y1) {
        super.applyScissors(graphics,
                (int) (x * globalScale), (int) (y * globalScale),
                (int) (x1 * globalScale), (int) (y1 * globalScale));
    }

    // ── Main render entry ─────────────────────────────────────────────────

    public void render(GuiGraphics graphics, DeltaTracker partialTick) {
        Window window = Minecraft.getInstance().getWindow();
        int screenWidth = window.getGuiScaledWidth();
        int screenHeight = window.getGuiScaledHeight();

        if (mc.level == null ||  mc.options.hideGui || mc.player == null || mc.getDebugOverlay().showDebugScreen()) return;

        if (!MapAtlasesClientConfig.drawMiniMapHUD.get()) return;
        if (MapAtlasesClientConfig.hideWhenInventoryOpen.get() && mc.screen != null) return;

        ItemStack atlas = MapAtlasesClient.getCurrentActiveAtlas();
        if (atlas.isEmpty()) return;

        MapDataHolder activeMap = MapAtlasesClient.getActiveMap();
        currentMapKey = MapAtlasesClient.getActiveMapKey();
        if (activeMap == null || currentMapKey == null) return;

        if (MapAtlasesClientConfig.hideWhenInHand.get()
                && (mc.player.getMainHandItem().is(MapAtlasesMod.MAP_ATLAS.get())
                || mc.player.getOffhandItem().is(MapAtlasesMod.MAP_ATLAS.get()))) return;

        if (currentAtlas != atlas) needsInit = true;
        currentAtlas = atlas;

        if (needsInit) {
            needsInit = false;
            initialize(activeMap);
        }
        mapWherePlayerIs = activeMap;

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.scale(globalScale, globalScale, 1);

        playMapChangeSoundIfNeeded();

        Anchoring anchorLocation = MapAtlasesClientConfig.miniMapAnchoring.get();
        int[] pos = computeMapPosition(screenWidth, screenHeight, anchorLocation);
        int x = pos[0];
        int y = pos[1];
        int mapWidgetSize = (int) (BG_SIZE * (116 / 128f));
        int borderSize = (BG_SIZE - mapWidgetSize) / 2;

        updatePlayerCenter();

        int light = !MapAtlasesClientConfig.minimapSkyLight.get() ? LightTexture.FULL_BRIGHT :
                LightTexture.pack(0, mc.level.getBrightness(LightLayer.SKY, mc.player.getOnPos().above()));

        RenderSystem.enableDepthTest();
        graphics.blit(MAP_HUD_BACKGROUND_TEXTURE, x, y, -2, 0, 0, BG_SIZE, BG_SIZE, BG_SIZE, BG_SIZE);
        RenderSystem.disableDepthTest();

        float yRot = mc.player.getYRot();
        renderMapContent(graphics, x, y, mapWidgetSize, borderSize, yRot, light);

        if (MapAtlasesMod.IMMEDIATELY_FAST) ImmediatelyFastCompat.startBatching();

        MapAtlasesClient.setDecorationsScale(1);
        MapAtlasesClient.setDecorationsTextScale(1);
        if (rotatesWithPlayer) MapAtlasesClient.setDecorationRotation(0);

        renderPlayerMarker(graphics, x, y, mapWidgetSize, yRot);
        renderText(graphics, x, y, anchorLocation);
        renderCardinals(graphics, x, y, yRot);
        renderPinTracking(graphics, x, y);

        poseStack.popPose();  // closes globalScale push

        if (MapAtlasesMod.IMMEDIATELY_FAST) ImmediatelyFastCompat.endBatching();
    }

    // ── Render sub-steps ──────────────────────────────────────────────────

    private void playMapChangeSoundIfNeeded() {
        if (!Objects.equals(lastMapKey, currentMapKey)) {
            lastMapKey = currentMapKey;
            if (mc.screen == null && MapAtlasesClientConfig.mapChangeSound.get()) {
                mc.player.playSound(MapAtlasesMod.ATLAS_PAGE_TURN_SOUND_EVENT.get(),
                        (float) (double) MapAtlasesClientConfig.soundScalar.get(), 1.0F);
            }
        }
    }

    private int[] computeMapPosition(int screenWidth, int screenHeight, Anchoring anchorLocation) {
        int off = 5;
        int x = anchorLocation.isLeft ? off : (int) (screenWidth / globalScale) - (BG_SIZE + off);
        int y = anchorLocation.isUp ? off : (int) (screenHeight / globalScale) - (BG_SIZE + off);
        x += (int) (MapAtlasesClientConfig.miniMapHorizontalOffset.get() / globalScale);
        y += (int) (MapAtlasesClientConfig.miniMapVerticalOffset.get() / globalScale);

        if (anchorLocation == Anchoring.UPPER_RIGHT) {
            boolean hasBeneficial = false;
            boolean hasNegative = false;
            for (var e : mc.player.getActiveEffects()) {
                Holder<MobEffect> effect = e.getEffect();
                if (effect.value().isBeneficial()) hasBeneficial = true;
                else hasNegative = true;
            }
            int offsetForEffects = MapAtlasesClientConfig.activePotionVerticalOffset.get();
            if (hasNegative && y < 2 * offsetForEffects) y += (2 * offsetForEffects - y);
            else if (hasBeneficial && y < offsetForEffects) y += (offsetForEffects - y);
        }
        return new int[]{x, y};
    }

    private void updatePlayerCenter() {
        LocalPlayer player = mc.player;
        if (!followingPlayer) {
            currentXCenter = Math.floor((player.getX() + 64) / mapBlocksSize) * mapBlocksSize + (mapBlocksSize / 2f - 64);
            currentZCenter = Math.floor((player.getZ() + 64) / mapBlocksSize) * mapBlocksSize + (mapBlocksSize / 2f - 64);
        } else {
            currentXCenter = player.getX();
            currentZCenter = player.getZ();
        }
    }

    private void renderMapContent(GuiGraphics graphics, int x, int y, int mapWidgetSize, int borderSize,
                                  float yRot, int light) {
        MapAtlasesClient.setDecorationsScale((float) (2 * zoomLevel * MapAtlasesClientConfig.miniMapDecorationScale.get()));
        MapAtlasesClient.setDecorationsTextScale((float) (2 * zoomLevel * MapAtlasesClientConfig.miniMapDecorationTextScale.get()));
        if (rotatesWithPlayer) MapAtlasesClient.setDecorationRotation(yRot - 180);

        graphics.pose().pushPose();
        drawAtlas(graphics, x + borderSize, y + borderSize,
                mapWidgetSize, mapWidgetSize, mc.player,
                zoomLevel * (float) (double) MapAtlasesClientConfig.miniMapZoomMultiplier.get(),
                MapAtlasesClientConfig.miniMapBorder.get(), currentMapKey.slice.type(), light, null);
        graphics.pose().popPose();
    }

    private void renderPlayerMarker(GuiGraphics graphics, int x, int y, int mapWidgetSize, float yRot) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(x + mapWidgetSize / 2f + 3f, y + mapWidgetSize / 2f + 3, 0);
        if (!rotatesWithPlayer) {
            poseStack.mulPose(Axis.ZN.rotationDegrees(180 - yRot));
        }
        if (drawBigPlayerMarker) {
            poseStack.translate(-4.5f, -4f, 0);
            graphics.blitSprite(MapAtlasesClient.PLAYER_MARKER_SPRITE, 0, 0, 8, 8);
        }
        poseStack.popPose();
    }

    private void renderText(GuiGraphics graphics, int x, int y, Anchoring anchorLocation) {
        float textScaling = (float) (double) MapAtlasesClientConfig.minimapCoordsAndBiomeScale.get();
        int textHeightOffset = 2;
        int actualBgSize = (int) (BG_SIZE * globalScale);
        Font font = mc.font;

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        if (!anchorLocation.isUp) poseStack.translate(0, -BG_SIZE - 20 * textScaling - 2, 0);

        boolean global = MapAtlasesClientConfig.drawMinimapCoords.get();
        boolean local = MapAtlasesClientConfig.drawMinimapChunkCoords.get();
        if (global || local) {
            BlockPos pos = new BlockPos(new Vec3i(
                    AtlasScreenUtils.towardsZero(mc.player.position().x),
                    AtlasScreenUtils.towardsZero(mc.player.position().y),
                    AtlasScreenUtils.towardsZero(mc.player.position().z)));
            if (global) {
                drawMapComponentCoords(graphics, font, x, (int) (y + BG_SIZE + (textHeightOffset / globalScale)),
                        actualBgSize, textScaling, pos, false);
                textHeightOffset += (int) (10 * textScaling);
            }
            if (local) {
                drawMapComponentCoords(graphics, font, x, (int) (y + BG_SIZE + (textHeightOffset / globalScale)),
                        actualBgSize, textScaling, pos, true);
                textHeightOffset += (int) (10 * textScaling);
            }
        }
        if (MapAtlasesClientConfig.drawMinimapBiome.get()) {
            drawMapComponentBiome(graphics, font, x, (int) (y + BG_SIZE + (textHeightOffset / globalScale)),
                    actualBgSize, textScaling, mc.player.blockPosition(), mc.level);
        }
        poseStack.popPose();
    }

    private void renderCardinals(GuiGraphics graphics, int x, int y, float yRot) {
        if (!MapAtlasesClientConfig.drawMinimapCardinals.get()) return;
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(x + BG_SIZE / 2f, y + BG_SIZE / 2f, 5);
        var p = AtlasScreenUtils.getDirectionPos(BG_SIZE / 2f - 3, rotatesWithPlayer ? yRot : 180);
        float a = p.getFirst();
        float b = p.getSecond();
        Font font = mc.font;
        drawLetter(graphics, font, a, b, "N");
        if (!MapAtlasesClientConfig.miniMapOnlyNorth.get()) {
            drawLetter(graphics, font, -a, -b, "S");
            drawLetter(graphics, font, -b, a, "E");
            drawLetter(graphics, font, b, -a, "W");
        }
        poseStack.popPose();
    }

    private void renderPinTracking(GuiGraphics graphics, int x, int y) {
        if (!MapAtlasesMod.MOONLIGHT || !MapAtlasesClientConfig.moonlightPinTracking.get()) return;
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        RenderSystem.enableDepthTest();
        poseStack.translate(x + BG_SIZE / 2f, y + BG_SIZE / 2f, 10);
        ClientMarkersRenderer.drawSmallPins(graphics, mc.font, currentXCenter + mapBlocksSize / 2f,
                currentZCenter + mapBlocksSize / 2f, currentMapKey.slice,
                mapBlocksSize * zoomLevel, mc.player, rotatesWithPlayer, currentMaps);
        RenderSystem.disableDepthTest();
        poseStack.popPose();
    }

    // ── Text drawing helpers ──────────────────────────────────────────────

    private void drawLetter(GuiGraphics graphics, Font font, float a, float b, String letter) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        float scale = (float) (double) MapAtlasesClientConfig.miniMapCardinalsScale.get() / globalScale;
        pose.scale(scale, scale, 1);
        AtlasScreenUtils.drawStringWithLighterShadow(graphics, font, letter,
                a / scale - font.width(letter) / 2f, b / scale - font.lineHeight / 2f);
        pose.popPose();
    }

    public void drawMapComponentCoords(GuiGraphics context, Font font, int x, int y,
                                       int targetWidth, float textScaling, BlockPos pos, boolean chunk) {
        String coordsToDisplay;
        if (chunk) {
            coordsToDisplay = Component.translatable("message.map_atlases.chunk_coordinates",
                    pos.getX() / 16, pos.getZ() / 16, pos.getX() % 16, pos.getZ() % 16).getString();
        } else {
            coordsToDisplay = displaysY
                    ? Component.translatable("message.map_atlases.coordinates_full",
                            pos.getX(), pos.getY(), pos.getZ()).getString()
                    : Component.translatable("message.map_atlases.coordinates",
                            pos.getX(), pos.getZ()).getString();
        }
        AtlasScreenUtils.drawScaledComponent(context, font, x, y, coordsToDisplay,
                textScaling / globalScale, targetWidth, (int) (targetWidth / globalScale));
    }

    public void drawMapComponentBiome(GuiGraphics context, Font font, int x, int y,
                                      int targetWidth, float textScaling, BlockPos blockPos, Level level) {
        String biomeToDisplay = "";
        var key = level.getBiome(blockPos).unwrapKey();
        if (key.isPresent()) {
            ResourceKey<Biome> biomeKey = key.get();
            biomeToDisplay = Component.translatable(Util.makeDescriptionId("biome", biomeKey.location())).getString();
        }
        AtlasScreenUtils.drawScaledComponent(context, font, x, y, biomeToDisplay,
                textScaling / globalScale, targetWidth, (int) (targetWidth / globalScale));
    }

    // ── Zoom ─────────────────────────────────────────────────────────────

    public void increaseZoom() {
        zoomLevel = Math.max(1, zoomLevel - 0.5f);
    }

    public void decreaseZoom() {
        zoomLevel = Math.min(10, zoomLevel + 0.5f);
    }
}