package pepjebs.mapatlases.mixin;

import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MapItemSavedData.class)
public interface MapDataAccessor {

    @Accessor("x")
    void setCenterX(int center);
    @Accessor("z")
    void setCenterZ(int center);

}
