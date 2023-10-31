
package pepjebs.mapatlases.capabilities;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import pepjebs.mapatlases.utils.Slice;

import java.util.Objects;

public record MapKey(ResourceKey<Level> dimension, int mapX, int mapZ, Slice slice) {
    public boolean isSameDimSameSlice(ResourceKey<Level> dimension, Slice slice) {
        return this.dimension.equals(dimension) && Objects.equals(slice, this.slice);
    }

    public static MapKey at(byte scale, double px, double pz, ResourceKey<Level> dimension, Slice slice) {
        //map code
        int i = 128 * (1 << scale);
        var center = slice.type().getCenter(px, pz, i);
        return new MapKey(dimension, center.x(), center.z(), slice);
    }

    public static MapKey at(byte scale, Player player, Slice slice) {
        double px = player.getX();
        double pz = player.getZ();
        return at(scale, px, pz, player.level().dimension(), slice);
    }

}