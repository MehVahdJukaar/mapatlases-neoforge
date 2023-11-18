package pepjebs.mapatlases.integration.moonlight;

import net.minecraft.resources.ResourceLocation;

public class PinDecorationRenderer extends AtlasOnlyDecorationRenderer<PinDecoration> {

    public PinDecorationRenderer(ResourceLocation texture) {
        super(texture);
    }

    @Override
    protected boolean hasOutline(PinDecoration decoration) {
        return decoration.isFocused();
    }
}
