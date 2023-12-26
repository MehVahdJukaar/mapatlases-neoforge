package pepjebs.mapatlases.integration;

import net.mehvahdjukaar.moonlight.api.client.model.BakedQuadBuilder;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.platform.forge.PlatHelperImpl;
import net.raphimc.immediatelyfastapi.BatchingAccess;
import net.raphimc.immediatelyfastapi.ImmediatelyFastApi;

public class ImmediatelyFastCompat {

    public static void startBatching(){
      //  BatchingAccess batching = ImmediatelyFastApi.getApiImpl().getBatching();
        //batching.beginHudBatching();
    }

    public static void endBatching(){
        //ImmediatelyFastApi.getApiImpl().getBatching().endHudBatching();
    }
}
