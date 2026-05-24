package pepjebs.mapatlases.utils;

import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.maps.MapDecoration;

public final class VanillaDecorationHolder extends DecorationHolder {
    private final MapDecoration deco;

    VanillaDecorationHolder(MapDecoration deco, String id, MapDataHolder data) {
        super(id, data, getSortingString(deco));
        this.deco = deco;
    }

    @Override
    public double decorationDistSq(double px, double pz) {
        var d = this.data.data;
        double wx = d.centerX, wz = d.centerZ;
        int scale = 1 << d.scale;
        wx += scale * deco.x() / 2.0;
        wz += scale * deco.y() / 2.0;
        return Mth.square(wx - px) + Mth.square(wz - pz);
    }

    private static String getSortingString(MapDecoration md) {
        StringBuilder sb = new StringBuilder();
        String path = md.type().unwrapKey().get().location().getPath();
        sb.append(path);
        var name = md.name();
        if (name.isPresent()) {
            sb.append(" ");
            sb.append(name.get().getString());
        }
        return sb.toString();
    }
}