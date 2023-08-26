package pebjebs.mapatlases.mixin;

import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import pebjebs.mapatlases.utils.AtlasHolder;

// TODO: use capabilities
@Mixin(LecternBlockEntity.class)
public abstract class LecternBlockEntityMixin implements AtlasHolder {

    @Unique
    private boolean mapatlases$hasAtlas = false;

    @Override
    public boolean mapatlases$hasAtlas() {
        return mapatlases$hasAtlas;
    }

    @Override
    public void mapatlases$setAtlas(boolean atlas) {
        this.mapatlases$hasAtlas = atlas;
    }
}
