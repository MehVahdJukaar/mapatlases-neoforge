package pebjebs.mapatlases.client.ui;

import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import pebjebs.mapatlases.MapAtlasesMod;
import pebjebs.mapatlases.client.MapAtlasesClient;
import pebjebs.mapatlases.config.MapAtlasesClientConfig;
import pebjebs.mapatlases.item.MapAtlasItem;
import pebjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.Arrays;

public class MapAtlasesHUD implements IGuiOverlay {

    public static final ResourceLocation MAP_BACKGROUND = MapAtlasesMod.res("textures/gui/hud/map_background.png");
    public static final ResourceLocation MAP_FOREGROUND = MapAtlasesMod.res("textures/gui/hud/map_foreground.png");

    private static String currentMapId = "";

    private static Minecraft mc;
    private static MapRenderer mapRenderer;

    public MapAtlasesHUD() {
        mc = Minecraft.getInstance();
        mapRenderer = mc.gameRenderer.getMapRenderer();
    }

    private boolean shouldDraw() {
        // Handle early returns
        if (mc.level == null || mc.player == null) {
            return false;
        }
        // Check config disable
        if (!MapAtlasesClientConfig.drawMiniMapHUD.get()) return false;
        // Check F3 menu displayed
        if (mc.options.renderDebug) return false;
        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(mc.player);
        // Check the player for an Atlas
        if (atlas.isEmpty()) return false;
        // Check the client has an active map id
        if (MapAtlasesClient.getActiveMap() == null) return false;
        // Check the active map id is in the active atlas
        //TODO: remove tag access in render thread
        return atlas.getTag() != null && atlas.getTag().contains(MapAtlasItem.MAP_LIST_NBT) &&
                Arrays.stream(atlas.getTag().getIntArray(MapAtlasItem.MAP_LIST_NBT))
                        .anyMatch(i ->
                                i == MapAtlasesAccessUtils.getMapIntFromString(MapAtlasesClient.getActiveMap()));
    }

    @Override
    public void render(ForgeGui forgeGui, GuiGraphics context, float partialTick,
                       int screenWidth, int screenHeight) {
        if (!shouldDraw()) return;
        var matrices = context.pose();

        String curMapId = MapAtlasesClient.getActiveMap();
        MapItemSavedData state = mc.level.getMapData(curMapId);
        if (state == null) return;
        // Update client current map id
        if (currentMapId == null || curMapId.compareTo(currentMapId) != 0) {
            if (currentMapId != null && currentMapId.compareTo("") != 0) {
                mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        MapAtlasesMod.ATLAS_PAGE_TURN_SOUND_EVENT.get(), SoundSource.PLAYERS,
                        (float) (double) MapAtlasesClientConfig.soundScalar.get(), 1.0F, false);
            }
            currentMapId = curMapId;
        }
        // Set zoom-level for map icons
        MapAtlasesClient.setWorldMapZoomLevel((float) (double) MapAtlasesClientConfig.miniMapDecorationScale.get());
        // Draw map background
        int mapBgScaledSize = (int) Math.floor(
                MapAtlasesClientConfig.forceMiniMapScaling.get() / 100.0 * mc.getWindow().getGuiScaledHeight());
        double drawnMapBufferSize = mapBgScaledSize / 20.0;
        int mapDataScaledSize = (int) ((mapBgScaledSize - (2 * drawnMapBufferSize)));
        float mapDataScale = mapDataScaledSize / 128.0f;
        var anchorLocation = MapAtlasesClientConfig.miniMapAnchoring.get();
        int x = anchorLocation.isLeft ? 0 : mc.getWindow().getGuiScaledWidth() - mapBgScaledSize;
        int y = !anchorLocation.isUp ? mc.getWindow().getGuiScaledHeight() - mapBgScaledSize : 0;
        x += MapAtlasesClientConfig.miniMapHorizontalOffset.get();
        y += MapAtlasesClientConfig.miniMapVerticalOffset.get();

        if (anchorLocation.isUp && !anchorLocation.isLeft) {
            boolean hasBeneficial = false;
            boolean hasNegative = false;
            for (var e : mc.player.getActiveEffects()) {
                MobEffect effect = e.getEffect();
                if (effect.isBeneficial()) {
                    hasBeneficial = true;
                } else {
                    hasNegative = true;
                }
            }
            int offsetForEffects = 26;

            offsetForEffects = MapAtlasesClientConfig.activePotionVerticalOffset.get();
            if (hasNegative && y < 2 * offsetForEffects) {
                y += (2 * offsetForEffects - y);
            } else if (hasBeneficial && y < offsetForEffects) {
                y += (offsetForEffects - y);
            }
        }
        context.blit(MAP_BACKGROUND, x, y, 0, 0, mapBgScaledSize, mapBgScaledSize, mapBgScaledSize, mapBgScaledSize);

        // Draw map data
        MultiBufferSource.BufferSource vcp;
        vcp = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        matrices.pushPose();
        matrices.translate(x + drawnMapBufferSize, y + drawnMapBufferSize, 0.0);
        matrices.scale(mapDataScale, mapDataScale, -1);
        mapRenderer.render(
                matrices,
                vcp,
                MapAtlasesAccessUtils.getMapIntFromString(curMapId),
                state,
                false,
                Integer.parseInt("F000F0", 16)
        );
        vcp.endBatch();
        matrices.popPose();
        context.blit(MAP_FOREGROUND, x, y, 0, 0, mapBgScaledSize, mapBgScaledSize, mapBgScaledSize, mapBgScaledSize);

        // Draw text data
        float textScaling = (float) (double) MapAtlasesClientConfig.minimapCoordsAndBiomeScale.get();
        int textHeightOffset = mapBgScaledSize + 4;
        int textWidthOffset = mapBgScaledSize;
        if (!anchorLocation.isUp) {
            textHeightOffset = (int) (-24 * textScaling);
        }
        if (MapAtlasesClientConfig.drawMinimapCoords.get()) {
            drawMapComponentCoords(
                    context, x, y, textWidthOffset, textHeightOffset,
                    textScaling, new BlockPos(new Vec3i(
                            towardsZero(mc.player.position().x),
                            towardsZero(mc.player.position().y),
                            towardsZero(mc.player.position().z))));
            textHeightOffset += (12 * textScaling);
        }
        if (MapAtlasesClientConfig.drawMinimapBiome.get()) {
            drawMapComponentBiome(
                    context, x, y, textWidthOffset, textHeightOffset,
                    textScaling, mc.player.blockPosition(), mc.level);
        }
    }

    private static int towardsZero(double d) {
        if (d < 0.0)
            return -1 * (int) Math.floor(-1 * d);
        else
            return (int) Math.floor(d);
    }

    public static void drawMapComponentCoords(
            GuiGraphics context,
            int x,
            int y,
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
            int x,
            int y,
            int originOffsetWidth,
            int originOffsetHeight,
            float textScaling,
            BlockPos blockPos,
            Level world
    ) {
        String biomeToDisplay = getBiomeStringToDisplay(world, blockPos);
        drawScaledComponent(context, x, y, biomeToDisplay, textScaling, originOffsetWidth, originOffsetHeight);
    }

    public static void drawScaledComponent(
            GuiGraphics context,
            int x,
            int y,
            String text,
            float textScaling,
            int originOffsetWidth,
            int originOffsetHeight
    ) {
        var matrices = context.pose();
        float textWidth = mc.font.width(text) * textScaling;
        float textX = (float) (x + (originOffsetWidth / 2.0) - (textWidth / 2.0));
        float textY = y + originOffsetHeight;
        if (textX + textWidth >= mc.getWindow().getGuiScaledWidth()) {
            textX = mc.getWindow().getGuiScaledWidth() - textWidth;
        }
        matrices.pushPose();
        matrices.translate(textX, textY, 5);
        matrices.scale(textScaling, textScaling, 1);
        context.drawString(mc.font, text, 1, 1, Integer.parseInt("595959", 16), false);
        context.drawString(mc.font, text, 0, 0, Integer.parseInt("E0E0E0", 16), false);
        matrices.popPose();
    }

    private static String getBiomeStringToDisplay(Level world, BlockPos blockPos) {
        if (world != null) {
            var key = world.getBiome(blockPos).unwrapKey();
            if (key.isPresent()) {
                ResourceKey<Biome> biomeKey = key.get();
                return Component.translatable(Util.makeDescriptionId("biome", biomeKey.location())).getString();
            }
        }
        return "";
    }


}
