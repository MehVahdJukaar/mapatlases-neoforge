package pepjebs.mapatlases.utils;

import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecoration;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapMarker;
import net.minecraft.world.level.saveddata.maps.MapDecoration;

public record DecorationHolder(Object deco, String id, MapDataHolder data) {

    public boolean matchesFilter( String filter) {
        if (this.deco() instanceof MapDecoration md) {
            String path = md.type().unwrapKey().get().location().getPath();
            if (path.contains(filter)) return true;
            var name = md.name();
            if (name.isPresent()) {
                if (name.get().getString().contains(filter)) return true;
            }

        } else if (this.deco() instanceof MLMapDecoration mm) {
            String path = mm.getType().unwrapKey().get().location().getPath();
            if (path.contains(filter)) return true;
            var name = mm.getDisplayName();
            if (name != null) {
                if (name.getString().contains(filter)) return true;
            }
        }
        return false;
    }
}
