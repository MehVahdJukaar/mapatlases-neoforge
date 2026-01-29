
package pepjebs.mapatlases.map_collection;

import net.minecraft.server.level.ColumnPos;
import net.minecraft.world.entity.player.Player;
import pepjebs.mapatlases.utils.Slice;

import java.util.Objects;

public final class MapGridKey {
    public final int mapX;
    public final int mapZ;
    public final Slice slice;
    public final int gridWidth;

    private MapGridKey(int mapX, int mapZ, Slice slice, int gridWidth ) {
        this.mapX = mapX;
        this.mapZ = mapZ;
        this.slice = slice;
        this.gridWidth = gridWidth;
    }

    public boolean isSameSlice(Slice slice) {
        return Objects.equals(slice, this.slice);
    }

    public static MapGridKey at(byte scale, Slice slice, double px, double pz) {
        //map code
        int width = getBlockWidthFromScale(scale);
        ColumnPos center = slice.type().getCenter(px, pz, width);
        return new MapGridKey(center.x(), center.z(), slice, width);
    }

    public static MapGridKey atEntityPosition(byte scale, Slice slice, Player player) {
        return at(scale, slice, player.getX(), player.getZ());
    }

    public MapGridKey offset(int dxTiles, int dzTiles) {
        return new MapGridKey(
                mapX + dxTiles * gridWidth,
                mapZ + dzTiles * gridWidth,
                slice, gridWidth
        );
    }


    public static int getBlockWidthFromScale(int scale) {
        return 128 * (1 << scale);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (MapGridKey) obj;
        return this.mapX == that.mapX &&
                this.mapZ == that.mapZ &&
                Objects.equals(this.slice, that.slice) &&
                this.gridWidth == that.gridWidth;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapX, mapZ, slice, gridWidth);
    }

    @Override
    public String toString() {
        return "MapIndexKey[" +
                "x=" + mapX + ", " +
                "z=" + mapZ + ", " +
                "slice=" + slice + ", " +
                "gridWidth=" + gridWidth + ']';
    }


}