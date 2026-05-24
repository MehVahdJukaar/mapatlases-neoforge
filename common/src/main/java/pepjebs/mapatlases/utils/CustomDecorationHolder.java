package pepjebs.mapatlases.utils;

import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecoration;
import net.minecraft.util.Mth;

public final class CustomDecorationHolder extends DecorationHolder {
    private final MLMapDecoration deco;

    CustomDecorationHolder(MLMapDecoration deco, String id, MapDataHolder data) {
        super(id, data, getSortingString(deco));
        this.deco = deco;
    }

    @Override
    public double decorationDistSq(double px, double pz) {
        var d = this.data.data;
        double wx = d.centerX, wz = d.centerZ;
        int scale = 1 << d.scale;
        wx += scale * deco.getX() / 2.0;
        wz += scale * deco.getY() / 2.0;
        return Mth.square(wx - px) + Mth.square(wz - pz);
    }

    private static String getSortingString(MLMapDecoration mm) {
        StringBuilder sb = new StringBuilder();
        String path = mm.getType().unwrapKey().get().location().getPath();
        sb.append(path);
        var name = mm.getDisplayName();
        if (name != null) {
            sb.append(" ");
            sb.append(name.getString());
        }
        return sb.toString();
    }
}