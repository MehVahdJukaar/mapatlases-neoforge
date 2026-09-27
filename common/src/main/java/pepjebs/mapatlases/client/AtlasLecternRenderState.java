package pepjebs.mapatlases.client;

import net.minecraft.client.resources.model.sprite.SpriteId;
import org.jetbrains.annotations.Nullable;

public interface AtlasLecternRenderState {

    void mapatlases$setAtlasTexture(@Nullable SpriteId texture);

    @Nullable
    SpriteId mapatlases$getAtlasTexture();
}
