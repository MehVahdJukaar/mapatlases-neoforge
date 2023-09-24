
package pepjebs.mapatlases.capabilities;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.SupplementariesCompat;

import java.util.Objects;

public record MapKey(ResourceKey<Level> dimension, int mapX, int mapZ, @Nullable Integer slice) {
    public boolean isSameDimSameSlice(ResourceKey<Level> dimension, Integer slice) {
        return this.dimension.equals(dimension) && Objects.equals(slice, this.slice);
    }

    public static MapKey of(MapItemSavedData data){
      return new MapKey(data.dimension, data.x, data.z, getSlice(data));
    }

    @Nullable
    public static Integer getSlice(MapItemSavedData data) {
        return MapAtlasesMod.SUPPLEMENTARIES ? SupplementariesCompat.getSlice(data) : null;
    }

    public static MapKey at(byte scale, double px, double pz, ResourceKey<Level> dimension, @Nullable Integer slice) {
        //map code
        int i = 128 * (1 << scale);
        int j = Mth.floor((px + 64.0D) / i);
        int k = Mth.floor((pz + 64.0D) / i);
        int mapCenterX = j * i + i / 2 - 64;
        int mapCenterZ = k * i + i / 2 - 64;
        return new MapKey(dimension, mapCenterX, mapCenterZ, slice);
    }

    public static MapKey at(byte scale, Player player, @Nullable Integer slice) {
        double px = player.getX();
        double pz = player.getZ();
        return at(scale, px, pz, player.level.dimension(), slice);
    }
}