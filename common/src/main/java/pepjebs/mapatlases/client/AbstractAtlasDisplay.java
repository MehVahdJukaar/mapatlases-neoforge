package pepjebs.mapatlases.client;

import net.mehvahdjukaar.moonlight.api.map.ExpandedMapData;
import net.mehvahdjukaar.moonlight.api.map.client.MLDecorationRenderState;
import net.mehvahdjukaar.moonlight.api.map.client.MapDecorationClientManager;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecoration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.utils.AtlasMap;
import pepjebs.mapatlases.utils.MapType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public abstract class AbstractAtlasDisplay {

    public static final int MAP_DIMENSION = 128;

    //internally controls how many maps are displayed
    protected final int mapsPerSide;
    protected int mapBlocksSize;
    protected AtlasMap mapWherePlayerIs;

    protected boolean followingPlayer = true;
    protected double currentXCenter;
    protected double currentZCenter;
    protected float zoomLevel = 3;

    protected boolean rotatesWithPlayer = false;
    protected boolean drawBigPlayerMarker = true;
    protected float decorationScale = 1;
    protected float decorationTextScale = 1;
    protected float decorationRotation = 0;

    protected AbstractAtlasDisplay(int mapsPerSide) {
        this.mapsPerSide = mapsPerSide;
    }

    protected void initialize(AtlasMap newCenter) {
        if (mapWherePlayerIs == null || !mapWherePlayerIs.slice.isSameGroup(newCenter.slice)) {
            this.zoomLevel = mapsPerSide * newCenter.type.getDefaultZoomFactor();
        }
        this.mapWherePlayerIs = newCenter;
        this.mapBlocksSize = (1 << mapWherePlayerIs.data.scale) * MAP_DIMENSION;

        this.currentXCenter = mapWherePlayerIs.data.centerX;
        this.currentZCenter = mapWherePlayerIs.data.centerZ;
    }

    public void drawAtlas(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                          float zoomLevelDim, MapType type, int tint, @Nullable MapItemSavedData selectedData) {

        Player player = Minecraft.getInstance().player;

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();

        float widgetScale = width / (float) (mapsPerSide * MAP_DIMENSION);
        float zoomScale = mapsPerSide / zoomLevelDim;

        int intXCenter = (int) (currentXCenter);
        int intZCenter = (int) (currentZCenter);
        int scaleIndex = mapBlocksSize / MAP_DIMENSION;

        ColumnPos c = type.getCenter(intXCenter, intZCenter, mapBlocksSize);
        int centerMapX = c.x();
        int centerMapZ = c.z();

        graphics.enableScissor(x, y, x + width, y + height);

        //translate to center
        pose.translate(x + width / 2f, y + height / 2f);
        //widget scale + zoom
        pose.scale(widgetScale * zoomScale);

        double mapCenterOffsetX = currentXCenter - centerMapX;
        double mapCenterOffsetZ = currentZCenter - centerMapZ;

        //zoom leve is essentially maps on screen
        //dont ask me why all this stuff is like that

        if (rotatesWithPlayer) {
            pose.rotate((180 - player.getYRot()) * Mth.DEG_TO_RAD);
        }
        pose.translate((float) (-mapCenterOffsetX / scaleIndex), (float) (-mapCenterOffsetZ / scaleIndex));

        //grid side len
        double sideLength = mapBlocksSize * zoomScale;
        //radius of widget
        int radius = (int) (mapBlocksSize * mapsPerSide * 0.71f); // radius using hyp

        // Calculate the distance from the circle's center to the center of each grid square
        int o = Mth.ceil(zoomLevelDim);
        double maxDist = rotatesWithPlayer ?
                Mth.square(radius + (sideLength * 0.71)) :
                (o + 1) * sideLength * 0.5;

        for (int i = o; i >= -o; i--) {
            for (int j = o; j >= -o; j--) {
                double gridCenterI = i * sideLength;
                double gridCenterJ = j * sideLength;

                boolean shouldDraw;
                // Calculate the distance between the grid square center and the circle's center
                if (rotatesWithPlayer) {
                    double distance = Mth.lengthSquared(
                            gridCenterI - mapCenterOffsetZ * zoomScale,
                            gridCenterJ - mapCenterOffsetX * zoomScale);
                    //circle dist
                    shouldDraw = (distance <= maxDist);
                } else {
                    //square dist
                    shouldDraw = Math.abs(gridCenterI - mapCenterOffsetZ * zoomScale) < maxDist &&
                            Math.abs(gridCenterJ - mapCenterOffsetX * zoomScale) < maxDist;
                }
                if (shouldDraw) {
                    AtlasMap state = getMapWithCenter(centerMapX + (j * mapBlocksSize), centerMapZ + (i * mapBlocksSize));
                    if (state != null) {
                        drawMap(graphics, player, i, j, state, tint, state.data == selectedData);
                    }
                }
            }
        }

        pose.popMatrix();
        graphics.disableScissor();
    }

    protected abstract boolean showMapBackground();

    protected abstract boolean showMapBorders();

    @Nullable
    public abstract AtlasMap getMapWithCenter(int centerX, int centerZ);

    public void setFollowingPlayer(boolean followingPlayer) {
        this.followingPlayer = followingPlayer;
    }

    private void drawMap(GuiGraphicsExtractor graphics, Player player, int ix, int iy, AtlasMap map, int tint, boolean hovered) {
        int curMapComponentX = (MAP_DIMENSION * iy) - MAP_DIMENSION / 2;
        int curMapComponentY = (MAP_DIMENSION * ix) - MAP_DIMENSION / 2;
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(curMapComponentX, curMapComponentY);

        boolean borders = showMapBorders();
        //using block atlas sprites so we get mipmaps. cant use gui sprites
        if (borders && showMapBackground()) blitMapSprite(graphics, MapAtlasesClient.MAP_BACKGROUND_TEXTURE);

        MapItemSavedData data = map.data;
        tint = MapAtlasesClient.debugIsMapUpdated(tint, map.id, map.type);
        Identifier texture = Minecraft.getInstance().getMapTextureManager().prepareMapTexture(map.id, data);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0, 0,
                MAP_DIMENSION, MAP_DIMENSION, MAP_DIMENSION, MAP_DIMENSION, tint);
        drawDecorations(graphics, data, player, tint);

        if (borders) {
            blitMapSprite(graphics, MapAtlasesClient.MAP_BORDER_TEXTURE);
            if (hovered) blitMapSprite(graphics, MapAtlasesClient.MAP_HOVERED_TEXTURE);
        }
        pose.popMatrix();
    }

    private static void blitMapSprite(GuiGraphicsExtractor graphics, SpriteId sprite) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, graphics.getSprite(sprite), 0, 0, MAP_DIMENSION, MAP_DIMENSION);
    }

    // like GuiGraphicsExtractor.map but with our marker transforms and moonlight decorations
    private void drawDecorations(GuiGraphicsExtractor graphics, MapItemSavedData data, Player player, int tint) {
        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.MAP_DECORATIONS);
        for (MapDecoration deco : data.decorations.values()) {
            if (isPlayerMarker(deco)) continue;
            drawDecoration(graphics, atlas.getSprite(deco.getSpriteLocation()), deco.x(), deco.y(), deco.rot(),
                    tint, false, deco.name().orElse(null));
        }
        for (MapDecoration deco : makePlayerMarkers(data, player)) {
            drawDecoration(graphics, atlas.getSprite(deco.getSpriteLocation()), deco.x(), deco.y(), deco.rot(),
                    tint, false, null);
        }
        if (data instanceof ExpandedMapData expanded) {
            MapAtlasesClient.setIsDrawingAtlas(true);
            for (MLMapDecoration deco : expanded.ml$getCustomDecorations().values()) {
                MLDecorationRenderState state = new MLDecorationRenderState();
                if (MapDecorationClientManager.extract(deco, data, state)) {
                    drawDecoration(graphics, state.sprite, state.x, state.y, state.rot,
                            ARGB.multiply(tint, state.color), state.outline, state.name);
                }
            }
            MapAtlasesClient.setIsDrawingAtlas(false);
        }
    }

    private void drawDecoration(GuiGraphicsExtractor graphics, @Nullable TextureAtlasSprite sprite, byte x, byte y, byte rot,
                                int color, boolean outline, @Nullable Component name) {
        if (sprite == null || ARGB.alpha(color) == 0) return;
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x / 2f + 64, y / 2f + 64);
        pose.rotate((rot * 360 / 16f + decorationRotation) * Mth.DEG_TO_RAD);
        pose.scale(4 * decorationScale);
        pose.translate(-0.125f, 0.125f);
        AtlasGuiUtils.drawMarkerSprite(graphics, sprite, color, outline);
        pose.popMatrix();
        if (name != null) drawDecorationName(graphics, x, y, name, color);
    }

    private void drawDecorationName(GuiGraphicsExtractor graphics, byte x, byte y, Component name, int color) {
        Font font = Minecraft.getInstance().font;
        float width = font.width(name);
        float scale = Mth.clamp(25f / width, 0, 6f / 9f);
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x / 2f + 64 - width * scale / 2f, y / 2f + 64 + 4);
        //rotate and scale around the marker, not the text corner
        float s = width * scale / 2f;
        pose.translate(s, -4);
        pose.rotate(decorationRotation * Mth.DEG_TO_RAD);
        pose.translate(-s * decorationTextScale, 4 * decorationTextScale);
        pose.scale(scale * decorationTextScale);
        graphics.fill(-1, -1, (int) width + 1, font.lineHeight, Integer.MIN_VALUE);
        graphics.text(font, name, 0, 0, color, false);
        pose.popMatrix();
    }

    private static boolean isPlayerMarker(MapDecoration d) {
        return d.type().is(MapDecorationTypes.PLAYER) || d.type().is(MapDecorationTypes.PLAYER_OFF_MAP)
                || d.type().is(MapDecorationTypes.PLAYER_OFF_LIMITS);
    }

    // client side decorations are keyed icon-N so we cant tell whose is whose. just redraw every player
    private List<MapDecoration> makePlayerMarkers(MapItemSavedData data, Player player) {
        if (!data.dimension.equals(player.level().dimension())) return List.of();

        List<MapDecoration> markers = new ArrayList<>();
        int i = 1 << data.scale;
        int radius = MapAtlasesClientConfig.playerMarkersRadius.get();
        for (Player p : player.level().players()) {
            float f = (float) (p.getX() - data.centerX) / i;
            float f1 = (float) (p.getZ() - data.centerZ) / i;
            if (p == player) {
                if (this.drawBigPlayerMarker || data != mapWherePlayerIs.data) continue;
                //stick to the edge like vanilla off map icon
                f = Mth.clamp(f, -63, 63);
                f1 = Mth.clamp(f1, -63, 63);
            } else {
                boolean offThisMap = Math.abs(f) > 63 || Math.abs(f1) > 63;
                if (offThisMap || p.distanceToSqr(player) > radius * radius) continue;
            }

            byte b0 = (byte) ((int) ((f * 2.0F) + 0.5D));
            byte b1 = (byte) ((int) ((f1 * 2.0F) + 0.5D));
            markers.add(new MapDecoration(MapDecorationTypes.PLAYER, b0, b1, getPlayerMarkerRot(p), Optional.empty()));
        }
        return markers;
    }

    private static byte getPlayerMarkerRot(Player p) {
        float pRotation = p.getYRot();
        pRotation += pRotation < 0.0D ? -8.0D : 8.0D;
        return (byte) ((int) (pRotation * 16.0D / 360.0D));
    }

    public static int round(int num, int mod) {
        //return Math.round((float) num / mod) * mod
        int t = num % mod;
        if (t < (int) Math.floor(mod / 2.0))
            return num - t;
        else
            return num + mod - t;
    }
}
