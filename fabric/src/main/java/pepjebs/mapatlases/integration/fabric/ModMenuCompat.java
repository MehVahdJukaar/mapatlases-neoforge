package pepjebs.mapatlases.integration.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.mehvahdjukaar.moonlight.api.platform.configs.fabric.FabricConfigListScreen;
import net.minecraft.network.chat.Component;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;

public class ModMenuCompat implements ModMenuApi {


    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return s -> new FabricConfigListScreen(MapAtlasesMod.MOD_ID, MapAtlasesMod.MAP_ATLAS.get().getDefaultInstance(),
                Component.literal("§6Map Atlases Configs"), null,
                s, MapAtlasesClientConfig.SPEC, MapAtlasesClientConfig.SPEC);
    }

}
