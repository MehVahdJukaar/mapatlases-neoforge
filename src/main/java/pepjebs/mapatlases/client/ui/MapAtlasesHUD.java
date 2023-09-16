package pepjebs.mapatlases.client.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
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

    private final Minecraft mc;

    //cached stuff
    private boolean needsInit = true;
    private ItemStack currentAtlas;
    private MapKey currentMapKey = null;

    public MapAtlasesHUD() {
        super(1);
        this.mc = Minecraft.getInstance();
        this.rotatesWithPlayer = true;
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
        if (activeMap == null) return;

        MapItemSavedData state = activeMap.getSecond();

        if (needsInit) {
            needsInit = false;
            initialize(state);
        }

        PoseStack poseStack = graphics.pose();

        // Update client current map id
        // TODO: dont like this sound here. should be in tick instead
        // playSoundIfMapChanged(curMapId, level, player);
        // Set zoom-level for map icons
        MapAtlasesClient.setWorldMapZoomLevel((float) (double) MapAtlasesClientConfig.miniMapDecorationScale.get());

        int bgSize = 64;
        int mapWidgetSize = (int) (64 * 116 / 128f);

        // Draw map background
        Anchoring anchorLocation = MapAtlasesClientConfig.miniMapAnchoring.get();
        int x = anchorLocation.isLeft ? 0 : screenWidth - mapWidgetSize;
        int y = !anchorLocation.isUp ? screenHeight - mapWidgetSize : 0;
        x += MapAtlasesClientConfig.miniMapHorizontalOffset.get();
        y += MapAtlasesClientConfig.miniMapVerticalOffset.get();

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

        //idk why its sacled up 2x
        graphics.blit(MAP_BACKGROUND, x, y, bgSize, bgSize, 0, 0,
                BACKGROUND_SIZE, BACKGROUND_SIZE, BACKGROUND_SIZE, BACKGROUND_SIZE);

        // Draw map data

        poseStack.pushPose();

        if (followingPlayer) {
            currentXCenter = (float) player.getX();
            currentZCenter = (float) player.getZ();
        }

        drawAtlas(graphics, x + (bgSize - mapWidgetSize) / 2, y + (bgSize - mapWidgetSize) / 2,
                mapWidgetSize, mapWidgetSize, player,
                1 * (float) (double) MapAtlasesClientConfig.miniMapZoomMultiplier.get(),
                MapAtlasesClientConfig.miniMapBorder.get());

        if (rotatesWithPlayer) {
            graphics.blit(MAP_ICON_TEXTURE, x + mapWidgetSize / 2 - 1,
                    y + mapWidgetSize / 2 - 1,
                    0, 0, 8, 8, 128, 128);
        }

        poseStack.popPose();
        //  graphics.blit(MAP_FOREGROUND, x, y, 0, 0, mapBgScaledSize, mapBgScaledSize, mapBgScaledSize, mapBgScaledSize);
        // Draw text data
        float textScaling = (float) (double) MapAtlasesClientConfig.minimapCoordsAndBiomeScale.get();
        int textHeightOffset = bgSize + 4;
        if (!anchorLocation.isUp) {
            textHeightOffset = (int) (-24 * textScaling);
        }
        Font font = mc.font;
        if (MapAtlasesClientConfig.drawMinimapCoords.get()) {
            drawMapComponentCoords(
                    graphics, font, x, y + textHeightOffset, bgSize,
                    textScaling, new BlockPos(new Vec3i(
                            towardsZero(player.position().x),
                            towardsZero(player.position().y),
                            towardsZero(player.position().z))));
            textHeightOffset += (10 * textScaling);
        }
        if (MapAtlasesClientConfig.drawMinimapBiome.get()) {
            drawMapComponentBiome(
                    graphics, font, x, y + textHeightOffset, bgSize,
                    textScaling, player.blockPosition(), level);
        }

        poseStack.pushPose();
        poseStack.translate(x + bgSize / 2f, y + bgSize / 2f, 5);

        var p = getDirectionPos(bgSize / 2f - 3, player.getYRot());
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

    private static void drawLetter(GuiGraphics graphics, Font font, float a, float b, String letter) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        float scale = (float) (double) MapAtlasesClientConfig.miniMapCardinalsScale.get();
        pose.scale(scale, scale, 1);
        graphics.drawString(font, letter, a / scale - font.width(letter) / 2f,
                b / scale - font.lineHeight / 2f, -1, true);

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

    public static void drawMapComponentCoords(
            GuiGraphics context,
            Font font,
            int x, int y,
            int targetWidth,
            float textScaling,
            BlockPos blockPos
    ) {
        String coordsToDisplay = blockPos.toShortString();
        drawScaledComponent(context, font, x, y, coordsToDisplay, textScaling, targetWidth);
    }

    public static void drawMapComponentBiome(
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
        drawScaledComponent(context, font, x, y, biomeToDisplay, textScaling, targetWidth);
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
        context.drawString(font, text, 1, 1, 0x595959, false);
        context.drawString(font, text, 0, 0, 0xE0E0E0, false);
        pose.popPose();
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
}