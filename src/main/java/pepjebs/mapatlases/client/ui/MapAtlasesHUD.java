package pepjebs.mapatlases.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
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

    public Vector4f transformPos(double mouseX, double mouseZ) {
        return scaleVector(mouseX, mouseZ, globalScale, BG_SIZE, BG_SIZE);
    }

    public static Vector4f scaleVector(double mouseX, double mouseZ, float scale, int w, int h) {
        Matrix4f matrix4f = new Matrix4f();
    matrix4f.setIdentity();
        // Calculate the translation and scaling factors
        float translateX = w / 2.0F;
        float translateY = h / 2.0F;
        // Apply translation to the matrix (combined)
        matrix4f.multiplyWithTranslation(translateX, translateY, 0);

        // Apply scaling to the matrix
        matrix4f.multiply(Matrix4f.createScaleMatrix(scale, scale, scale));

        // Apply translation back to the original position (combined)
        matrix4f.multiplyWithTranslation(-translateX, -translateY, 0);

        // Create a vector with the input coordinates
        Vector4f v = new Vector4f((float) mouseX,(float) mouseZ, 0, 1.0F);

        // Apply the transformation matrix to the vector
        v.transform(matrix4f);
        return v;
    }


    @Override
    protected void applyScissors(PoseStack poseStack, int x, int y, int x1, int y1) {
        var v = transformPos(x, y);
        var v2 = transformPos(x1, y1);
        super.applyScissors(poseStack, (int) v.x(), (int) v.y(), (int) v2.x(), (int) v2.y());
    }

    @Override
    public void render(ForgeGui forgeGui, PoseStack poseStack, float partialTick,
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

        RenderSystem.setShaderTexture(0, MAP_BACKGROUND);
        GuiComponent.blit(poseStack, x, y, BG_SIZE, BG_SIZE, 0, 0,
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
        drawAtlas(poseStack, x + (BG_SIZE - mapWidgetSize) / 2, y + (BG_SIZE - mapWidgetSize) / 2,
                mapWidgetSize, mapWidgetSize, player,
                zoomLevel * (float) (double) MapAtlasesClientConfig.miniMapZoomMultiplier.get(),
                MapAtlasesClientConfig.miniMapBorder.get());

        MapAtlasesClient.setDecorationsScale(1);
        if (rotatesWithPlayer) {
            MapAtlasesClient.setDecorationRotation(0);
        }

        if (rotatesWithPlayer) {
            RenderSystem.setShaderTexture(0, MAP_ICON_TEXTURE);
            GuiComponent.blit(poseStack, x + mapWidgetSize / 2 - 1,
                    y + mapWidgetSize / 2 - 1,
                    0, 0, 8, 8, 128, 128);
        }
        //always render as its better
        poseStack.pushPose();
        poseStack.translate(x + mapWidgetSize / 2f + 3f, y + mapWidgetSize / 2f + 3, 0);
        if (!rotatesWithPlayer) {
            poseStack.mulPose(Vector3f.ZN.rotationDegrees(180 - yRot));
        }
        poseStack.translate(-4.5f, -4f, 0);
        RenderSystem.setShaderTexture(0,MAP_ICON_TEXTURE);
        this.blit(poseStack, 0,
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
                    poseStack, font, x, y + textHeightOffset, BG_SIZE,
                    textScaling, new BlockPos(new Vec3i(
                            towardsZero(player.position().x),
                            towardsZero(player.position().y),
                            towardsZero(player.position().z))));
            textHeightOffset += (10 * textScaling);
        }
        if (MapAtlasesClientConfig.drawMinimapBiome.get()) {
            drawMapComponentBiome(
                    poseStack, font, x, y + textHeightOffset, BG_SIZE,
                    textScaling, player.blockPosition(), level);
        }

        poseStack.pushPose();
        poseStack.translate(x + BG_SIZE / 2f, y + BG_SIZE / 2f, 5);


        var p = getDirectionPos(BG_SIZE / 2f - 3, rotatesWithPlayer ? yRot : 180);
        float a = p.getFirst();
        float b = p.getSecond();
        drawLetter(poseStack, font, a, b, "N");
        if (!MapAtlasesClientConfig.miniMapOnlyNorth.get()) {
            drawLetter(poseStack, font, -a, -b, "S");
            drawLetter(poseStack, font, -b, a, "E");
            drawLetter(poseStack, font, b, -a, "W");
        }

        poseStack.popPose();


        poseStack.popPose();
    }

    private void drawLetter(PoseStack pose, Font font, float a, float b, String letter) {
        pose.pushPose();
        float scale = (float) (double) MapAtlasesClientConfig.miniMapCardinalsScale.get() / globalScale;
        pose.scale(scale, scale, 1);
        drawStringWithLighterShadow(pose, font, letter, a / scale - font.width(letter) / 2f,
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
            PoseStack context,
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
            PoseStack context,
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
            PoseStack pose,
            Font font,
            int x, int y,
            String text,
            float textScaling,
            int targetWidth
    ) {
        float textWidth = font.width(text);

        float scale = Math.min(1, targetWidth * textScaling / textWidth);
        scale *= textScaling;

        float centerX = x + targetWidth / 2f;

        pose.pushPose();
        pose.translate(centerX, y + 4, 5);
        pose.scale(scale, scale, 1);
        pose.translate(-(textWidth) / 2f, -4, 0);
        // uses slightly lighter drop shadow
        drawStringWithLighterShadow(pose, font, text, 0, 0);
        pose.popPose();
    }

    private static void drawStringWithLighterShadow(PoseStack pose, Font font, String text, float x, float y) {
        font.draw(pose,text, x + 1, y + 1, 0x595959);
        font.draw(pose, text, x, y, 0xE0E0E0);
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