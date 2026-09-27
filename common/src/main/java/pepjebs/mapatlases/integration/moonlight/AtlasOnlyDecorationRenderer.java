package pepjebs.mapatlases.integration.moonlight;

import net.mehvahdjukaar.moonlight.api.map.client.MLDecorationRenderState;
import net.mehvahdjukaar.moonlight.api.map.client.MapDecorationRenderer;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecoration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.client.MapAtlasesClient;

public class AtlasOnlyDecorationRenderer<T extends MLMapDecoration> extends MapDecorationRenderer<T> {

    public AtlasOnlyDecorationRenderer(Identifier texture) {
        super(texture);
    }

    @Override
    public boolean extract(T decoration, MapItemSavedData mapData, MLDecorationRenderState state) {
        if (!MapAtlasesClient.isDrawingAtlas()) return false;
        return super.extract(decoration, mapData, state);
    }

}
