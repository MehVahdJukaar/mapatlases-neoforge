package pepjebs.mapatlases.client.screen;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import static pepjebs.mapatlases.client.MapAtlasesClient.*;

public enum CursorAction {
    NONE, PLACING_PIN, SHEARING;

    @Nullable
    public ResourceLocation getIcon(boolean canPerformCursorAction) {
        return switch (this) {
            case NONE -> null;
            case PLACING_PIN -> canPerformCursorAction ? PLACE_PIN_READY_SPRITE : PLACE_PIN_SPRITE;
            case SHEARING -> canPerformCursorAction ? SHEAR_MAP_READY_SPRITE : SHEAR_MAP_SPRITE;
        };
    }
}
