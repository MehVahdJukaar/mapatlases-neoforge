package pepjebs.mapatlases.integration.moonlight;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.PlatStuff;
import pepjebs.mapatlases.client.AbstractAtlasWidget;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

public class EntityRadar {
    private static final int YELLOW_PIN_INDEX = 3;
    private static final Identifier PASSIVE_TEXTURE = MapAtlasesMod.res("textures/map_marker/passive_entity.png");
    private static final Identifier HOSTILE_TEXTURE = MapAtlasesMod.res("textures/map_marker/hostile_entity.png");
    private static final Identifier NEUTRAL_TEXTURE = MapAtlasesMod.res("textures/map_marker/neutral_entity.png");
    private static final Identifier BOSS_TEXTURE = MapAtlasesMod.res("textures/map_marker/boss_entity.png");

    private static final WeakHashMap<Level, Set<TrackedEntityMarker>> nearbyEntityMarkers = new WeakHashMap<>();

    public static Set<?> send(Integer integer, MapItemSavedData data) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && data.dimension.equals(level.dimension())) {
            return nearbyEntityMarkers.computeIfAbsent(level, ignored -> new HashSet<>());
        }
        return Set.of();
    }

    public static void onClientTick(Player player) {
        Level level = player.level();
        Set<TrackedEntityMarker> markers = nearbyEntityMarkers.computeIfAbsent(level, ignored -> new HashSet<>());
        markers.clear();

        int radius = MapAtlasesClientConfig.radarRadius.get();
        for (LivingEntity entity : level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(player.blockPosition()).inflate(radius, 30, radius).move(0, 2, 0))) {
            if (entity == player) {
                continue;
            }
            MarkerKind kind = getMarkerKind(entity);
            if (kind != null) {
                markers.add(new TrackedEntityMarker(entity, kind));
            }
        }
    }

    public static void renderMapMarkers(GuiGraphicsExtractor graphics, MapDataHolder holder, Player player) {
        if (!MapAtlasesConfig.entityRadar.get() || !MapAtlasesClientConfig.entityRadar.get()) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !holder.data.dimension.equals(level.dimension())) {
            return;
        }

        Matrix3x2fStack pose = graphics.pose();
        float scale = pepjebs.mapatlases.client.MapAtlasesClient.getDecorationsScale();
        for (TrackedEntityMarker marker : nearbyEntityMarkers.computeIfAbsent(level, ignored -> new HashSet<>())) {
            Entity entity = marker.entity();
            if (entity == null || entity.isRemoved() || entity.level() != level) {
                continue;
            }

            int scaleFactor = 1 << holder.data.scale;
            float mapOffsetX = (float) (entity.getX() - holder.data.centerX) / scaleFactor;
            float mapOffsetY = (float) (entity.getZ() - holder.data.centerZ) / scaleFactor;
            if (mapOffsetX < -64.0F || mapOffsetY < -64.0F || mapOffsetX > 64.0F || mapOffsetY > 64.0F) {
                continue;
            }

            float renderX = AbstractAtlasWidget.MAP_DIMENSION / 2f + mapOffsetX;
            float renderY = AbstractAtlasWidget.MAP_DIMENSION / 2f + mapOffsetY;
            float rotationDegrees = MapAtlasesClientConfig.radarRotation.get() ? entity.getYRot() + 180.0F : 0.0F;
            int alpha = getAlpha(entity, player);
            if (alpha <= 0) {
                continue;
            }

            pose.pushMatrix();
            pose.translate(renderX, renderY);
            if (rotationDegrees != 0.0F) {
                pose.rotate((float) Math.toRadians(rotationDegrees));
            }
            pose.scale(scale, scale);
            pose.translate(-4f, -4f);
            graphics.nextStratum();
            graphics.blit(RenderPipelines.GUI_TEXTURED, getTexture(marker.kind()),
                    0, 0, 0, 0, 8, 8, 8, 8, withAlpha(alpha));
            pose.popMatrix();
        }
    }

    public static void unloadLevel() {
        nearbyEntityMarkers.clear();
    }

    @Nullable
    private static MarkerKind getMarkerKind(LivingEntity entity) {
        EntityType<?> type = entity.getType();
        if (type == EntityType.PLAYER) {
            return null;
        }
        if (PlatStuff.isBoss(type)) {
            return MarkerKind.BOSS;
        }
        if (entity instanceof Enemy) {
            return MarkerKind.HOSTILE;
        }
        if (entity instanceof NeutralMob) {
            return MarkerKind.NEUTRAL;
        }
        if (entity instanceof Animal) {
            return MarkerKind.PASSIVE;
        }
        return null;
    }

    private static Identifier getTexture(MarkerKind kind) {
        if (MapAtlasesClientConfig.radarColor.get()) {
            return ClientMarkers.getPinTexture(YELLOW_PIN_INDEX, false);
        }
        return switch (kind) {
            case PASSIVE -> PASSIVE_TEXTURE;
            case HOSTILE -> HOSTILE_TEXTURE;
            case NEUTRAL -> NEUTRAL_TEXTURE;
            case BOSS -> BOSS_TEXTURE;
        };
    }

    private static int getAlpha(Entity entity, Player player) {
        double diff = Math.abs(entity.getY() - player.getY()) / 15.0;
        double cubic = diff * diff * diff;
        return (int) Math.max(0, 255 * (1 - cubic));
    }

    private static int withAlpha(int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | 0x00FFFFFF;
    }

    private enum MarkerKind {
        PASSIVE,
        HOSTILE,
        NEUTRAL,
        BOSS
    }

    private record TrackedEntityMarker(Entity entity, MarkerKind kind) {
    }
}
