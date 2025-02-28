package pepjebs.mapatlases.integration.moonlight;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.moonlight.api.map.client.MapDecorationRenderer;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecoration;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.client.MapAtlasesClient;

public class AtlasOnlyDecorationRenderer<T extends MLMapDecoration> extends MapDecorationRenderer<T> {

    public AtlasOnlyDecorationRenderer(ResourceLocation texture) {
        super(texture);
    }

    @Override
    public boolean render(T decoration, PoseStack matrixStack, VertexConsumer vertexBuilder, MultiBufferSource buffer, @Nullable MapItemSavedData mapData, boolean isOnFrame, int light, int index, boolean rendersText) {
        if (!MapAtlasesClient.isDrawingAtlas()) return false;
        return super.render(decoration, matrixStack, vertexBuilder, buffer, mapData, isOnFrame, light, index, rendersText);
    }

}
