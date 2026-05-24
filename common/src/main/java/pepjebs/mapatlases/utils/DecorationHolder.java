package pepjebs.mapatlases.utils;

import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecoration;

public abstract sealed class DecorationHolder permits VanillaDecorationHolder, CustomDecorationHolder {
    protected final String id;
    protected final MapDataHolder data;
    protected final String sortingString;

    protected DecorationHolder(String id, MapDataHolder data, String sortingString) {
        this.id = id;
        this.data = data;
        this.sortingString = sortingString;
    }

    public String id() {
        return id;
    }

    public MapDataHolder data() {
        return data;
    }

    public String sortingString() {
        return sortingString;
    }

    public abstract double decorationDistSq(double px, double pz);

    static VanillaDecorationHolder vanilla(MapDecoration deco, String id, MapDataHolder data) {
        return new VanillaDecorationHolder(deco, id, data);
    }

    static CustomDecorationHolder custom(MLMapDecoration deco, String id, MapDataHolder data) {
        return new CustomDecorationHolder(deco, id, data);
    }
}