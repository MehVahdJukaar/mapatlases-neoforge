package pepjebs.mapatlases.mixin;

import net.minecraft.client.renderer.blockentity.state.LecternRenderState;
import net.minecraft.client.resources.model.sprite.SpriteId;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import pepjebs.mapatlases.client.AtlasLecternRenderState;

@Mixin(LecternRenderState.class)
public class LecternRenderStateMixin implements AtlasLecternRenderState {

    @Unique
    @Nullable
    private SpriteId mapatlases$atlasTexture;

    @Override
    public void mapatlases$setAtlasTexture(@Nullable SpriteId texture) {
        this.mapatlases$atlasTexture = texture;
    }

    @Nullable
    @Override
    public SpriteId mapatlases$getAtlasTexture() {
        return mapatlases$atlasTexture;
    }
}
