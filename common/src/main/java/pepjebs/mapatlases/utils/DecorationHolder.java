package pepjebs.mapatlases.utils;

import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecoration;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.maps.MapDecoration;

public record DecorationHolder(Object deco, String id, MapDataHolder data, String sortingString) {

    public DecorationHolder(Object deco, String id, MapDataHolder data) {
        this(deco, id, data, getSortingString(deco));
    }

    /**
     * Squared world-space distance between a decoration and (px, pz).
     * Uses the exact decoration position for vanilla markers; falls back to map centre for custom ones.
     */
    public double decorationDistSq(double px, double pz) {
        var d = this.data().data;
        double wx = d.centerX, wz = d.centerZ;
        if (this.deco() instanceof MapDecoration md) {
            // getDecorationPos simplifies to -(1 << scale) * coord / 2
            int scale = 1 << d.scale;
            wx += scale * md.x() / 2.0;
            wz += scale * md.y() / 2.0;
        } else if (this.deco() instanceof MLMapDecoration mm) {
            // getDecorationPos simplifies to -(1 << scale) * coord / 2
            int scale = 1 << d.scale;
            wx += scale * mm.getX() / 2.0;
            wz += scale * mm.getY() / 2.0;
        }
        return Mth.square(wx - px) + Mth.square(wz - pz);
    }

    private static String getSortingString(Object deco) {
        StringBuilder sb = new StringBuilder();
        if (deco instanceof MapDecoration md) {
            String path = md.type().unwrapKey().get().location().getPath();
            sb.append(path);
            var name = md.name();
            if (name.isPresent()) {
                sb.append(" ");
                sb.append(name.get().getString());
            }

        } else if (deco instanceof MLMapDecoration mm) {
            String path = mm.getType().unwrapKey().get().location().getPath();
            sb.append(path);
            var name = mm.getDisplayName();
            if (name != null) {
                sb.append(" ");
                sb.append(name.getString());
            }
        }
        return sb.toString();
    }
}
