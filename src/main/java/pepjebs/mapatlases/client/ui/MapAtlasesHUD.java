package pepjebs.mapatlases.client.ui;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.AbstractAtlasWidget;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtilsOld;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.Arrays;

public class MapAtlasesHUD   implements IGuiOverlay {

    public static final ResourceLocation MAP_BACKGROUND = MapAtlasesMod.res("textures/gui/hud/map_background.png");
    public static final ResourceLocation MAP_FOREGROUND = MapAtlasesMod.res("textures/gui/hud/map_foreground.png");

    @Nullable
    private static String localPlayerCurrentMapId = null;

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
        ItemStack atlas = MapAtlasesAccessUtilsOld.getAtlasFromPlayerByConfig(mc.player);
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
    public void render(ForgeGui forgeGui, GuiGraphics graphics, float partialTick,
                       int screenWidth, int screenHeight) {
        if (!shouldDraw()) return;
        String curMapId = MapAtlasesClient.getActiveMap();
        ClientLevel level = mc.level;
        MapItemSavedData state = level.getMapData(curMapId);
        if (state == null) return;

        PoseStack matrices = graphics.pose();

        // Update client current map id
        LocalPlayer player = mc.player;
        // TODO: dont like this sound here. should be in tick instead
        playSoundIfMapChanged(curMapId, level, player);
        // Set zoom-level for map icons
        MapAtlasesClient.setWorldMapZoomLevel((float) (double) MapAtlasesClientConfig.miniMapDecorationScale.get());
        // Draw map background
        Window window = mc.getWindow();
        int mapBgScaledSize = (int) Math.floor(
                MapAtlasesClientConfig.forceMiniMapScaling.get() / 100.0 * window.getGuiScaledHeight());
        double drawnMapBufferSize = mapBgScaledSize / 20.0;
        int mapDataScaledSize = (int) (mapBgScaledSize - (2 * drawnMapBufferSize));
        float mapDataScale = mapDataScaledSize / 128.0f;
        var anchorLocation = MapAtlasesClientConfig.miniMapAnchoring.get();
        int x = anchorLocation.isLeft ? 0 : window.getGuiScaledWidth() - mapBgScaledSize;
        int y = !anchorLocation.isUp ? window.getGuiScaledHeight() - mapBgScaledSize : 0;
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
        matrices.pushPose();
        matrices.translate(x + drawnMapBufferSize, y + drawnMapBufferSize, 0.0);
        matrices.scale(mapDataScale, mapDataScale, -1);
        mapRenderer.render(
                matrices,
                vcp,
                MapAtlasesAccessUtils.getMapIntFromString(curMapId),
                state,
                false,
                LightTexture.FULL_BRIGHT
        );
        vcp.endBatch();
        matrices.popPose();
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
        if (!curMapId.equals(localPlayerCurrentMapId)) {
            if (localPlayerCurrentMapId != null) {
                level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                        MapAtlasesMod.ATLAS_PAGE_TURN_SOUND_EVENT.get(), SoundSource.PLAYERS,
                        (float) (double) MapAtlasesClientConfig.soundScalar.get(), 1.0F, false);
            }
            localPlayerCurrentMapId = curMapId;
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
