package pepjebs.mapatlases.client;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import pepjebs.mapatlases.integration.moonlight.ClientMarkers;
import pepjebs.mapatlases.integration.moonlight.CustomDecorationButton;
import pepjebs.mapatlases.integration.moonlight.InternalPinDecoration;
import pepjebs.mapatlases.integration.moonlight.MoonlightCompat;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.MapType;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static pepjebs.mapatlases.client.MapAtlasesClient.MAP_BORDER_TEXTURE;
import static pepjebs.mapatlases.client.MapAtlasesClient.MAP_HOVERED_TEXTURE;

public abstract class AbstractAtlasWidget {

    public static final int MAP_DIMENSION = 128;

    protected final int atlasesCount;
    protected int mapBlocksSize;
    protected MapDataHolder mapWherePlayerIs;

    protected boolean followingPlayer = true;
    protected double currentXCenter;
    protected double currentZCenter;
    protected float zoomLevel = 3;
    protected float mapRotationDegrees = 0;

    protected boolean rotatesWithPlayer = false;
    protected boolean drawBigPlayerMarker = true;

    protected AbstractAtlasWidget(int atlasesCount) {
        this.atlasesCount = atlasesCount;
    }

    protected void initialize(MapDataHolder newCenter) {
        if (mapWherePlayerIs == null || !mapWherePlayerIs.slice.isSameGroup(newCenter.slice)) {
            this.zoomLevel = atlasesCount * newCenter.type.getDefaultZoomFactor();
        }
        this.mapWherePlayerIs = newCenter;
        this.mapBlocksSize = (1 << mapWherePlayerIs.data.scale) * MAP_DIMENSION;

        this.currentXCenter = mapWherePlayerIs.data.centerX;
        this.currentZCenter = mapWherePlayerIs.data.centerZ;
    }

    public void drawAtlas(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                          Player player, float zoomLevelDim, boolean showBorders, MapType type, int light,
                          @Nullable MapItemSavedData selectedKey) {

        MapAtlasesClient.setIsDrawingAtlas(true);

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();

        float widgetScale = width / (float) (atlasesCount * MAP_DIMENSION);
        float zoomScale = atlasesCount / zoomLevelDim;

        int intXCenter = (int) currentXCenter;
        int intZCenter = (int) currentZCenter;
        int scaleIndex = mapBlocksSize / MAP_DIMENSION;

        ColumnPos c = type.getCenter(intXCenter, intZCenter, mapBlocksSize);
        int centerMapX = c.x();
        int centerMapZ = c.z();

        applyScissors(graphics, x, y, x + width, y + height);

        pose.translate(x + width / 2f, y + height / 2f);
        pose.scale(widgetScale * zoomScale, widgetScale * zoomScale);

        List<Pair<Integer, Integer>> normalBorders = new ArrayList<>();
        List<Pair<Integer, Integer>> selectedBorders = new ArrayList<>();

        double mapCenterOffsetX = currentXCenter - centerMapX;
        double mapCenterOffsetZ = currentZCenter - centerMapZ;

        if (rotatesWithPlayer) {
            pose.rotate((float) Math.toRadians(180 - player.getYRot() + mapRotationDegrees));
        } else if (mapRotationDegrees != 0) {
            pose.rotate((float) Math.toRadians(mapRotationDegrees));
        }
        pose.translate((float) (-mapCenterOffsetX / scaleIndex), (float) (-mapCenterOffsetZ / scaleIndex));

        double sideLength = mapBlocksSize * zoomScale;
        int radius = (int) (mapBlocksSize * atlasesCount * 0.71f);
        int o = Mth.ceil(zoomLevelDim);
        double maxDist = rotatesWithPlayer ?
                Mth.square(radius + (sideLength * 0.71)) :
                (o + 1) * sideLength * 0.5;

        for (int i = o; i >= -o; i--) {
            for (int j = o; j >= -o; j--) {
                double gridCenterI = i * sideLength;
                double gridCenterJ = j * sideLength;

                boolean shouldDraw;
                if (rotatesWithPlayer) {
                    double distance = Mth.lengthSquared(
                            gridCenterI - mapCenterOffsetZ * zoomScale,
                            gridCenterJ - mapCenterOffsetX * zoomScale);
                    shouldDraw = distance <= maxDist;
                } else {
                    shouldDraw = Math.abs(gridCenterI - mapCenterOffsetZ * zoomScale) < maxDist &&
                            Math.abs(gridCenterJ - mapCenterOffsetX * zoomScale) < maxDist;
                }
                if (shouldDraw) {
                    getAndDrawMap(player, graphics, centerMapX, centerMapZ, normalBorders, selectedBorders,
                            i, j, light, selectedKey);
                }
            }
        }

        if (showBorders) {
            for (var border : normalBorders) {
                graphics.nextStratum();
                graphics.blit(RenderPipelines.GUI_TEXTURED, MAP_BORDER_TEXTURE, border.getFirst(), border.getSecond(),
                        0, 0, MAP_DIMENSION, MAP_DIMENSION, MAP_DIMENSION, MAP_DIMENSION);
            }
            for (var border : selectedBorders) {
                graphics.nextStratum();
                graphics.blit(RenderPipelines.GUI_TEXTURED, MAP_HOVERED_TEXTURE, border.getFirst(), border.getSecond(),
                        0, 0, MAP_DIMENSION, MAP_DIMENSION, MAP_DIMENSION, MAP_DIMENSION);
            }
        }

        pose.popMatrix();
        graphics.disableScissor();
        MapAtlasesClient.setIsDrawingAtlas(false);
    }

    protected void applyScissors(GuiGraphicsExtractor graphics, int x, int y, int x1, int y1) {
        graphics.enableScissor(x, y, x1, y1);
    }

    private void getAndDrawMap(Player player, GuiGraphicsExtractor graphics, int centerMapX, int centerMapZ,
                               List<Pair<Integer, Integer>> normalBorders, List<Pair<Integer, Integer>> selectedBorders,
                               int i, int j, int light, @Nullable MapItemSavedData selectedData) {
        int reqXCenter = centerMapX + (j * mapBlocksSize);
        int reqZCenter = centerMapZ + (i * mapBlocksSize);
        MapDataHolder state = getMapWithCenter(reqXCenter, reqZCenter);
        if (state != null) {
            MapItemSavedData data = state.data;
            boolean drawPlayerIcons = !this.drawBigPlayerMarker && data.dimension.equals(player.level().dimension());
            this.drawMap(player, graphics, normalBorders, selectedBorders, i, j, state, drawPlayerIcons, light, selectedData);
        }
    }

    @Nullable
    public abstract MapDataHolder getMapWithCenter(int centerX, int centerZ);

    public void setFollowingPlayer(boolean followingPlayer) {
        this.followingPlayer = followingPlayer;
    }

    private void drawMap(Player player, GuiGraphicsExtractor graphics,
                         List<Pair<Integer, Integer>> normalBorders,
                         List<Pair<Integer, Integer>> selectedBorders,
                         int ix, int iy, MapDataHolder state, boolean drawPlayerIcons,
                         int light, @Nullable MapItemSavedData selectedData) {
        int curMapComponentX = (MAP_DIMENSION * iy) - MAP_DIMENSION / 2;
        int curMapComponentY = (MAP_DIMENSION * ix) - MAP_DIMENSION / 2;

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(curMapComponentX, curMapComponentY);

        MapItemSavedData data = state.data;
        Map<String, MapDecoration> decorations = MapAtlasesClient.getMutableDecorations(data);
        List<Map.Entry<String, MapDecoration>> removed = new ArrayList<>();
        List<Map.Entry<String, MapDecoration>> added = new ArrayList<>();
        for (var e : decorations.entrySet()) {
            MapDecoration dec = e.getValue();
            var type = dec.type();
            if (type.equals(MapDecorationTypes.PLAYER_OFF_MAP) || type.equals(MapDecorationTypes.PLAYER_OFF_LIMITS)) {
                if (data == mapWherePlayerIs.data && drawPlayerIcons) {
                    removed.add(e);
                    added.add(new AbstractMap.SimpleEntry<>(e.getKey(), new MapDecoration(MapDecorationTypes.PLAYER,
                            dec.x(), dec.y(), getPlayerMarkerRot(player), dec.name())));
                } else {
                    removed.add(e);
                }

            } else if (type.equals(MapDecorationTypes.PLAYER)) {
                if (!drawPlayerIcons || data != mapWherePlayerIs.data) {
                    removed.add(e);
                } else {
                    int scale = 1 << data.scale;
                    float f = (float) (player.getX() - data.centerX) / scale;
                    float f1 = (float) (player.getZ() - data.centerZ) / scale;
                    byte b0 = (byte) ((int) ((f * 2.0F) + 0.5D));
                    byte b1 = (byte) ((int) ((f1 * 2.0F) + 0.5D));
                    added.add(new AbstractMap.SimpleEntry<>(e.getKey(), new MapDecoration(MapDecorationTypes.PLAYER,
                            b0, b1, getPlayerMarkerRot(player), dec.name())));
                }
            }
        }

        removed.forEach(d -> decorations.remove(d.getKey()));
        added.forEach(d -> decorations.put(d.getKey(), d.getValue()));

        light = MapAtlasesClient.debugIsMapUpdated(light, state.stringId);

        MapRenderState renderState = new MapRenderState();
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getMapRenderer().extractRenderState(new MapId(state.id), data, renderState);
        graphics.map(renderState);
        renderCustomPins(graphics, state);

        if (state.data == selectedData) {
            selectedBorders.add(Pair.of(curMapComponentX, curMapComponentY));
        } else {
            normalBorders.add(Pair.of(curMapComponentX, curMapComponentY));
        }

        removed.forEach(d -> decorations.put(d.getKey(), d.getValue()));
        MapAtlasesClient.markDecorationsDirty(data);
        pose.popMatrix();
    }

    private void renderCustomPins(GuiGraphicsExtractor graphics, MapDataHolder state) {
        for (var decorationHolder : MoonlightCompat.getCustomDecorations(state)) {
            if (!(decorationHolder.deco() instanceof InternalPinDecoration pin)) {
                continue;
            }
            float x = MAP_DIMENSION / 2f + pin.decoration().x() / 2f;
            float y = MAP_DIMENSION / 2f + pin.decoration().y() / 2f;
            CustomDecorationButton.renderStaticMarker(graphics, pin, x, y, 1,
                    ClientMarkers.isClientDecoFocused(state, pin), 255);
        }
    }

    private static byte getPlayerMarkerRot(Player p) {
        float pRotation = p.getYRot();
        pRotation += pRotation < 0.0D ? -8.0D : 8.0D;
        return (byte) ((int) (pRotation * 16.0D / 360.0D));
    }

    public static int round(int num, int mod) {
        int t = num % mod;
        if (t < (int) Math.floor(mod / 2.0)) {
            return num - t;
        }
        return num + mod - t;
    }
}
