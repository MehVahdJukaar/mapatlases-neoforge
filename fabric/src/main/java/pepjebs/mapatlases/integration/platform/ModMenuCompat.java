package pepjebs.mapatlases.integration.platform;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.mehvahdjukaar.moonlight.core.client.config.MoonlightConfigSelectScreen;
import pepjebs.mapatlases.MapAtlasesMod;

public class ModMenuCompat implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> MoonlightConfigSelectScreen.create(MapAtlasesMod.MOD_ID, parent, null);
    }

}
