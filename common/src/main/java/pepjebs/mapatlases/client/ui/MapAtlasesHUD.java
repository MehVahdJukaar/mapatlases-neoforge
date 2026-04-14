package pepjebs.mapatlases.client.ui;

import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.PlatStuff;
import pepjebs.mapatlases.client.AbstractAtlasWidget;
import pepjebs.mapatlases.client.Anchoring;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.integration.moonlight.ClientMarkersRenderer;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.IMapCollection;
import pepjebs.mapatlases.map_collection.MapKey;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.Slice;

import java.util.Objects;

import static pepjebs.mapatlases.client.MapAtlasesClient.MAP_HUD_BACKGROUND_TEXTURE;
import static pepjebs.mapatlases.client.MapAtlasesClient.MAP_HUD_FOREGROUND_TEXTURE;
import static pepjebs.mapatlases.client.MapAtlasesClient.MAP_PLAYER_DECORATION_TEXTURE;

public class MapAtlasesHUD extends AbstractAtlasWidget implements HudElement {

    private static final int BG_SIZE = 64;
    /** Pixels the minimap is pushed down to make room for the biome label above it. */
    private static final int BIOME_ABOVE_OFFSET = 12;

    private final Minecraft mc;

    private boolean needsInit = true;
    private boolean lastHadCompass = false;
    private ItemStack currentAtlas = ItemStack.EMPTY;
    private MapKey currentMapKey;
    private MapKey lastMapKey;
    private IMapCollection currentMaps;

    private float globalScale = 1;
    private boolean displaysY = true;

    public MapAtlasesHUD() {
        super(1);
        this.mc = Minecraft.getInstance();
        this.rotatesWithPlayer = false;
        this.zoomLevel = 1;
    }

    @Nullable
    @Override
    public MapDataHolder getMapWithCenter(int centerX, int centerZ) {
        Slice slice = currentMapKey.slice();
        MapDataHolder exact = currentMaps.select(centerX, centerZ, slice);
        if (exact != null) {
            return exact;
        }
        MapDataHolder closest = currentMaps.getClosest(centerX, centerZ, slice);
        if (closest == null) {
            return null;
        }
        int maxOffset = mapBlocksSize / 2;
        if (Math.abs(closest.data.centerX - centerX) <= maxOffset &&
                Math.abs(closest.data.centerZ - centerZ) <= maxOffset) {
            return closest;
        }
        return null;
    }

    @Override
    protected boolean shouldClipDecorations() {
        return true;
    }

    @Override
    protected void initialize(MapDataHolder originalCenterMap) {
        super.initialize(originalCenterMap);
        this.followingPlayer = MapAtlasesClientConfig.miniMapFollowPlayer.get();
        this.rotatesWithPlayer = hasCompass(mc.player);
        this.globalScale = (float) (double) MapAtlasesClientConfig.miniMapScale.get();
        this.currentMaps = MapAtlasItem.getMaps(currentAtlas, mc.level);
        this.displaysY = !MapAtlasesClientConfig.yOnlyWithSlice.get() || currentMaps.hasOneSlice();
        this.drawBigPlayerMarker = followingPlayer;
        this.drawMapDecorationsFallback = true;
    }

    @Override
    protected void applyScissors(GuiGraphicsExtractor graphics, int x, int y, int x1, int y1) {
        super.applyScissors(graphics, x, y, x1, y1);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        this.render(graphics, screenWidth, screenHeight);
    }

    public void render(GuiGraphicsExtractor graphics, int screenWidth, int screenHeight) {
        if (mc.level == null || mc.player == null || mc.getDebugOverlay().showDebugScreen()) {
            return;
        }
        if (!MapAtlasesClientConfig.drawMiniMapHUD.get()) {
            return;
        }
        if (MapAtlasesClientConfig.hideWhenInventoryOpen.get() && mc.screen != null) {
            return;
        }

        ItemStack atlas = MapAtlasesClient.getCurrentActiveAtlas();
        if (atlas.isEmpty()) {
            return;
        }

        MapDataHolder activeMap = MapAtlasesClient.getActiveMap();
        currentMapKey = MapAtlasesClient.getActiveMapKey();
        if (activeMap == null || currentMapKey == null) {
            return;
        }

        if (MapAtlasesClientConfig.hideWhenInHand.get() &&
                (mc.player.getMainHandItem().is(MapAtlasesMod.MAP_ATLAS.get()) ||
                        mc.player.getOffhandItem().is(MapAtlasesMod.MAP_ATLAS.get()))) {
            return;
        }

        if (currentAtlas != atlas) {
            needsInit = true;
        }
        currentAtlas = atlas;

        if (MapAtlasItem.isLocked(currentAtlas)) {
           return;
        }

        if (needsInit) {
            needsInit = false;
            initialize(activeMap);
        }
        mapWherePlayerIs = activeMap;

        if (!Objects.equals(lastMapKey, currentMapKey)) {
            lastMapKey = currentMapKey;
            if (mc.screen == null && MapAtlasesClientConfig.mapChangeSound.get()) {
                mc.player.playSound(MapAtlasesMod.ATLAS_PAGE_TURN_SOUND_EVENT.get(),
                        (float) (double) MapAtlasesClientConfig.soundScalar.get(), 1.0F);
            }
        }

        int mapWidgetSize = (int) (BG_SIZE * (116 / 128f));
        Anchoring anchorLocation = MapAtlasesClientConfig.miniMapAnchoring.get();
        int off = 5;
        int x = anchorLocation.isLeft ? off : (int) (screenWidth / globalScale) - (BG_SIZE + off);
        int y = anchorLocation.isUp ? off : (int) (screenHeight / globalScale) - (BG_SIZE + off);
        x += (int) (MapAtlasesClientConfig.miniMapHorizontalOffset.get() / globalScale);
        y += (int) (MapAtlasesClientConfig.miniMapVerticalOffset.get() / globalScale);
        y -= 4;
        if (!hasCompass(mc.player)) {
        y -= 12;
        }

        // Push the minimap down to leave space for the biome label above it.
        // Only apply the offset when the biome display is enabled.
        if (MapAtlasesClientConfig.drawMinimapBiome.get()) {
            if (anchorLocation.isUp) {
                y += BIOME_ABOVE_OFFSET;
            } else {
                y += 8;
            }
        }

        if (anchorLocation == Anchoring.UPPER_RIGHT) {
            y = Math.max(y, getPotionOffsetY(mc.player, y));
        }

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.scale(globalScale, globalScale);

        if (!followingPlayer) {
            currentXCenter = Math.floor((mc.player.getX() + 64) / mapBlocksSize) * mapBlocksSize + (mapBlocksSize / 2f - 64);
            currentZCenter = Math.floor((mc.player.getZ() + 64) / mapBlocksSize) * mapBlocksSize + (mapBlocksSize / 2f - 64);
        } else {
            currentXCenter = mc.player.getX();
            currentZCenter = mc.player.getZ();
        }

        MapAtlasesClient.setDecorationsScale((float) (2 * zoomLevel * MapAtlasesClientConfig.miniMapDecorationScale.get()));
        MapAtlasesClient.setDecorationsTextScale((float) (4 * zoomLevel * MapAtlasesClientConfig.miniMapDecorationTextScale.get()));
        float yRot = mc.player.getYRot();
        MapAtlasesClient.setDecorationRotation(yRot - 180);
        int light = !MapAtlasesClientConfig.minimapSkyLight.get() ? 0x00F000F0 :
                packSkyLight(mc.level.getBrightness(LightLayer.SKY, mc.player.getOnPos().above()));
        int borderSize = (BG_SIZE - mapWidgetSize) / 2;

        graphics.blit(RenderPipelines.GUI_TEXTURED, MAP_HUD_BACKGROUND_TEXTURE, x, y,
                0, 0, BG_SIZE, BG_SIZE, BG_SIZE, BG_SIZE);

        boolean hasCompass = hasCompass(mc.player);
        this.rotatesWithPlayer = hasCompass;
        if (hasCompass != lastHadCompass) {
            lastHadCompass = hasCompass;
            needsInit = true;
        }

        drawAtlas(graphics, x + borderSize, y + borderSize, mapWidgetSize, mapWidgetSize, mc.player,
                zoomLevel * (float) (double) MapAtlasesClientConfig.miniMapZoomMultiplier.get(),
                MapAtlasesClientConfig.miniMapBorder.get(), currentMapKey.slice().type(), light, null);

        graphics.nextStratum();
        graphics.blit(RenderPipelines.GUI_TEXTURED, MAP_HUD_FOREGROUND_TEXTURE, x, y,
                0, 0, BG_SIZE, BG_SIZE, BG_SIZE, BG_SIZE);

        MapAtlasesClient.setDecorationsScale(1);
        MapAtlasesClient.setDecorationsTextScale(1);
        MapAtlasesClient.setDecorationRotation(0);

        pose.pushMatrix();
        pose.translate(x + mapWidgetSize / 2f + 3f, y + mapWidgetSize / 2f + 3f);
        if (!rotatesWithPlayer) {
            pose.rotate((float) Math.toRadians(yRot - 180));
        }
        if (drawBigPlayerMarker) {
            pose.translate(-4.5f, -4f);
            graphics.nextStratum();
            graphics.blit(RenderPipelines.GUI_TEXTURED, MAP_PLAYER_DECORATION_TEXTURE, 0, 0,
                    0, 0, 8, 8, 8, 8);
        }
        pose.popMatrix();

        graphics.nextStratum();
        drawHudText(graphics, x, y, screenWidth, mapWidgetSize, anchorLocation);

        graphics.nextStratum();
        drawCardinals(graphics, x, y, yRot);

        if (MapAtlasesClientConfig.moonlightCompat.get() && MapAtlasesClientConfig.moonlightPinTracking.get()) {
            graphics.nextStratum();
            pose.pushMatrix();
            pose.translate(x + BG_SIZE / 2f, y + BG_SIZE / 2f);
            ClientMarkersRenderer.drawSmallPins(graphics, mc.font, currentXCenter + mapBlocksSize / 2f,
                    currentZCenter + mapBlocksSize / 2f, currentMapKey.slice(), mapBlocksSize * zoomLevel,
                    mc.player, rotatesWithPlayer, currentMaps);
            pose.popMatrix();
        }

        pose.popMatrix();
    }

    private static int packSkyLight(int skyLight) {
        return skyLight << 20 | skyLight << 4;
    }

    private int getPotionOffsetY(net.minecraft.client.player.LocalPlayer player, int y) {
        boolean hasBeneficial = false;
        boolean hasNegative = false;
        for (MobEffectInstance effectInstance : player.getActiveEffects()) {
            if (effectInstance.getEffect().value().isBeneficial()) {
                hasBeneficial = true;
            } else {
                hasNegative = true;
            }
        }
        int offset = MapAtlasesClientConfig.activePotionVerticalOffset.get();
        if (hasNegative && y < 2 * offset) {
            return 2 * offset;
        }
        if (hasBeneficial && y < offset) {
            return offset;
        }
        return y;
    }

    private void drawHudText(GuiGraphicsExtractor graphics, int x, int y, int screenWidth, int mapWidgetSize, Anchoring anchorLocation) {
        Matrix3x2fStack pose = graphics.pose();
        float textScaling = (float) (double) MapAtlasesClientConfig.minimapCoordsAndBiomeScale.get();
        int textHeightOffset = 2;
        int actualBgSize = (int) (BG_SIZE * globalScale);

        // Draw biome label above the minimap background.
        if (MapAtlasesClientConfig.drawMinimapBiome.get()) {
            pose.pushMatrix();
            if (!anchorLocation.isUp) {
                // When anchored to the bottom the minimap was shifted up, so place
                // the biome label below the minimap (which visually is "above" the
                // original anchor edge).
                pose.translate(0, BG_SIZE + BIOME_ABOVE_OFFSET + 2);
            }

            // Use a wider maxWidth so long biome names don't shrink as aggressively.
            // targetWidth stays at actualBgSize so the text remains centred over the minimap.
            int biomeMaxWidth = actualBgSize * 3;
            drawMapComponentBiome(graphics, mc.font, x, (int) (y - BIOME_ABOVE_OFFSET + 2),
                    actualBgSize, biomeMaxWidth, textScaling, mc.player.blockPosition(), mc.level);
            pose.popMatrix();
        }

        pose.pushMatrix();
        if (!anchorLocation.isUp) {
            pose.translate(0, -BG_SIZE - 20 * textScaling - 2);
        }

        Font font = mc.font;
        boolean global = MapAtlasesClientConfig.drawMinimapCoords.get();
        boolean local = MapAtlasesClientConfig.drawMinimapChunkCoords.get();
        if (global || local) {
            BlockPos pos = new BlockPos(new Vec3i(
                    towardsZero(mc.player.position().x),
                    towardsZero(mc.player.position().y),
                    towardsZero(mc.player.position().z)));
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

        pose.popMatrix();
    }

    private boolean hasCompass(Player player) {
        // Main + offhand
        if (isVanillaCompass(player.getMainHandItem()) || isVanillaCompass(player.getOffhandItem())) {
            return true;
        }

        // Inventory (includes hotbar)
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isVanillaCompass(stack)) {
                return true;
            }
        }

        return false;
    }

    private boolean isVanillaCompass(ItemStack stack) {
        return stack.is(Items.COMPASS) 
            && !stack.has(net.minecraft.core.component.DataComponents.LODESTONE_TRACKER);
    }

    private void drawCardinals(GuiGraphicsExtractor graphics, int x, int y, float yRot) {
        if (mc.player == null || !hasCompass(mc.player) || mc.level.dimension() == Level.NETHER) {
            return;
        }
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x + BG_SIZE / 2f, y + BG_SIZE / 2f);

        var p = getDirectionPos(BG_SIZE / 2f - 3, rotatesWithPlayer ? yRot : 180);
        float a = p.getFirst();
        float b = p.getSecond();
        drawNorthLetter(graphics, mc.font, a, b, "N");
        if (!MapAtlasesClientConfig.miniMapOnlyNorth.get()) {
            drawLetter(graphics, mc.font, -a, -b, "S");
            drawLetter(graphics, mc.font, -b, a, "E");
            drawLetter(graphics, mc.font, b, -a, "W");
        }

        pose.popMatrix();
    }

    private void drawLetter(GuiGraphicsExtractor graphics, Font font, float a, float b, String letter) {
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        float scale = (float) (double) MapAtlasesClientConfig.miniMapCardinalsScale.get() / globalScale;
        pose.scale(scale, scale);
        PlatStuff.drawString(graphics, font, letter,
            a / scale - font.width(letter) / 2f + 1,
            b / scale - font.lineHeight / 2f + 1,
            0xFF323232, false);
        PlatStuff.drawString(graphics, font, letter,
            a / scale - font.width(letter) / 2f,
            b / scale - font.lineHeight / 2f,
            0xFFE0E0E0, false);
        pose.popMatrix();
    }


    private void drawNorthLetter(GuiGraphicsExtractor graphics, Font font, float a, float b, String letter) {
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        float scale = (float) (double) MapAtlasesClientConfig.miniMapCardinalsScale.get() / globalScale;
        pose.scale(scale, scale);
        PlatStuff.drawString(graphics, font, letter,
            a / scale - font.width(letter) / 2f + 1,
            b / scale - font.lineHeight / 2f + 1,
            0xFF000000, false);
        PlatStuff.drawString(graphics, font, letter,
            a / scale - font.width(letter) / 2f - 1,
            b / scale - font.lineHeight / 2f - 1,
            0xFF000000, false);
        PlatStuff.drawString(graphics, font, letter,
            a / scale - font.width(letter) / 2f + 1,
            b / scale - font.lineHeight / 2f - 1,
            0xFF000000, false);
        PlatStuff.drawString(graphics, font, letter,
            a / scale - font.width(letter) / 2f - 1,
            b / scale - font.lineHeight / 2f + 1,
            0xFF000000, false);
        PlatStuff.drawString(graphics, font, letter,
            a / scale - font.width(letter) / 2f,
            b / scale - font.lineHeight / 2f - 1,
            0xFF000000, false);
        PlatStuff.drawString(graphics, font, letter,
            a / scale - font.width(letter) / 2f,
            b / scale - font.lineHeight / 2f + 1,
            0xFF000000, false);
        PlatStuff.drawString(graphics, font, letter,
            a / scale - font.width(letter) / 2f - 1,
            b / scale - font.lineHeight / 2f,
            0xFF000000, false);
        PlatStuff.drawString(graphics, font, letter,
            a / scale - font.width(letter) / 2f + 1,
            b / scale - font.lineHeight / 2f,
            0xFF000000, false);
        PlatStuff.drawString(graphics, font, letter,
            a / scale - font.width(letter) / 2f,
            b / scale - font.lineHeight / 2f,
            0xFFF21D25, false);
        pose.popMatrix();
    }

    private static int towardsZero(double d) {
        if (d < 0.0) {
            return -1 * (int) Math.floor(-1 * d);
        }
        return (int) Math.floor(d);
    }

    public void drawMapComponentCoords(
            GuiGraphicsExtractor context,
            Font font,
            int x, int y,
            int targetWidth,
            float textScaling,
            BlockPos pos,
            boolean chunk
    ) {
        String coordsToDisplay;
        if (chunk) {
            coordsToDisplay = Component.translatable("message.map_atlases.chunk_coordinates",
                    pos.getX() / 16, pos.getZ() / 16, pos.getX() % 16, pos.getZ() % 16).getString();
        } else {
            coordsToDisplay = displaysY ?
                    Component.translatable("message.map_atlases.coordinates_full",
                            pos.getX(), pos.getY(), pos.getZ()).getString()
                    : Component.translatable("message.map_atlases.coordinates",
                    pos.getX(), pos.getZ()).getString();
        }
        drawScaledComponent(context, font, x, y, coordsToDisplay,
                textScaling / globalScale, targetWidth, (int) (targetWidth / globalScale));
    }

    public void drawMapComponentBiome(
            GuiGraphicsExtractor context,
            Font font,
            int x, int y,
            int targetWidth,
            int maxWidth,
            float textScaling,
            BlockPos blockPos,
            Level level
    ) {
        String biomeToDisplay = "";
        var key = level.getBiome(blockPos).unwrapKey();
        if (key.isPresent()) {
            ResourceKey<Biome> biomeKey = key.get();
            var id = biomeKey.identifier();
            biomeToDisplay = Component.translatable("biome." + id.getNamespace() + "." +
                    id.getPath().replace('/', '.')).getString();
        }
        drawScaledComponent(context, font, x, y, biomeToDisplay,
                textScaling / globalScale, maxWidth, (int) (targetWidth / globalScale));
    }

    public static void drawScaledComponent(
            GuiGraphicsExtractor context,
            Font font,
            int x, int y,
            String text,
            float textScaling,
            int maxWidth,
            int targetWidth
    ) {
        Matrix3x2fStack pose = context.pose();
        float textWidth = font.width(text);
        float scale = Math.min(1, maxWidth * textScaling / Math.max(textWidth, 1));
        scale *= textScaling;
        float centerX = x + targetWidth / 2f;

        pose.pushMatrix();
        pose.translate(centerX, y + 4);
        pose.scale(scale, scale);
        pose.translate(-(textWidth) / 2f, -4);
        drawStringWithLighterShadow(context, font, text, 0, 0);
        pose.popMatrix();
    }

    private static void drawStringWithLighterShadow(GuiGraphicsExtractor context, Font font, String text, float x, float y) {
        PlatStuff.drawString(context, font, text, x + 1, y + 1, 0xFF323232, false);
        PlatStuff.drawString(context, font, text, x, y, 0xFFE0E0E0, false);
    }

    public static Pair<Float, Float> getDirectionPos(float radius, float angleDegrees) {
        angleDegrees = Mth.wrapDegrees(90 - angleDegrees);
        float angleRadians = (float) Math.toRadians(angleDegrees);

        float x;
        float y;
        if (angleDegrees >= -45 && angleDegrees < 45) {
            x = radius;
            y = radius * (float) Math.tan(angleRadians);
        } else if (angleDegrees >= 45 && angleDegrees < 135) {
            x = radius / (float) Math.tan(angleRadians);
            y = radius;
        } else if (angleDegrees >= 135 || angleDegrees < -135) {
            x = -radius;
            y = -radius * (float) Math.tan(angleRadians);
        } else {
            x = -radius / (float) Math.tan(angleRadians);
            y = -radius;
        }
        return Pair.of(x, y);
    }

    public void increaseZoom() {
        zoomLevel = Math.max(1, zoomLevel - 0.5f);
    }

    public void decreaseZoom() {
        zoomLevel = Math.min(10, zoomLevel + 0.5f);
    }
}