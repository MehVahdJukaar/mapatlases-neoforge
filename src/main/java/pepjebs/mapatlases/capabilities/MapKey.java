
package pepjebs.mapatlases.capabilities;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.SupplementariesCompat;
import pepjebs.mapatlases.utils.Slice;
import twilightforest.item.MagicMapItem;

import java.util.Objects;

public record MapKey(ResourceKey<Level> dimension, int mapX, int mapZ, Slice slice) {
    public boolean isSameDimSameSlice(ResourceKey<Level> dimension, Slice slice) {
        return this.dimension.equals(dimension) && Objects.equals(slice, this.slice);
    }

    @Deprecated(forRemoval = true)
    public static MapKey of(String s, MapItemSavedData d) {
        var data = d.getSecond();
        return new MapKey(data.dimension, data.centerX, data.centerZ, Slice.of(d));
    }

    public static MapKey at(byte scale, double px, double pz, ResourceKey<Level> dimension, Slice slice) {
        //map code
        int i = 128 * (1 << scale);
        var center = slice.type().getCenter(px, pz, i);
        return new MapKey(dimension, center.x(), center.z(),  slice);
    }

    public static MapKey at(byte scale, Player player, Slice slice) {
        double px = player.getX();
        double pz = player.getZ();
        return at(scale, px, pz, player.level().dimension(), slice);
    }

}