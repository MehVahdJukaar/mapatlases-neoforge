package pepjebs.mapatlases.fabric;

import net.fabricmc.api.ModInitializer;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.fabric.MapAtlasesClientImpl;

public class MapAtlasesFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        MapAtlasesMod.init();

        if (PlatHelper.getPhysicalSide().isClient()) {
            MapAtlasesClientImpl.init();
        }

    }
}
