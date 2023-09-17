package pepjebs.mapatlases.mixin;

import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MapItemSavedData.class)
public interface MapDataAccessor {



    @Mutable
    @Accessor("x")
    void setX(int center);

    @Mutable
    @Accessor("z")
    void setZ(int center);

}
