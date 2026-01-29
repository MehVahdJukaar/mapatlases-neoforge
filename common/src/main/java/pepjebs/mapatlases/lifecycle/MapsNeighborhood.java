package pepjebs.mapatlases.lifecycle;

import net.minecraft.world.entity.player.Player;
import pepjebs.mapatlases.map_collection.MapGridKey;
import pepjebs.mapatlases.utils.Slice;

import java.util.HashSet;
import java.util.Set;

public final class MapsNeighborhood {

    private final Set<MapGridKey> around;
    private final MapGridKey center;

    private MapsNeighborhood(Set<MapGridKey> around, MapGridKey center) {
        this.around = around;
        this.center = center;
    }

    public static MapsNeighborhood around(Player player,
                                          byte scale,
                                          Slice slice) {

        MapGridKey centerKey = MapGridKey.atEntityPosition(scale, slice, player);

        Set<MapGridKey> around = computeDiscoveryNeighbors(
                centerKey,
                player.getBlockX(),
                player.getBlockZ(),
                slice.getDiscoveryReach()
        );
        around.add(centerKey);

        return new MapsNeighborhood(around, centerKey);
    }

    public Iterable<MapGridKey> all() {
        return around;
    }

    public MapGridKey center() {
        return center;
    }


    private static Set<MapGridKey> computeDiscoveryNeighbors(
            MapGridKey center,
            int playerX,
            int playerZ,
            int reach
    ) {
        int width = center.gridWidth;
        int halfWidth = width / 2;

        Set<MapGridKey> result = new HashSet<>();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {

                if (dx == 0 && dz == 0) continue;

                boolean crossX = (dx == -1 && playerX - reach <= center.mapX - halfWidth) ||
                        (dx == 1 && playerX + reach >= center.mapX + halfWidth);

                boolean crossZ = (dz == -1 && playerZ - reach <= center.mapZ - halfWidth) ||
                        (dz == 1 && playerZ + reach >= center.mapZ + halfWidth);

                if (crossX || crossZ) {
                    result.add(center.offset(dx, dz));
                }
            }
        }
        return result;
    }
}
