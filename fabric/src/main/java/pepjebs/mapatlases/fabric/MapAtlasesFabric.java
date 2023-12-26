package pepjebs.mapatlases.fabric;

import net.fabricmc.api.ModInitializer;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import pepjebs.mapatlases.MapAtlasesMod;

public class MapAtlasesFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        MapAtlasesMod.init();

        if (PlatHelper.getPhysicalSide().isClient()) {

        }
        PlatHelper.addCommonSetup(ModSetup::setup);
        PlatHelper.addCommonSetup(ModSetup::asyncSetup);

    }
}
