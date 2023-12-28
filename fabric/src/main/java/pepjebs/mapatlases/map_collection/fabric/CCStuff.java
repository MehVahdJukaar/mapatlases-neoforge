package pepjebs.mapatlases.map_collection.fabric;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistryV3;
import dev.onyxstudios.cca.api.v3.item.ItemComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.item.ItemComponentInitializer;
import pepjebs.mapatlases.MapAtlasesMod;

public class CCStuff implements ItemComponentInitializer {
    public static final ComponentKey<IMapCollectionImpl> MAP_COLLECTION_COMPONENT =
            ComponentRegistryV3.INSTANCE.getOrCreate(MapAtlasesMod.res("map_collection"), IMapCollectionImpl.class);

    @Override
    public void registerItemComponentFactories(ItemComponentFactoryRegistry registry) {
        registry.register(MapAtlasesMod.MAP_ATLAS.get(), MAP_COLLECTION_COMPONENT, IMapCollectionImpl::new);
    }


}
