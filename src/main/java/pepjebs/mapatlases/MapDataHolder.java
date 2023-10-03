package pepjebs.mapatlases;

import com.mojang.datafixers.util.Pair;
import net.mehvahdjukaar.moonlight.core.mixins.FirstPersonRendererMixin;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.capabilities.MapKey;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

public class MapDataHolder {
    //TODO: replace pair<String,MapData> with this
    private final int id;
    private final String stringId;
    private final MapKey key;
    private final MapItemSavedData data;

    public MapDataHolder(MapItemSavedData data, String name) {
        this.id = MapAtlasesAccessUtils.findMapIntFromString(name);
        this.key = MapKey.of(Pair.of(name, data));
        this.data = data;
        this.stringId = key.slice().getMapString(id);
    }

    //utility methods
}
