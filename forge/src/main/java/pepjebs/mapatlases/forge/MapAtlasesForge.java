package pepjebs.mapatlases.forge;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.map_collection.forge.IMapCollectionImpl;

@Mod(MapAtlasesMod.MOD_ID)
public class MapAtlasesForge {

    public MapAtlasesForge() {
        MapAtlasesMod.init();

        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        bus.addListener(IMapCollectionImpl::register);
    }


}
