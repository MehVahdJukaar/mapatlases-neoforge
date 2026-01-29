
package pepjebs.mapatlases.map_collection;

import net.minecraft.world.entity.player.Player;
import pepjebs.mapatlases.utils.Slice;

import java.util.Objects;

public record MapSearchKey(int mapX, int mapZ, Slice slice) {

    public boolean isSameSlice(Slice slice) {
        return Objects.equals(slice, this.slice);
    }

    public static MapSearchKey at(byte scale, double px, double pz, Slice slice) {
        //map code
        int i = 128 * (1 << scale);
        var center = slice.type().getCenter(px, pz, i);
        return new MapSearchKey(center.x(), center.z(), slice);
    }

    public static MapSearchKey at(byte scale, Player player, Slice slice) {
        double px = player.getX();
        double pz = player.getZ();
        return at(scale, px, pz, slice);
    }

}