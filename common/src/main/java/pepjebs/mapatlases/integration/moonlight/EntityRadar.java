package pepjebs.mapatlases.integration.moonlight;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.mehvahdjukaar.moonlight.api.map.MapDataRegistry;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecorationType;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapMarker;
import net.mehvahdjukaar.moonlight.api.misc.HolderReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.PlatStuff;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class EntityRadar {

    private static final HolderReference<MLMapDecorationType<?, ?>> PASSIVE_PIN =
            HolderReference.of(MapAtlasesMod.res("passive_entity"), MapDataRegistry.REGISTRY_KEY);
    private static final HolderReference<MLMapDecorationType<?, ?>> HOSTILE_PIN =
            HolderReference.of(MapAtlasesMod.res("hostile_entity"), MapDataRegistry.REGISTRY_KEY);
    private static final HolderReference<MLMapDecorationType<?, ?>> NEUTRAL_PIN =
            HolderReference.of(MapAtlasesMod.res("neutral_entity"), MapDataRegistry.REGISTRY_KEY);
    private static final HolderReference<MLMapDecorationType<?, ?>> BOSS_PIN =
            HolderReference.of(MapAtlasesMod.res("boss_entity"), MapDataRegistry.REGISTRY_KEY);

    private static final WeakHashMap<Level, Set<MLMapMarker<?>>> nearbyEntityMarkers = new WeakHashMap<>();

    // we dont clear as just bosses use tags...too bad
    private static final Map<Class<? extends LivingEntity>, HolderReference<MLMapDecorationType<?, ?>>> entityTypeMap = new Object2ObjectOpenHashMap<>();

    public static void onClientTick(Player player) {
        Level level = player.level();

        var set = nearbyEntityMarkers.computeIfAbsent(level, k -> new HashSet<>());
        set.clear();

        Integer pValue = MapAtlasesClientConfig.radarRadius.get();
        var entities = level.getEntitiesOfClass(LivingEntity.class, new AABB(player.blockPosition())
                .inflate(pValue, 30, pValue).move(0, 2, 0));
        for (var e : entities) {
            if (e == player) continue;
            var typeHolder = getMarkerForType(e);
            if (typeHolder == null) continue;
            var type = typeHolder.getHolder(level);
            if (type != null) {
                EntityPinMarker marker = new EntityPinMarker(type, e);
                if (marker instanceof EntityPinMarker m) {
                    set.add(marker);
                }
            }
        }
    }


    @Nullable
    public static HolderReference<MLMapDecorationType<?, ?>> getMarkerForType(LivingEntity entity) {
        return entityTypeMap.computeIfAbsent(entity.getClass(), clazz -> {
            EntityType<?> type = entity.getType();
            if (type == EntityType.PLAYER)
                return null;
            if (PlatStuff.isBoss(type))
                return BOSS_PIN;
            if (entity instanceof Enemy)
                return HOSTILE_PIN;
            if (entity instanceof NeutralMob)
                return NEUTRAL_PIN;
            if (entity instanceof Animal) {
                return PASSIVE_PIN;
            }
            //excludes armor stands and such
            return null;
        });
    }


    public static Set<MLMapMarker<?>> send(MapId id, MapItemSavedData data) {
        ClientLevel level = Minecraft.getInstance().level;
        if (data.dimension.equals(level.dimension())) {
            return nearbyEntityMarkers.computeIfAbsent(level, j -> new HashSet<>());
        }
        return Set.of();
    }

    public static void unloadLevel() {
        nearbyEntityMarkers.clear();
    }


}
