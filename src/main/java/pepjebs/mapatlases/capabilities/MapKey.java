
package pepjebs.mapatlases.capabilities;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record MapKey(ResourceKey<Level> dimension, int mapX, int mapZ, @Nullable Integer slice) {
    public boolean isSameDimSameSlice(ResourceKey<Level> dimension, Integer slice) {
        return this.dimension.equals(dimension) && Objects.equals(slice, this.slice);
    }

    public static MapKey at(byte scale, Player player, @Nullable Integer slice) {
        double px = player.getX();
        double pz = player.getZ();
        //map code
        int i = 128 * (1 << scale);
        int j = Mth.floor((px + 64.0D) / i);
        int k = Mth.floor((pz + 64.0D) / i);
        int mapCenterX = j * i + i / 2 - 64;
        int mapCenterZ = k * i + i / 2 - 64;
        return new MapKey(player.level.dimension(), mapCenterX, mapCenterZ, slice);
    }
}