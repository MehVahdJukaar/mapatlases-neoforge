package pepjebs.mapatlases.client.ui;

import net.mehvahdjukaar.moonlight.api.util.TextHelper;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.AbstractAtlasDisplay;
import pepjebs.mapatlases.client.Anchoring;
import pepjebs.mapatlases.client.AtlasGuiUtils;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.integration.moonlight.ClientMarkersRenderer;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.MapCollection;
import pepjebs.mapatlases.map_collection.MapGridKey;
import pepjebs.mapatlases.utils.AtlasMap;
import pepjebs.mapatlases.utils.Slice;

import java.util.Objects;

import static pepjebs.mapatlases.client.MapAtlasesClient.MAP_HUD_BACKGROUND_TEXTURE;

public class MapAtlasesHUD extends AbstractAtlasDisplay {

    private static final int BG_SIZE = 64;

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

    @Override
    protected boolean showMapBorders() {
        return MapAtlasesClientConfig.miniMapBorder.get();
    }

    @Nullable
    @Override
    public AtlasMap getMapWithCenter(int centerX, int centerZ) {
        Slice slice = currentMapKey.slice;
        return currentMaps.getMapAt(centerX, centerZ, slice);
    }

    @Override
    protected void initialize(AtlasMap originalCenterMap) {
        super.initialize(originalCenterMap);
        this.followingPlayer = MapAtlasesClientConfig.miniMapFollowPlayer.get();
        this.rotatesWithPlayer = MapAtlasesClientConfig.miniMapRotate.get();
        this.globalScale = (float) (double) MapAtlasesClientConfig.miniMapScale.get();
        this.currentMaps = MapAtlasItem.getMaps(currentAtlas, mc.level);
        this.displaysY = !MapAtlasesClientConfig.yOnlyWithSlice.get() || currentMaps.hasAnySlicedMap();
        this.drawBigPlayerMarker = followingPlayer;
    }

    public void render(GuiGraphicsExtractor graphics, DeltaTracker partialTick) {
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        if (mc.level == null || mc.options.hideGui || mc.player == null) return;

        if (!MapAtlasesClientConfig.drawMiniMapHUD.get()) return;
        if (MapAtlasesClientConfig.hideWhenDebugScreenOpen.get() && mc.getDebugOverlay().showDebugScreen()) return;
        if (MapAtlasesClientConfig.hideWhenInventoryOpen.get() && mc.screen != null) return;

        ItemStack atlas = MapAtlasesClient.getCurrentActiveAtlas();
        if (atlas.isEmpty()) return;

        AtlasMap activeMap = MapAtlasesClient.getActiveMap();
        currentMapKey = MapAtlasesClient.getActiveMapKey();
        if (activeMap == null || currentMapKey == null) return;

        if (MapAtlasesClientConfig.hideWhenInHand.get()
                && mc.player.isHolding(MapAtlasesMod.MAP_ATLAS.get())) return;

        if (currentAtlas != atlas) needsInit = true;
        currentAtlas = atlas;

        if (needsInit) {
            needsInit = false;
            initialize(activeMap);
        }
        mapWherePlayerIs = activeMap;

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.scale(globalScale);

        playMapChangeSoundIfNeeded();

        Anchoring anchorLocation = MapAtlasesClientConfig.miniMapAnchoring.get();
        int[] pos = computeMapPosition(screenWidth, screenHeight, anchorLocation);
        int x = pos[0];
        int y = pos[1];
        int mapWidgetSize = (int) (BG_SIZE * (116 / 128f));
        int borderSize = (BG_SIZE - mapWidgetSize) / 2;

        updatePlayerCenter();

        int tint = MapAtlasesClientConfig.minimapSkyLight.get() ? skyLightTint() : -1;

        graphics.blit(RenderPipelines.GUI_TEXTURED, MAP_HUD_BACKGROUND_TEXTURE, x, y, 0, 0, BG_SIZE, BG_SIZE, BG_SIZE, BG_SIZE);

        float yRot = mc.player.getYRot();
        renderMapContent(graphics, x, y, mapWidgetSize, borderSize, yRot, tint);

        renderPlayerMarker(graphics, x, y, mapWidgetSize, yRot);
        renderText(graphics, x, y, anchorLocation);
        renderCardinals(graphics, x, y, yRot);
        renderPinTracking(graphics, x, y);

        pose.popMatrix();
    }

    private int skyLightTint() {
        int sky = mc.level.getBrightness(LightLayer.SKY, mc.player.getOnPos().above());
        int brightness = (int) (255 * Mth.lerp(sky / 15f, 0.25f, 1));
        return ARGB.color(255, brightness, brightness, brightness);
    }

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
            ColumnPos center = currentMapKey.slice.type().getCenter(player.getX(), player.getZ(), mapBlocksSize);
            currentXCenter = center.x();
            currentZCenter = center.z();
        } else {
            currentXCenter = player.getX();
            currentZCenter = player.getZ();
        }
    }

    private void renderMapContent(GuiGraphicsExtractor graphics, int x, int y, int mapWidgetSize, int borderSize,
                                  float yRot, int tint) {
        this.decorationScale = (float) (2 * zoomLevel * MapAtlasesClientConfig.miniMapDecorationScale.get());
        this.decorationTextScale = (float) (2 * zoomLevel * MapAtlasesClientConfig.miniMapDecorationTextScale.get());
        this.decorationRotation = rotatesWithPlayer ? yRot - 180 : 0;

        drawAtlas(graphics, x + borderSize, y + borderSize, mapWidgetSize, mapWidgetSize,
                zoomLevel * (float) (double) MapAtlasesClientConfig.miniMapZoomMultiplier.get(),
                currentMapKey.slice.type(), tint, null);
    }

    private void renderPlayerMarker(GuiGraphicsExtractor graphics, int x, int y, int mapWidgetSize, float yRot) {
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x + mapWidgetSize / 2f + 3f, y + mapWidgetSize / 2f + 3);
        if (!rotatesWithPlayer) {
            pose.rotate(-(180 - yRot) * Mth.DEG_TO_RAD);
        }
        if (drawBigPlayerMarker) {
            pose.translate(-4.5f, -4f);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, MapAtlasesClient.PLAYER_MARKER_SPRITE, 0, 0, 8, 8);
        }
        pose.popMatrix();
    }

    private void renderText(GuiGraphicsExtractor graphics, int x, int y, Anchoring anchorLocation) {
        float textScaling = (float) (double) MapAtlasesClientConfig.minimapCoordsAndBiomeScale.get();
        int textHeightOffset = 2;
        int actualBgSize = (int) (BG_SIZE * globalScale);
        Font font = mc.font;

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        if (!anchorLocation.isUp) pose.translate(0, -BG_SIZE - 20 * textScaling - 2);

        boolean global = MapAtlasesClientConfig.drawMinimapCoords.get();
        boolean local = MapAtlasesClientConfig.drawMinimapChunkCoords.get();
        if (global || local) {
            BlockPos pos = new BlockPos((int) mc.player.getX(), (int) mc.player.getY(), (int) mc.player.getZ());
            if (global) {
                drawCoords(graphics, font, x, (int) (y + BG_SIZE + (textHeightOffset / globalScale)),
                        actualBgSize, textScaling, pos);
                textHeightOffset += (int) (10 * textScaling);
            }
            if (local) {
                drawChunkCoords(graphics, font, x, (int) (y + BG_SIZE + (textHeightOffset / globalScale)),
                        actualBgSize, textScaling, pos);
                textHeightOffset += (int) (10 * textScaling);
            }
        }
        if (MapAtlasesClientConfig.drawMinimapBiome.get()) {
            drawBiome(graphics, font, x, (int) (y + BG_SIZE + (textHeightOffset / globalScale)),
                    actualBgSize, textScaling, mc.player.blockPosition(), mc.level);
        }
        pose.popMatrix();
    }

    private void renderCardinals(GuiGraphicsExtractor graphics, int x, int y, float yRot) {
        if (!MapAtlasesClientConfig.drawMinimapCardinals.get()) return;
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x + BG_SIZE / 2f, y + BG_SIZE / 2f);
        var p = AtlasGuiUtils.getDirectionPos(BG_SIZE / 2f - 3, rotatesWithPlayer ? yRot : 180);
        float a = p.getFirst();
        float b = p.getSecond();
        Font font = mc.font;
        drawLetter(graphics, font, a, b, "N");
        if (!MapAtlasesClientConfig.miniMapOnlyNorth.get()) {
            drawLetter(graphics, font, -a, -b, "S");
            drawLetter(graphics, font, -b, a, "E");
            drawLetter(graphics, font, b, -a, "W");
        }
        pose.popMatrix();
    }

    private void renderPinTracking(GuiGraphicsExtractor graphics, int x, int y) {
        if (!MapAtlasesClientConfig.pinTracking.get()) return;
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x + BG_SIZE / 2f, y + BG_SIZE / 2f);
        ClientMarkersRenderer.drawSmallPins(graphics, mc.font, currentXCenter + mapBlocksSize / 2f,
                currentZCenter + mapBlocksSize / 2f, currentMapKey.slice,
                mapBlocksSize * zoomLevel, mc.player, rotatesWithPlayer, currentMaps);
        pose.popMatrix();
    }


    private void drawLetter(GuiGraphicsExtractor graphics, Font font, float a, float b, String letter) {
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        float scale = (float) (double) MapAtlasesClientConfig.miniMapCardinalsScale.get() / globalScale;
        pose.scale(scale);
        AtlasGuiUtils.drawStringWithLighterShadow(graphics, font, letter,
                a / scale - font.width(letter) / 2f, b / scale - font.lineHeight / 2f);
        pose.popMatrix();
    }

    private void drawCoords(GuiGraphicsExtractor context, Font font, int x, int y,
                            int targetWidth, float textScaling, BlockPos pos) {
        String coordsToDisplay = displaysY
                ? Component.translatable("message.map_atlases.coordinates_full",
                        pos.getX(), pos.getY(), pos.getZ()).getString()
                : Component.translatable("message.map_atlases.coordinates",
                        pos.getX(), pos.getZ()).getString();
        AtlasGuiUtils.drawScaledText(context, font, x, y, coordsToDisplay,
                textScaling / globalScale, targetWidth, (int) (targetWidth / globalScale));
    }

    private void drawChunkCoords(GuiGraphicsExtractor context, Font font, int x, int y,
                                 int targetWidth, float textScaling, BlockPos pos) {
        String coordsToDisplay = Component.translatable("message.map_atlases.chunk_coordinates",
                SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()),
                SectionPos.sectionRelative(pos.getX()), SectionPos.sectionRelative(pos.getZ())).getString();
        AtlasGuiUtils.drawScaledText(context, font, x, y, coordsToDisplay,
                textScaling / globalScale, targetWidth, (int) (targetWidth / globalScale));
    }

    private void drawBiome(GuiGraphicsExtractor context, Font font, int x, int y,
                           int targetWidth, float textScaling, BlockPos blockPos, Level level) {
        String biomeToDisplay = "";
        var key = level.getBiome(blockPos).unwrapKey();
        if (key.isPresent()) {
            ResourceKey<Biome> biomeKey = key.get();
            // datapack biomes often ship no lang
            biomeToDisplay = Component.translatableWithFallback(Util.makeDescriptionId("biome", biomeKey.identifier()),
                    TextHelper.getReadableName(biomeKey.identifier().getPath())).getString();
        }
        AtlasGuiUtils.drawScaledText(context, font, x, y, biomeToDisplay,
                textScaling / globalScale, targetWidth, (int) (targetWidth / globalScale));
    }


    public void increaseZoom() {
        zoomLevel = Math.max(1, zoomLevel - 0.5f);
    }

    public void decreaseZoom() {
        zoomLevel = Math.min(10, zoomLevel + 0.5f);
    }
}