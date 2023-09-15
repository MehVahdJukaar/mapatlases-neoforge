package pepjebs.mapatlases.client.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.datafixers.util.Pair;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.capabilities.MapCollectionCap;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

public class MapAtlasesHUD implements IGuiOverlay {

    public static final ResourceLocation MAP_BACKGROUND = MapAtlasesMod.res("textures/gui/hud/map_background.png");
    public static final ResourceLocation MAP_FOREGROUND = MapAtlasesMod.res("textures/gui/hud/map_foreground.png");

    private final Minecraft mc;
    private final MapRenderer mapRenderer;

    public MapAtlasesHUD() {
        this.mc = Minecraft.getInstance();
        this.mapRenderer = mc.gameRenderer.getMapRenderer();
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


        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(mc.player);
        // Check the player for an Atlas
        if (atlas.isEmpty()) return;

        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;

        MapCollectionCap maps = MapAtlasItem.getMaps(atlas, level);

        Pair<String, MapItemSavedData> activeMap = maps.getActive();
        if (activeMap == null) return;

        MapItemSavedData state = activeMap.getSecond();
        String curMapId = activeMap.getFirst();

        PoseStack poseStack = graphics.pose();

        // Update client current map id
        // TODO: dont like this sound here. should be in tick instead
        // playSoundIfMapChanged(curMapId, level, player);
        // Set zoom-level for map icons
        MapAtlasesClient.setWorldMapZoomLevel((float) (double) MapAtlasesClientConfig.miniMapDecorationScale.get());
        // Draw map background
        int mapBgScaledSize = (int) Math.floor(
                MapAtlasesClientConfig.forceMiniMapScaling.get() / 100.0 * screenHeight);
        double drawnMapBufferSize = mapBgScaledSize / 20.0;
        int mapDataScaledSize = (int) (mapBgScaledSize - (2 * drawnMapBufferSize));
        float mapDataScale = mapDataScaledSize / 128.0f;
        var anchorLocation = MapAtlasesClientConfig.miniMapAnchoring.get();
        int x = anchorLocation.isLeft ? 0 : screenWidth - mapBgScaledSize;
        int y = !anchorLocation.isUp ? screenHeight - mapBgScaledSize : 0;
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
        graphics.blit(MAP_BACKGROUND, x, y, 0, 0, mapBgScaledSize, mapBgScaledSize, mapBgScaledSize, mapBgScaledSize);

        // Draw map data
        MultiBufferSource.BufferSource vcp;
        vcp = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        poseStack.pushPose();
        poseStack.translate(x + drawnMapBufferSize, y + drawnMapBufferSize, 0.0);
        poseStack.scale(mapDataScale, mapDataScale, -1);
        mapRenderer.render(
                poseStack,
                vcp,
                MapAtlasesAccessUtils.getMapIntFromString(curMapId),
                state,
                false,
                LightTexture.FULL_BRIGHT
        );
        vcp.endBatch();
        poseStack.popPose();
        graphics.blit(MAP_FOREGROUND, x, y, 0, 0, mapBgScaledSize, mapBgScaledSize, mapBgScaledSize, mapBgScaledSize);

        // Draw text data
        float textScaling = (float) (double) MapAtlasesClientConfig.minimapCoordsAndBiomeScale.get();
        int textHeightOffset = mapBgScaledSize + 4;
        int textWidthOffset = mapBgScaledSize;
        if (!anchorLocation.isUp) {
            textHeightOffset = (int) (-24 * textScaling);
        }
        if (MapAtlasesClientConfig.drawMinimapCoords.get()) {
            drawMapComponentCoords(
                    graphics, x, y, textWidthOffset, textHeightOffset,
                    textScaling, new BlockPos(new Vec3i(
                            towardsZero(player.position().x),
                            towardsZero(player.position().y),
                            towardsZero(player.position().z))));
            textHeightOffset += (12 * textScaling);
        }
        if (MapAtlasesClientConfig.drawMinimapBiome.get()) {
            drawMapComponentBiome(
                    graphics, x, y, textWidthOffset, textHeightOffset,
                    textScaling, player.blockPosition(), level);
        }
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
            int x, int y,
            int originOffsetWidth,
            int originOffsetHeight,
            float textScaling,
            BlockPos blockPos
    ) {
        String coordsToDisplay = blockPos.toShortString();
        drawScaledComponent(context, x, y, coordsToDisplay, textScaling, originOffsetWidth, originOffsetHeight);
    }

    public static void drawMapComponentBiome(
            GuiGraphics context,
            int x, int y,
            int originOffsetWidth,
            int originOffsetHeight,
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
        drawScaledComponent(context, x, y, biomeToDisplay, textScaling, originOffsetWidth, originOffsetHeight);
    }

    public static void drawScaledComponent(
            GuiGraphics context,
            int x, int y,
            String text,
            float textScaling,
            int originOffsetWidth,
            int originOffsetHeight
    ) {
        Minecraft mc = Minecraft.getInstance();
        PoseStack pose = context.pose();
        float textWidth = mc.font.width(text) * textScaling;
        float textX = (float) (x + (originOffsetWidth / 2.0) - (textWidth / 2.0));
        float textY = y + originOffsetHeight;
        if (textX + textWidth >= mc.getWindow().getGuiScaledWidth()) {
            textX = mc.getWindow().getGuiScaledWidth() - textWidth;
        }
        pose.pushPose();
        pose.translate(textX, textY, 5);
        pose.scale(textScaling, textScaling, 1);
        // uses slightly lighter drop shadow
        context.drawString(mc.font, text, 1, 1, 0x595959, false);
        context.drawString(mc.font, text, 0, 0, 0xE0E0E0, false);
        pose.popPose();
    }

}
