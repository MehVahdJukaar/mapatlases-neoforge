package pepjebs.mapatlases.client.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.joml.Matrix4d;
import org.joml.Vector4d;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.capabilities.MapCollectionCap;
import pepjebs.mapatlases.capabilities.MapKey;
import pepjebs.mapatlases.client.AbstractAtlasWidget;
import pepjebs.mapatlases.client.Anchoring;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.item.MapAtlasItem;

import static pepjebs.mapatlases.client.screen.DecorationBookmarkButton.MAP_ICON_TEXTURE;

public class MapAtlasesHUD extends AbstractAtlasWidget implements IGuiOverlay {

    public static final ResourceLocation MAP_BACKGROUND = MapAtlasesMod.res("textures/gui/hud/map_background.png");
    public static final ResourceLocation MAP_FOREGROUND = MapAtlasesMod.res("textures/gui/hud/map_foreground.png");

    private static final int BACKGROUND_SIZE = 128;
    protected final int BG_SIZE = 64;

    private final Minecraft mc;

    //cached stuff
    private boolean needsInit = true;
    private ItemStack currentAtlas;
    private MapKey currentMapKey = null;


    private float globalScale = 1;
    private boolean displaysY = true;

    public MapAtlasesHUD() {
        super(1);
        this.mc = Minecraft.getInstance();
        this.rotatesWithPlayer = true;
        this.zoomLevel = 1;
        this.drawPlayerIcon = false;
    }

    @Override
    public Pair<String, MapItemSavedData> getMapWithCenter(int centerX, int centerZ) {
        //TODO: cache this too
        return MapAtlasItem.getMaps(currentAtlas, mc.level).select(centerX, centerZ,
                currentMapKey.dimension(), currentMapKey.slice());
    }

    @Override
    protected void initialize(MapItemSavedData originalCenterMap) {
        super.initialize(originalCenterMap);

        this.followingPlayer = MapAtlasesClientConfig.miniMapFollowPlayer.get();
        this.rotatesWithPlayer = MapAtlasesClientConfig.miniMapRotate.get();
        this.globalScale = (float) (double) MapAtlasesClientConfig.miniMapScale.get();

        this.displaysY = !MapAtlasesClientConfig.yOnlyWithSlice.get() || MapAtlasItem.getMaps(currentAtlas, mc.level).hasOneSlice();
    }

    public Vector4d transformPos(double mouseX, double mouseZ) {
        return scaleVector(mouseX, mouseZ, globalScale, BG_SIZE, BG_SIZE);
    }

    public static Vector4d scaleVector(double mouseX, double mouseZ, float scale, int w, int h) {
        Matrix4d matrix4d = new Matrix4d();

        // Calculate the translation and scaling factors
        double translateX = w / 2.0;
        double translateY = h / 2.0;
        double scaleFactor = scale - 1.0;

        // Apply translation to the matrix (combined)
        matrix4d.translate(translateX, translateY, 0);

        // Apply scaling to the matrix
        matrix4d.scale(1.0 + scaleFactor);

        // Apply translation back to the original position (combined)
        matrix4d.translate(-translateX, -translateY, 0);

        // Create a vector with the input coordinates
        Vector4d v = new Vector4d(mouseX, mouseZ, 0, 1.0F);

        // Apply the transformation matrix to the vector
        matrix4d.transform(v);
        return v;
    }

    @Override
    protected void applyScissors(GuiGraphics graphics, int x, int y, int x1, int y1) {
        var v = transformPos(x, y);
        var v2 = transformPos(x1, y1);
        super.applyScissors(graphics, (int) v.x, (int) v.y, (int) v2.x, (int) v2.y);
    }

    @Override
    public void render(ForgeGui forgeGui, GuiGraphics graphics, float partialTick,
                       int screenWidth, int screenHeight) {
        // Handle early returns
        if (mc.level == null || mc.player == null) {
            return;
        }

        // Check config disable
        // Check F3 menu displayed
        if (mc.options.renderDebug) return;
        if (!MapAtlasesClientConfig.drawMiniMapHUD.get()) return;


        ItemStack atlas = MapAtlasesClient.getCurrentActiveAtlas();

        if (atlas != currentAtlas) {
            currentAtlas = atlas;
            needsInit = true;
        }

        if (atlas.isEmpty()) return;

        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;

        MapCollectionCap maps = MapAtlasItem.getMaps(atlas, level);
        currentMapKey = MapAtlasesClient.getActiveMapKey();
        if (currentMapKey == null) return;
        Pair<String, MapItemSavedData> activeMap = maps.select(currentMapKey);
        if (activeMap == null) {
            return;
        }


        MapItemSavedData state = activeMap.getSecond();

        if (needsInit) {
            needsInit = false;
            initialize(state);
        }

        PoseStack poseStack = graphics.pose();

        poseStack.pushPose();

        poseStack.translate(BG_SIZE / 2f, BG_SIZE / 2f, 0);
        poseStack.scale(globalScale, globalScale, 1);
        poseStack.translate(-BG_SIZE / 2f, -BG_SIZE / 2f, 0);


        // Update client current map id
        // TODO: dont like this sound here. should be in tick instead
        // playSoundIfMapChanged(curMapId, level, player);

        int mapWidgetSize = (int) (64 * 116 / 128f);

        // Draw map background
        Anchoring anchorLocation = MapAtlasesClientConfig.miniMapAnchoring.get();
        int x = anchorLocation.isLeft ? 0 : screenWidth - mapWidgetSize * 3 / 2;
        int y = !anchorLocation.isUp ? screenHeight - mapWidgetSize * 3 / 2 : 0;
        x += MapAtlasesClientConfig.miniMapHorizontalOffset.get() * globalScale * 2;
        y += MapAtlasesClientConfig.miniMapVerticalOffset.get() * globalScale * 2;

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

        if (followingPlayer) {
            currentXCenter = player.getX();
            currentZCenter = player.getZ();
        }

        // Set zoom-level for map icons
        MapAtlasesClient.setDecorationsScale((float) (zoomLevel * MapAtlasesClientConfig.miniMapDecorationScale.get()));
        float yRot = player.getYRot();
        if (rotatesWithPlayer) {
            MapAtlasesClient.setDecorationRotation(yRot - 180);
        }
        drawAtlas(graphics, x + (BG_SIZE - mapWidgetSize) / 2, y + (BG_SIZE - mapWidgetSize) / 2,
                mapWidgetSize, mapWidgetSize, player,
                zoomLevel * (float) (double) MapAtlasesClientConfig.miniMapZoomMultiplier.get(),
                MapAtlasesClientConfig.miniMapBorder.get());

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
        poseStack.translate(-4.5f, -4f, 0);
        graphics.blit(MAP_ICON_TEXTURE, 0,
                0,
                0, 0, 8, 8, 128, 128);

        poseStack.popPose();

        poseStack.popPose();
        //  graphics.blit(MAP_FOREGROUND, x, y, 0, 0, mapBgScaledSize, mapBgScaledSize, mapBgScaledSize, mapBgScaledSize);
        // Draw text data
        float textScaling = (float) (double) MapAtlasesClientConfig.minimapCoordsAndBiomeScale.get();
        int textHeightOffset = BG_SIZE + 4;
        if (!anchorLocation.isUp) {
            textHeightOffset = (int) (-24 * textScaling);
        }
        Font font = mc.font;
        if (MapAtlasesClientConfig.drawMinimapCoords.get()) {
            drawMapComponentCoords(
                    graphics, font, x, y + textHeightOffset, BG_SIZE,
                    textScaling, new BlockPos(new Vec3i(
                            towardsZero(player.position().x),
                            towardsZero(player.position().y),
                            towardsZero(player.position().z))));
            textHeightOffset += (10 * textScaling);
        }
        if (MapAtlasesClientConfig.drawMinimapBiome.get()) {
            drawMapComponentBiome(
                    graphics, font, x, y + textHeightOffset, BG_SIZE,
                    textScaling, player.blockPosition(), level);
        }

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


        poseStack.popPose();
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

    private static void playSoundIfMapChanged(String curMapId, ClientLevel level, LocalPlayer player) {
        /*
        if (!curMapId.equals(localPlayerCurrentMapId)) {
            if (localPlayerCurrentMapId != null) {
                level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                        MapAtlasesMod.ATLAS_PAGE_TURN_SOUND_EVENT.get(), SoundSource.PLAYERS,
                        (float) (double) MapAtlasesClientConfig.soundScalar.get(), 1.0F, false);
            }
            localPlayerCurrentMapId = curMapId;
        }*/
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
            BlockPos pos
    ) {
        String coordsToDisplay = displaysY ? pos.toShortString() : pos.getX() + ", " + pos.getZ();
        drawScaledComponent(context, font, x, y, coordsToDisplay, textScaling / globalScale, targetWidth);
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
        drawScaledComponent(context, font, x, y, biomeToDisplay, textScaling / globalScale, targetWidth);
    }

    public static void drawScaledComponent(
            GuiGraphics context,
            Font font,
            int x, int y,
            String text,
            float textScaling,
            int targetWidth
    ) {
        PoseStack pose = context.pose();
        float textWidth = font.width(text);

        float scale = Math.min(1, targetWidth * textScaling / textWidth);
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

    private static Pair<Float, Float> getDirectionPos(float radius, float angleDegrees) {

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

    public void decreaseZoom() {
        zoomLevel = Math.max(1, zoomLevel - 0.5f);
    }

    public void increaseZoom() {
        zoomLevel = Math.min(10, zoomLevel + 0.5f);

    }
}