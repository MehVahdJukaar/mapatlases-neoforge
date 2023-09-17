package pepjebs.mapatlases.mixin;

import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MapItemSavedData.class)
public interface MapItemSavedDataAccessor {

    @Accessor("centerX")
    void setCenterX(int center);
    @Accessor("centerZ")
    void setCenterZ(int center);

}
