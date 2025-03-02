package pepjebs.mapatlases.integration;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.blaze3d.platform.NativeImage;
import net.mehvahdjukaar.moonlight.api.platform.ClientHelper;
import net.mehvahdjukaar.moonlight.api.resources.textures.TextureImage;
import net.mehvahdjukaar.supplementaries.common.misc.map_data.MapLightHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;

import java.io.IOException;
import java.util.Map;

// client class
public class SupplementariesClientCompat {

    public static void init() {
        ClientHelper.addClientReloadListener(R::new, MapAtlasesMod.res("lightmap"));
    }

    public static class R extends SimpleJsonResourceReloadListener {

        public R() {
            super(new Gson().newBuilder().create(), "lightmap");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
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
            float timeOfDay = level.getTimeOfDay(0);
            boolean isDay = timeOfDay < 0.26 || timeOfDay > 0.8;
            if (isDay != lastTickWasDay) {
                lastTickWasDay = isDay;
                MapLightHandler.setLightMap(lastTickWasDay ? dayTexture : nightTexture);
                MapRenderer mapRenderer = Minecraft.getInstance().gameRenderer.getMapRenderer();
                //lag
                for (var e : level.mapData.entrySet()) {
                    MapId keyId = e.getKey();
                    MapItemSavedData data = e.getValue();
                    mapRenderer.update(keyId, data);
                }
            }
        }
    }
}
