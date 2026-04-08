package pepjebs.mapatlases.mixin;

import net.minecraft.client.renderer.blockentity.state.LecternRenderState;
import net.minecraft.client.resources.model.sprite.SpriteId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import pepjebs.mapatlases.client.AtlasLecternRenderState;

@Mixin(LecternRenderState.class)
public class LecternRenderStateMixin implements AtlasLecternRenderState {
    @Unique
    private boolean mapatlases$hasAtlas;

    @Unique
    private SpriteId mapatlases$texture;

    @Override
    public boolean mapatlases$hasAtlas() {
        return mapatlases$hasAtlas;
    }

    @Override
    public void mapatlases$setHasAtlas(boolean hasAtlas) {
        this.mapatlases$hasAtlas = hasAtlas;
    }

    @Override
    public SpriteId mapatlases$getTexture() {
        return mapatlases$texture;
    }

    @Override
    public void mapatlases$setTexture(SpriteId texture) {
        this.mapatlases$texture = texture;
    }
}
