package pepjebs.mapatlases.integration;

import com.mojang.blaze3d.platform.NativeImage;
import net.mehvahdjukaar.moonlight.api.platform.ClientHelper;
import net.mehvahdjukaar.moonlight.api.resources.textures.TextureImage;
import net.mehvahdjukaar.supplementaries.common.misc.map_data.MapLightHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.MapTextureManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;

import java.io.IOException;

// client class
public class SupplementariesClientCompat {

    public static void init() {
        ClientHelper.addClientReloadListener(ModReloader::new, MapAtlasesMod.res("lightmap"));
    }

    public static class ModReloader extends SimplePreparableReloadListener<Unit> {

        @Override
        protected Unit prepare(ResourceManager manager, ProfilerFiller profiler) {
            return Unit.INSTANCE;
        }

        @Override
        protected void apply(Unit unit, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
            try {
                dayTexture = TextureImage.open(pResourceManager, MapAtlasesMod.res("item/lightmap_day")).getImage();
                nightTexture = TextureImage.open(pResourceManager, MapAtlasesMod.res("item/lightmap_night")).getImage();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static NativeImage dayTexture;
    private static NativeImage nightTexture;
    private static boolean lastTickWasDay = true;

    public static void onClientTick(ClientLevel level) {
        if (MapAtlasesClientConfig.nightLightMap.get()) {
            boolean isDay = level.isBrightOutside();
            if (isDay != lastTickWasDay) {
                lastTickWasDay = isDay;
                MapLightHandler.setLightMap(lastTickWasDay ? dayTexture : nightTexture);
                MapTextureManager mapTextures = Minecraft.getInstance().getMapTextureManager();
                //lag
                for (var e : level.mapData.entrySet()) {
                    MapId keyId = e.getKey();
                    MapItemSavedData data = e.getValue();
                    mapTextures.update(keyId, data);
                }
            }
        }
    }
}
