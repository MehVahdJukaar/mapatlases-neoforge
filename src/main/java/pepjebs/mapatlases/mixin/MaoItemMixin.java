package pepjebs.mapatlases.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MapItem.class)
public class MaoItemMixin {
    @Inject(method = "update", at = @At(value = "INVOKE",
            target = "Lcom/google/common/collect/LinkedHashMultiset;create()Lcom/google/common/collect/LinkedHashMultiset;"))
    public void reduceUpdateNonGeneratedChunks(Level pLevel, Entity pViewer, MapItemSavedData pData, CallbackInfo ci,
                                               @Local(ordinal = 10) int worldX, @Local(ordinal = 11) int worldZ) {
        if (worldZ % 16 == 0 && worldX % 16 == 0) {
            if (!pLevel.hasChunk(worldX, worldZ)) ci.cancel();
        }
    }

}
