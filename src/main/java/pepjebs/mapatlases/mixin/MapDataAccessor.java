package pepjebs.mapatlases.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;

@Mixin(MapItemSavedData.class)
public interface MapDataAccessor {



    @Mutable
    @Accessor("x")
    void setX(int center);

    @Mutable
    @Accessor("z")
    void setZ(int center);

    @Invoker("addDecoration")
    void invokeAddDecoration(MapDecoration.Type pType, @Nullable LevelAccessor pLevel, String pDecorationName, double pLevelX,
                             double pLevelZ, double pRotation, @Nullable Component pName);


    @Mutable
    @Accessor("dimension")
    void setDimension(ResourceKey<Level> levelResourceKey);
}
