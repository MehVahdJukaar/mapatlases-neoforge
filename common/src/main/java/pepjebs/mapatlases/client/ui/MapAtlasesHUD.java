package pepjebs.mapatlases.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.capabilities.MapCollectionCap;
import pepjebs.mapatlases.capabilities.MapKey;
import pepjebs.mapatlases.client.AbstractAtlasWidget;
import pepjebs.mapatlases.client.Anchoring;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.integration.ImmediatelyFastCompat;
import pepjebs.mapatlases.integration.moonlight.ClientMarkers;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.Slice;

import java.util.Objects;

import static pepjebs.mapatlases.client.screen.DecorationBookmarkButton.MAP_ICON_TEXTURE;

public class MapAtlasesHUD extends AbstractAtlasWidget implements IGuiOverlay {

    public static final ResourceLocation MAP_BACKGROUND = MapAtlasesMod.res("textures/gui/hud/map_background.png");
    public static final ResourceLocation MAP_FOREGROUND = MapAtlasesMod.res("textures/gui/hud/map_foreground.png");

    private static final int BACKGROUND_SIZE = 128;
    protected final int BG_SIZE = 64;

    private final Minecraft mc;

    //cached stuff
    private boolean needsInit = true;
    private ItemStack currentAtlas = ItemStack.EMPTY;
    private MapKey currentMapKey = null;
    private MapKey lastMapKey = null;


    private float globalScale = 1;
    private boolean displaysY = true;

    public MapAtlasesHUD() {
        super(1);
        this.mc = Minecraft.getInstance();
        this.rotatesWithPlayer = true;
        this.zoomLevel = 1;
    }

    @Nullable
    @Override
    public MapDataHolder getMapWithCenter(int centerX, int centerZ) {
        //TODO: cache this too
        Slice slice = currentMapKey.slice();
        MapCollectionCap maps = MapAtlasItem.getMaps(currentAtlas, mc.level);
        return maps.select(centerX, centerZ, slice);
    }

    @Override
    protected void initialize(MapDataHolder originalCenterMap) {
        super.initialize(originalCenterMap);

        this.followingPlayer = MapAtlasesClientConfig.miniMapFollowPlayer.get();
        this.rotatesWithPlayer = MapAtlasesClientConfig.miniMapRotate.get();
        this.globalScale = (float) (double) MapAtlasesClientConfig.miniMapScale.get();

        this.displaysY = !MapAtlasesClientConfig.yOnlyWithSlice.get() || MapAtlasItem.getMaps(currentAtlas, mc.level).hasOneSlice();
        this.drawBigPlayerMarker = followingPlayer;

    }

    @Override
    protected void applyScissors(GuiGraphics graphics, int x, int y, int x1, int y1) {
        super.applyScissors(graphics, (int) (x * globalScale), (int) (y * globalScale), (int) (x1 * globalScale), (int) (y1 * globalScale));
    }

    @Override
    public void render(ForgeGui forgeGui, GuiGraphics graphics, float partialTick,
                       int screenWidth, int screenHeight) {
        // Handle early returns
        if (mc.level == null || mc.player == null) {
            return;
        }

        // Check F3 menu displayed
        if (mc.options.renderDebug) return;
        if (!MapAtlasesClientConfig.drawMiniMapHUD.get()) return;

        ItemStack atlas = MapAtlasesClient.getCurrentActiveAtlas();

        if (MapAtlasesClientConfig.hideWhenInHand.get() && (mc.player.getMainHandItem().is(MapAtlasesMod.MAP_ATLAS.get()) ||
                mc.player.getOffhandItem().is(MapAtlasesMod.MAP_ATLAS.get()))) return;

        if (currentAtlas != atlas) {
            needsInit = true;
        }
        currentAtlas = atlas;

        if (atlas.isEmpty()) return;

        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;

        MapCollectionCap maps = MapAtlasItem.getMaps(atlas, level);
        currentMapKey = MapAtlasesClient.getActiveMapKey();
        if (currentMapKey == null) return;
        MapDataHolder activeMap = maps.select(currentMapKey);
        if (activeMap == null) return;


        if (MapAtlasesMod.IMMEDIATELY_FAST) ImmediatelyFastCompat.startBatching();
        if (needsInit) {
            needsInit = false;
            initialize(activeMap);
        }
        mapWherePlayerIs = activeMap;

        PoseStack poseStack = graphics.pose();

        poseStack.pushPose();

        //scaling on 0,0
        poseStack.scale(globalScale, globalScale, 1);

        // play sound
        if (!Objects.equals(lastMapKey, currentMapKey)) {
            lastMapKey = currentMapKey;
            if (mc.screen == null && MapAtlasesClientConfig.mapChangeSound.get()) {
                player.playSound(MapAtlasesMod.ATLAS_PAGE_TURN_SOUND_EVENT.get(),
                        (float) (double) MapAtlasesClientConfig.soundScalar.get(), 1.0F);
            }
        }

        int mapWidgetSize = (int) (BG_SIZE * (116 / 128f));
        // Draw map background
        Anchoring anchorLocation = MapAtlasesClientConfig.miniMapAnchoring.get();
        int x = anchorLocation.isLeft ? 0 : (int) (screenWidth / globalScale) - BG_SIZE;
        int y = anchorLocation.isUp ? 0 : (int) (screenHeight / globalScale) - BG_SIZE;
        x += MapAtlasesClientConfig.miniMapHorizontalOffset.get() / globalScale;
        y += MapAtlasesClientConfig.miniMapVerticalOffset.get() / globalScale;

        //TODO: fix rounding error when at non integer scales
        if (anchorLocation.isUp && !anchorLocation.isLeft) {
            boolean hasBeneficial = false;
            boolean hasNegative = false;
            for (var e : player.getActiveEffects()) {
                MobEffect effect = e.getEffect();
                if (effect.isBeneficial()) {
                    hasBeneficial = true;
                } else {
                    hasNegative = true;
                }
            }
            int offsetForEffects = MapAtlasesClientConfig.activePotionVerticalOffset.get();
            if (hasNegative && y < 2 * offsetForEffects) {
                y += (2 * offsetForEffects - y);
            } else if (hasBeneficial && y < offsetForEffects) {
                y += (offsetForEffects - y);
            }
        }

        graphics.blit(MAP_BACKGROUND, x, y, BG_SIZE, BG_SIZE, 0, 0,
                BACKGROUND_SIZE, BACKGROUND_SIZE, BACKGROUND_SIZE, BACKGROUND_SIZE);

        // Draw map data

        poseStack.pushPose();


        //rounds up if following player
        if (!followingPlayer) {
            currentXCenter = currentXCenter % mapBlocksSize + Math.round(player.getX() / mapBlocksSize) * mapBlocksSize;
            currentZCenter = currentZCenter % mapBlocksSize + Math.round(player.getZ() / mapBlocksSize) * mapBlocksSize;
        } else {
            currentXCenter = player.getX();
            currentZCenter = player.getZ();
        }

        // Set zoom-height for map icons
        MapAtlasesClient.setDecorationsScale((float) (2 * zoomLevel * MapAtlasesClientConfig.miniMapDecorationScale.get()));
        float yRot = player.getYRot();
        if (rotatesWithPlayer) {
            MapAtlasesClient.setDecorationRotation(yRot - 180);
        }
        int light = !MapAtlasesClientConfig.minimapSkyLight.get() ? LightTexture.FULL_BRIGHT :
                LightTexture.pack(0, level.getBrightness(LightLayer.SKY, player.getOnPos().above()));
        int borderSize = (BG_SIZE - mapWidgetSize) / 2;
        drawAtlas(graphics, x + borderSize, y + borderSize,
                mapWidgetSize, mapWidgetSize, player,
                zoomLevel * (float) (double) MapAtlasesClientConfig.miniMapZoomMultiplier.get(),
                MapAtlasesClientConfig.miniMapBorder.get(), currentMapKey.slice().type(), light);

        MapAtlasesClient.setDecorationsScale(1);
        if (rotatesWithPlayer) {
            MapAtlasesClient.setDecorationRotation(0);
        }

        //always render as its better
        poseStack.pushPose();
        poseStack.translate(x + mapWidgetSize / 2f + 3f, y + mapWidgetSize / 2f + 3, 0);
        if (!rotatesWithPlayer) {
            poseStack.mulPose(Axis.ZN.rotationDegrees(180 - yRot));
        }
        if (drawBigPlayerMarker) {
            poseStack.translate(-4.5f, -4f, 0);
            graphics.blit(MAP_ICON_TEXTURE, 0,
                    0,
                    0, 0, 8, 8, 128, 128);
        }
        poseStack.popPose();

        poseStack.popPose();
        //  graphics.blit(MAP_FOREGROUND, x, y, 0, 0, mapBgScaledSize, mapBgScaledSize, mapBgScaledSize, mapBgScaledSize);
        // Draw text data
        float textScaling = (float) (double) MapAtlasesClientConfig.minimapCoordsAndBiomeScale.get();
        int textHeightOffset = 2;
        int actualBgSize = (int) (BG_SIZE * globalScale);
        if (!anchorLocation.isUp) {
            //textHeightOffset = -actualBgSize + ;
        }
        Font font = mc.font;
        boolean global = MapAtlasesClientConfig.drawMinimapCoords.get();
        boolean local = MapAtlasesClientConfig.drawMinimapChunkCoords.get();
        if (global || local) {
            BlockPos pos = new BlockPos(new Vec3i(
                    towardsZero(player.position().x),
                    towardsZero(player.position().y),
                    towardsZero(player.position().z)));
            if (global) {
                drawMapComponentCoords(
                        graphics, font, x, (int) (y + BG_SIZE + (textHeightOffset / globalScale)), actualBgSize,
                        textScaling, pos, false);
                textHeightOffset += (10 * textScaling);
            }
            if (local) {
                drawMapComponentCoords(
                        graphics, font, x, (int) (y + BG_SIZE + (textHeightOffset / globalScale)), actualBgSize,
                        textScaling, pos, true);
                textHeightOffset += (10 * textScaling);
            }
        }

        if (MapAtlasesClientConfig.drawMinimapBiome.get()) {
            drawMapComponentBiome(
                    graphics, font, x, (int) (y + BG_SIZE + (textHeightOffset / globalScale)), actualBgSize,
                    textScaling, player.blockPosition(), level);
        }

        if (MapAtlasesClientConfig.drawMinimapCardinals.get()) {
            poseStack.pushPose();
            poseStack.translate(x + BG_SIZE / 2f, y + BG_SIZE / 2f, 5);

            var p = getDirectionPos(BG_SIZE / 2f - 3, rotatesWithPlayer ? yRot : 180);
            float a = p.getFirst();
            float b = p.getSecond();
            drawLetter(graphics, font, a, b, "N");
            if (!MapAtlasesClientConfig.miniMapOnlyNorth.get()) {
                drawLetter(graphics, font, -a, -b, "S");
                drawLetter(graphics, font, -b, a, "E");
                drawLetter(graphics, font, b, -a, "W");
            }

            poseStack.popPose();
        }

        if (MapAtlasesMod.MOONLIGHT && MapAtlasesClientConfig.moonlightPinTracking.get()) {
            poseStack.pushPose();
            RenderSystem.enableDepthTest();
            poseStack.translate(x + BG_SIZE / 2f, y + BG_SIZE / 2f, -10);
            ClientMarkers.drawSmallPins(graphics, font, currentXCenter + mapBlocksSize / 2f, currentZCenter + mapBlocksSize / 2f, currentMapKey.slice(),
                    mapBlocksSize * zoomLevel, player, rotatesWithPlayer);
            poseStack.popPose();
        }

        poseStack.popPose();

        if (MapAtlasesMod.IMMEDIATELY_FAST) ImmediatelyFastCompat.endBatching();
    }

    private void drawLetter(GuiGraphics graphics, Font font, float a, float b, String letter) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        float scale = (float) (double) MapAtlasesClientConfig.miniMapCardinalsScale.get() / globalScale;
        pose.scale(scale, scale, 1);
        drawStringWithLighterShadow(graphics, font, letter, a / scale - font.width(letter) / 2f,
                b / scale - font.lineHeight / 2f);

        pose.popPose();
    }

    private static int towardsZero(double d) {
        if (d < 0.0)
            return -1 * (int) Math.floor(-1 * d);
        else
            return (int) Math.floor(d);
    }

    public void drawMapComponentCoords(
            GuiGraphics context,
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
            coordsToDisplay = displaysY ? pos.toShortString() : Component.translatable("message.map_atlases.coordinates",
                    pos.getX() + ", " + pos.getZ()).getString();
        }
        drawScaledComponent(context, font, x, y, coordsToDisplay, textScaling / globalScale, targetWidth, (int) (targetWidth / globalScale));
    }

    public void drawMapComponentBiome(
            GuiGraphics context,
            Font font,
            int x, int y,
            int targetWidth,
            float textScaling,
            BlockPos blockPos,
            Level level
    ) {
        String biomeToDisplay = "";
        var key = level.getBiome(blockPos).unwrapKey();
        if (key.isPresent()) {
            ResourceKey<Biome> biomeKey = key.get();
            biomeToDisplay = Component.translatable(Util.makeDescriptionId("biome", biomeKey.location())).getString();
        }
        drawScaledComponent(context, font, x, y, biomeToDisplay, textScaling / globalScale, targetWidth, (int) (targetWidth / globalScale));
    }

    public static void drawScaledComponent(
            GuiGraphics context,
            Font font,
            int x, int y,
            String text,
            float textScaling,
            int maxWidth,
            int targetWidth
    ) {
        PoseStack pose = context.pose();
        float textWidth = font.width(text);

        float scale = Math.min(1, maxWidth * textScaling / textWidth);
        scale *= textScaling;

        float centerX = x + targetWidth / 2f;

        pose.pushPose();
        pose.translate(centerX, y + 4, 5);
        pose.scale(scale, scale, 1);
        pose.translate(-(textWidth) / 2f, -4, 0);
        // uses slightly lighter drop shadow
        drawStringWithLighterShadow(context, font, text, 0, 0);
        pose.popPose();
    }

    private static void drawStringWithLighterShadow(GuiGraphics context, Font font, String text, float x, float y) {
        context.drawString(font, text, x + 1, y + 1, 0x595959, false);
        context.drawString(font, text, x, y, 0xE0E0E0, false);
    }

    public static Pair<Float, Float> getDirectionPos(float radius, float angleDegrees) {

        angleDegrees = Mth.wrapDegrees(90 - angleDegrees);

        // Convert angle from degrees to radians
        float angleRadians = (float) Math.toRadians(angleDegrees);

        // Calculate the coordinates of the point on the square
        float x, y;

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