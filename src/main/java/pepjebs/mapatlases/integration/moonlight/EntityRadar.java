package pepjebs.mapatlases.integration.moonlight;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.mehvahdjukaar.moonlight.api.map.MapDataRegistry;
import net.mehvahdjukaar.moonlight.api.map.markers.MapBlockMarker;
import net.mehvahdjukaar.moonlight.api.map.type.MapDecorationType;
import net.mehvahdjukaar.moonlight.api.misc.DataObjectReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class EntityRadar {

    private static final DataObjectReference<MapDecorationType<?, ?>> PASSIVE_PIN =
            new DataObjectReference<>(MapAtlasesMod.res("passive_entity"), MapDataRegistry.REGISTRY_KEY);
    private static final DataObjectReference<MapDecorationType<?, ?>> HOSTILE_PIN =
            new DataObjectReference<>(MapAtlasesMod.res("hostile_entity"), MapDataRegistry.REGISTRY_KEY);
    private static final DataObjectReference<MapDecorationType<?, ?>> NEUTRAL_PIN =
            new DataObjectReference<>(MapAtlasesMod.res("neutral_entity"), MapDataRegistry.REGISTRY_KEY);
    private static final DataObjectReference<MapDecorationType<?, ?>> BOSS_PIN =
            new DataObjectReference<>(MapAtlasesMod.res("boss_entity"), MapDataRegistry.REGISTRY_KEY);

    private static final WeakHashMap<Level, Set<MapBlockMarker<?>>> nearbyEntityMarkers = new WeakHashMap<>();

    // we dont clear as just bosses use tags...too bad
    private static final Map<Class<? extends LivingEntity>, DataObjectReference<MapDecorationType<?, ?>>> entityTypeMap = new Object2ObjectOpenHashMap<>();

    public static void onClientTick(Player player) {
        Level level = player.level();

        var set = nearbyEntityMarkers.computeIfAbsent(level, k -> new HashSet<>());
        set.clear();

        Integer pValue = MapAtlasesClientConfig.radarRadius.get();
        var entities = level.getEntitiesOfClass(LivingEntity.class, new AABB(player.blockPosition())
                .inflate(pValue, 30, pValue).move(0, 2, 0));
        for (var e : entities) {
            if (e == player) continue;
            var type = getMarkerForType(e);
            if (type != null) {
                MapBlockMarker<?> marker = type.get().createEmptyMarker();
                if (marker instanceof EntityPinMarker m) {
                    m.setPos(new BlockPos(e.getBlockX(), e.getBlockY(), e.getBlockZ()));
                    m.setEntity(e);
                    set.add(marker);
                }
            }
        }
    }


    @Nullable
    public static DataObjectReference<MapDecorationType<?, ?>> getMarkerForType(LivingEntity entity) {
        return entityTypeMap.computeIfAbsent(entity.getClass(), clazz -> {
            EntityType<?> type = entity.getType();
            if (type == EntityType.PLAYER)
                return null;
            if (type.is(Tags.EntityTypes.BOSSES))
                return BOSS_PIN;
            if (entity instanceof Enemy)
                return HOSTILE_PIN;
            if (entity instanceof NeutralMob)
                return NEUTRAL_PIN;
            return PASSIVE_PIN;
        });
    }

    public static Set<MapBlockMarker<?>> send(Integer integer, MapItemSavedData data) {
        ClientLevel level = Minecraft.getInstance().level;
        if (data.dimension.equals(level.dimension())) {
            return nearbyEntityMarkers.computeIfAbsent(level, j->new HashSet<>());
        }
        return Set.of();
    }

    public static void unloadLevel() {
        nearbyEntityMarkers.clear();
    }
}
